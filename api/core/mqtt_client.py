import json
import aiomqtt
import asyncio
from core.socket_manager import manager
from dotenv import load_dotenv
import os
from typing import Any
from sqlmodel import Session, select
import CRUD.missions as missions_crud
import CRUD.games as games_crud
from db.dbconnection import engine
from models.users import Usuarios


# Configuracion
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


def _maybe_decode_json(value: Any) -> Any:
    if not isinstance(value, str):
        return value
    try:
        return json.loads(value)
    except json.JSONDecodeError:
        return value


def _normalize_payload(payload: Any) -> Any:
    payload = _maybe_decode_json(payload)
    if not isinstance(payload, dict):
        return payload

    normalized = dict(payload)
    for key in ("action", "id_usuario"):
        nested = _maybe_decode_json(normalized.get(key))
        if isinstance(nested, dict):
            normalized.update(nested)
    return normalized


def _extract_username(payload: Any) -> str | None:
    if not isinstance(payload, dict):
        return None
    username = payload.get("username") or payload.get("nombre_usuario")
    if username is None:
        return None
    return str(username)


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
        return _normalize_payload(json.loads(decoded_payload))
    except json.JSONDecodeError:
        return {"raw_payload": decoded_payload}


def _extract_join_code(topic_parts: list[str]) -> str | None:
    if len(topic_parts) < 3:
        return None
    if topic_parts[0] != "juego":
        return None
    if topic_parts[1] not in {"devices", "commands"}:
        return None
    return topic_parts[2]


def _is_server_mission_command(topic_parts: list[str], payload: Any) -> bool:
    """Evita reemitir por WS los comandos MQTT que el propio servidor publica."""
    return (
        len(topic_parts) >= 5
        and topic_parts[0] == "juego"
        and topic_parts[1] == "commands"
        and topic_parts[3] == "missions"
        and isinstance(payload, dict)
        and payload.get("status") in {"confirmed", "pending_confirmation"}
        and "mission_id" in payload
        and "player" in payload
    )


def _resolve_device_event_context(
    db: Session,
    *,
    join_code: str,
    payload: Any,
) -> dict[str, Any] | None:
    payload = _normalize_payload(payload)
    username = _extract_username(payload)
    mission_ref = _extract_mission_id(payload)
    if username is None or mission_ref is None:
        return None

    game = games_crud.get_game_by_code(db, join_code)
    user = db.exec(select(Usuarios).where(Usuarios.nombre_usuario == username)).first()
    if game is None or user is None:
        return None

    mission_instance_id = missions_crud.resolve_mision_partida_instance_id(
        db,
        game.id_partida,
        user.id_usuario,
        mission_ref,
    )
    if mission_instance_id is None:
        return None

    mission = missions_crud.get_mission_by_game_mission_id(db, mission_instance_id)
    if mission is None:
        return None

    return {
        "game": game,
        "user": user,
        "username": username,
        "mission_ref": mission_ref,
        "mission_instance_id": mission_instance_id,
        "mission": mission,
    }


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
    await manager.broadcast_to_room(message, room_uuid)


async def _broadcast_score_updated(
    db: Session,
    room_uuid: str,
    game_id: int,
    player_score,
) -> None:
    if player_score is None:
        return
    await manager.broadcast_to_room(
        {
            "type": "SCORE_UPDATED",
            "player": str(player_score.id_usuario),
            "puntos_partida": int(player_score.puntos_partida or 0),
            "misiones_completadas": int(player_score.misiones_completadas or 0),
            "sabotajes_realizados": int(player_score.sabotajes_realizados or 0),
            "eliminaciones_realizadas": int(player_score.eliminaciones_realizadas or 0),
            "scores": games_crud.get_game_scores(db, game_id),
        },
        room_uuid,
    )


async def _handle_device_event(topic_parts: list[str], payload: Any) -> None:
    join_code = _extract_join_code(topic_parts)
    if not join_code:
        print(f"Topic de dispositivo invalido: {'/'.join(topic_parts)}")
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
        print(f"Evento de dispositivo no soportado: {event}")
        return

    room_uuid = manager.join_code_to_room_uuid(join_code)
    score_update = None
    with Session(engine) as db:
        context = _resolve_device_event_context(db, join_code=join_code, payload=payload)
        if context is None:
            print(f"Evento de dispositivo no resoluble: {payload}")
            return

        mission_instance_id = context["mission_instance_id"]
        if event == "started":
            missions_crud.update_mission_start_time(db, mission_instance_id)
        elif event == "completed":
            mission_state = missions_crud.get_mission_player_row(db, mission_instance_id)
            completed_before = (
                mission_state is not None and mission_state.id_estado_mision == 3
            )
            missions_crud.update_mission_completion(db, mission_instance_id)
            mission = context["mission"]
            game_mission_row = missions_crud.get_game_mission_row(db, mission_instance_id)
            if mission is not None and game_mission_row is not None:
                if not completed_before:
                    games_crud.update_reparacion_partida(
                        db, game_mission_row.id_partida, mission.porcentaje_reparacion
                    )
                    if mission_state is not None:
                        player_score = games_crud.award_player_mission_points(
                            db,
                            game_mission_row.id_partida,
                            mission_state.id_jugador,
                            int(mission.puntos_otorgados or 0),
                        )
                        if player_score is not None:
                            score_update = {
                                "type": "SCORE_UPDATED",
                                "player": str(player_score.id_usuario),
                                "puntos_partida": int(player_score.puntos_partida or 0),
                                "misiones_completadas": int(
                                    player_score.misiones_completadas or 0
                                ),
                                "sabotajes_realizados": int(
                                    player_score.sabotajes_realizados or 0
                                ),
                                "eliminaciones_realizadas": int(
                                    player_score.eliminaciones_realizadas or 0
                                ),
                                "scores": games_crud.get_game_scores(
                                    db, game_mission_row.id_partida
                                ),
                            }
        elif event == "sabotaged":
            mission_state = missions_crud.get_mission_player_row(db, mission_instance_id)
            sabotaged_before = (
                mission_state is not None and mission_state.id_estado_mision == 5
            )
            missions_crud.update_mission_sabotage(db, mission_instance_id)
            mission = context["mission"]
            if not sabotaged_before and mission is not None:
                player_score = games_crud.award_player_sabotage_points(
                    db,
                    context["game"].id_partida,
                    context["user"].id_usuario,
                    int(mission.puntos_sabotaje or 0),
                )
                if player_score is not None:
                    score_update = {
                        "type": "SCORE_UPDATED",
                        "player": str(player_score.id_usuario),
                        "puntos_partida": int(player_score.puntos_partida or 0),
                        "misiones_completadas": int(
                            player_score.misiones_completadas or 0
                        ),
                        "sabotajes_realizados": int(
                            player_score.sabotajes_realizados or 0
                        ),
                        "eliminaciones_realizadas": int(
                            player_score.eliminaciones_realizadas or 0
                        ),
                        "scores": games_crud.get_game_scores(
                            db, context["game"].id_partida
                        ),
                    }
        elif event == "desabotaged":
            missions_crud.update_mission_desabotage(db, mission_instance_id)

        player_id = context["user"].id_usuario
        username = context["username"]
        mission_ref = context["mission_ref"]
        mission_name = context["mission"].nombre

    nombre_juego = topic_parts[3] if len(topic_parts) > 3 else None
    await manager.broadcast_to_room(
        {
            "type": event_type,
            "source": "mqtt",
            "action": event,
            "player": str(player_id),
            "username": username,
            "mission_id": mission_ref,
            "join_code": join_code,
            "nombre_juego": nombre_juego,
            "mission_name": mission_name,
        },
        room_uuid,
    )
    if score_update is not None:
        await manager.broadcast_to_room(score_update, room_uuid)


async def _handle_command_event(topic_parts: list[str], payload: Any) -> None:
    if _is_server_mission_command(topic_parts, payload):
        return

    join_code = _extract_join_code(topic_parts)
    if not join_code:
        print(f"Topic de comando invalido: {'/'.join(topic_parts)}")
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
                print(f"Conectado a MQTT. Escuchando: {TOPIC_SENSORS}")
                print(f"Conectado a MQTT. Escuchando: {TOPIC_COMMANDS}")
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
                            print(f"Topic ignorado: {topic}")
                    except KeyError:
                        print(f"join_code no registrado para topic {message.topic.value}")
                    except Exception as processing_error:
                        print(f"Error procesando mensaje MQTT ({message.topic.value}): {processing_error}")
        except aiomqtt.MqttError as error:
            print(f"Error de conexion MQTT: {error}. Reintentando en 3s...")
            await asyncio.sleep(3)
        except asyncio.CancelledError:
            print("Escuchador MQTT detenido correctamente.")
            break
        except RuntimeError as runtime_error:
            if "Event loop is closed" in str(runtime_error):
                print("Event loop cerrado; deteniendo MQTT.")
                break
            raise