import json
import aiomqtt
import asyncio
from core.socket_manager import manager
from dotenv import load_dotenv
import os
from typing import Any
from sqlmodel import Session
import CRUD.missions as missions_crud
import CRUD.games as games_crud
from db.dbconnection import engine


# Configuración
MQTT_BROKER = "129.158.197.45"
MQTT_PORT = 1883
TOPIC_SENSORS = "juego/devices/#"  # juego/devices/{room code}/{game}/started
TOPIC_COMMANDS = "juego/commands/#"
TOPIC_MISSION_COMMAND_TEMPLATE = "juego/commands/{join_code}/missions/{event}"
load_dotenv()
USERNAME = os.environ.get("MQTT_USER")
PWD = os.environ.get("PASSWORD")


def _extract_message_text(payload: Any) -> str:
    """Normaliza distintos formatos de payload a texto."""
    if isinstance(payload, dict):
        if "action" in payload:
            return str(payload["action"])
        if "id_usuario" in payload:
            return str(payload["id_usuario"])
        if len(payload) == 1:
            return str(next(iter(payload.values())))
        return json.dumps(payload, ensure_ascii=False)
    if isinstance(payload, list):
        return json.dumps(payload, ensure_ascii=False)
    return str(payload)


def _extract_mission_id(payload: Any) -> int | None:
    if not isinstance(payload, dict):
        return None
    mission_id = (
        payload.get("mission_id")
        or payload.get("id_mision_partida")
        or payload.get("id_mision")
    )
    if mission_id is None:
        return None
    try:
        return int(mission_id)
    except (TypeError, ValueError):
        return None


def _decode_payload(raw_payload: bytes) -> Any:
    decoded_payload = raw_payload.decode()
    try:
        return json.loads(decoded_payload)
    except json.JSONDecodeError:
        return {"raw_payload": decoded_payload}


def _extract_join_code(topic_parts: list[str]) -> str | None:
    """
    Espera topics:
    - juego/devices/{join_code}/...
    - juego/commands/{join_code}/...
    """
    if len(topic_parts) < 3:
        return None
    if topic_parts[0] != "juego":
        return None
    if topic_parts[1] not in {"devices", "commands"}:
        return None
    return topic_parts[2]


async def _broadcast_room_event(join_code: str, event_type: str, payload: Any) -> None:
    room_uuid = manager.join_code_to_room_uuid(join_code)
    mission_id = _extract_mission_id(payload)
    message = {
        "type": event_type,
        "action": _extract_message_text(payload),
        "id_usuario": _extract_message_text(payload),
        "source": "mqtt",
    }
    if mission_id is not None:
        message["mission_id"] = mission_id
    await manager.broadcast_to_room(
        message,
        room_uuid,
    )


async def _handle_device_event(topic_parts: list[str], payload: Any) -> None:
    join_code = _extract_join_code(topic_parts)
    if not join_code:
        print(f"⚠️ Topic de dispositivo inválido: {'/'.join(topic_parts)}")
        return

    event = topic_parts[-1]
    event_to_type = {
        "started": "START_MISSION",
        "completed": "COMPLETE_MISSION",
        "sabotaged": "SABOTAGE",
        "desabotaged": "DESABOTAGE",
    }
    event_type = event_to_type.get(event)
    if not event_type:
        print(f"ℹ️ Evento de dispositivo no soportado: {event}")
        return

    mission_id = _extract_mission_id(payload)
    if mission_id is None:
        print(f"⚠️ Evento de dispositivo sin mission_id: {payload}")
        return

    with Session(engine) as db:
        if event == "started":
            missions_crud.update_mission_start_time(db, mission_id)
        elif event == "completed":
            missions_crud.update_mission_completion(db, mission_id)
            mission = missions_crud.get_mission_by_game_mission_id(db, mission_id)
            game_mission_row = missions_crud.get_game_mission_row(db, mission_id)
            if mission is not None and game_mission_row is not None:
                games_crud.update_reparacion_partida(
                    db, game_mission_row.id_partida, mission.porcentaje_reparacion
                )
        elif event == "sabotaged":
            missions_crud.update_mission_sabotage(db, mission_id)
        elif event == "desabotaged":
            missions_crud.update_mission_desabotage(db, mission_id)

    await _broadcast_room_event(join_code, event_type, payload)


async def _handle_command_event(topic_parts: list[str], payload: Any) -> None:
    join_code = _extract_join_code(topic_parts)
    if not join_code:
        print(f"⚠️ Topic de comando inválido: {'/'.join(topic_parts)}")
        return

    await _broadcast_room_event(join_code, "IOT_COMMAND", payload)


async def publish_mqtt_message(topic: str, payload: dict[str, Any]) -> None:
    async with aiomqtt.Client(
        hostname=MQTT_BROKER,
        port=MQTT_PORT,
        username=USERNAME,
        password=PWD,
    ) as client:
        await client.publish(topic, json.dumps(payload, ensure_ascii=False))


async def publish_mission_event(join_code: str, event: str, payload: dict[str, Any]) -> None:
    topic = TOPIC_MISSION_COMMAND_TEMPLATE.format(join_code=join_code, event=event)
    await publish_mqtt_message(topic, payload)


async def game_mqtt():
    while True:
        try:
            async with aiomqtt.Client(hostname=MQTT_BROKER, port=MQTT_PORT, username=USERNAME, password=PWD) as client:
                await client.subscribe(f"{TOPIC_SENSORS}")
                await client.subscribe(f"{TOPIC_COMMANDS}")
                print(f"✅ Conectado a MQTT. Escuchando: {TOPIC_SENSORS}")
                print(f"✅ Conectado a MQTT. Escuchando: {TOPIC_COMMANDS}")
                async for message in client.messages:
                    try:
                        topic = message.topic.value
                        topic_parts = topic.split("/")
                        payload = _decode_payload(message.payload)

                        if message.topic.matches("juego/devices/#"):
                            await _handle_device_event(topic_parts, payload)
                        elif message.topic.matches("juego/commands/#"):
                            await _handle_command_event(topic_parts, payload)
                        else:
                            print(f"ℹ️ Topic ignorado: {topic}")
                    except KeyError:
                        # join_code no registrado en el manager.
                        print(f"⚠️ join_code no registrado para topic {message.topic.value}")
                    except Exception as processing_error:
                        # No detenemos el loop por errores de un solo mensaje.
                        print(f"⚠️ Error procesando mensaje MQTT ({message.topic.value}): {processing_error}")

        except aiomqtt.MqttError as error:
            print(f"⚠️ Error de conexión MQTT: {error}. Reintentando en 3s...")
            await asyncio.sleep(3)
        except asyncio.CancelledError:
            # Esto ocurre cuando apagamos el servidor de FastAPI (Ctrl+C)
            print("🛑 Escuchador MQTT detenido correctamente.")
            break  # Salimos del bucle while para apagar el hilo
        except RuntimeError as runtime_error:
            # Evita ruido al cerrar el loop durante apagado abrupto.
            if "Event loop is closed" in str(runtime_error):
                print("🛑 Event loop cerrado; deteniendo MQTT.")
                break
            raise
