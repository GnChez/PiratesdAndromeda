"""
MQTT pipeline (minimal).

INGRESS (server subscribes ONLY to this):
    juego/devices/<join_code>/<nombre_juego>/<evento>
    where <evento> ∈ {started, completed, sabotaged, desabotaged}

Canonical JSON body:
    {"username": "joan", "mission_id": 1}

Behavior:
- Extra fields in the JSON are silently ignored.
- If `username` is not a player of <join_code>, OR `mission_id` does not resolve to a
  mission in that partida, the message is DROPPED with a warning (no DB write, no WS).
- For valid messages we (a) run the matching DB side effect and (b) broadcast a
  minimal payload to the room's websocket.

EGRESS:
    juego/commands/<join_code>/<game>/<event>   (server -> hardware)
    via `publish_mission_event(...)`. Topic format is intentionally kept stable.

Outbound WS body for a device event (broadcast to room):
    {
      "type": "START_MISSION" | "COMPLETE_MISSION" | "SABOTAGE" | "DESABOTAGE",
      "source": "mqtt",
      "action": "started" | "completed" | "sabotaged" | "desabotaged",
      "player": "<id_usuario>",       # mismo criterio que WS de app (monitor / puntuación)
      "username": "joan",
      "mission_id": 1,
      "mission_name": "...",    # omitted when not resolvable
      "join_code": "UV2924",
      "nombre_juego": "JUEGOLED"
    }
"""

import asyncio
import json
import logging
import os
from typing import Any

import aiomqtt
from dotenv import load_dotenv
from sqlmodel import Session, select

import CRUD.games as games_crud
import CRUD.missions as missions_crud
from core.socket_manager import manager
from db.dbconnection import engine
from models.games import JugadoresPartida, Partidas
from models.links import MisionesPartida
from models.missions import Misiones
from models.users import Usuarios


log = logging.getLogger(__name__)

load_dotenv()

MQTT_BROKER ="129.158.197.45"
MQTT_PORT = 1883
TOPIC_DEVICES_SUBSCRIBE = "juego/devices/#"
TOPIC_MISSION_COMMAND_TEMPLATE = "juego/commands/{join_code}/{game}/{event}"

USERNAME = os.environ.get("MQTT_USER")
PWD = os.environ.get("PASSWORD")

# Single source of truth for action -> WS type mapping.
ACTION_TO_TYPE: dict[str, str] = {
    "started": "START_MISSION",
    "completed": "COMPLETE_MISSION",
    "sabotaged": "SABOTAGE",
    "desabotaged": "DESABOTAGE",
}


def _maybe_inner_json_dict(val: Any) -> dict[str, Any] | None:
    """If `val` is a JSON object string, return the parsed dict; else None."""
    if not isinstance(val, str):
        return None
    s = val.strip()
    if not s.startswith("{"):
        return None
    try:
        inner = json.loads(s)
    except json.JSONDecodeError:
        return None
    return inner if isinstance(inner, dict) else None


def _decode_payload(raw: Any) -> dict[str, Any]:
    """
    Decode an MQTT payload as a JSON object.

    Handles:
    - bytes / bytearray / memoryview (typical from brokers)
    - JSON that was accidentally double-encoded (outer JSON is a string)
    """
    if raw is None:
        return {}
    if isinstance(raw, str):
        text = raw.strip()
    elif isinstance(raw, (bytes, bytearray, memoryview)):
        try:
            text = bytes(raw).decode().strip()
        except UnicodeDecodeError:
            return {}
    else:
        return {}
    if not text:
        return {}
    try:
        data: Any = json.loads(text)
    except json.JSONDecodeError:
        return {}
    if isinstance(data, str):
        try:
            data = json.loads(data)
        except json.JSONDecodeError:
            return {}
    return data if isinstance(data, dict) else {}


def _username_mission_from_payload(payload: dict[str, Any]) -> tuple[str, int | None]:
    """
    Read canonical `username` / `mission_id`, and unwrap legacy firmware shapes where
    the device duplicated the JSON body as a string under `action` / `id_usuario`.
    """
    username = str(payload.get("username") or "").strip()
    raw_mid: Any = payload.get("mission_id")

    if (not username or raw_mid is None) and isinstance(payload, dict):
        for key in ("action", "id_usuario", "payload", "body", "data", "message"):
            inner = _maybe_inner_json_dict(payload.get(key))
            if inner:
                if not username:
                    username = str(inner.get("username") or "").strip()
                if raw_mid is None and inner.get("mission_id") is not None:
                    raw_mid = inner.get("mission_id")

    inner_u = _maybe_inner_json_dict(username) if username.startswith("{") else None
    if inner_u:
        username = str(inner_u.get("username") or "").strip()
        if raw_mid is None:
            raw_mid = inner_u.get("mission_id")

    try:
        mission_id = int(raw_mid) if raw_mid is not None else None
    except (TypeError, ValueError):
        mission_id = None
    return username, mission_id


def _resolve_context(
    db: Session, join_code: str, username: str, mission_id: int
) -> tuple[Partidas, MisionesPartida, int] | None:
    """
    Return (partida, mision_partida, id_usuario) iff:
      - join_code maps to a Partidas row,
      - `username` is a registered player of that partida,
      - `mission_id` is a MisionesPartida row inside that partida.
    Otherwise return None (caller must drop the message).
    """
    print(join_code, username)
    partida = db.exec(
        select(Partidas).where(Partidas.codigo_partida == join_code)
    ).first()
    if partida is None:
        return None

    user = db.exec(
        select(Usuarios).where(Usuarios.nombre_usuario == username)
    ).first()
    if user is None:
        return None
    player_row = db.exec(
        select(JugadoresPartida).where(
            JugadoresPartida.id_partida == partida.id_partida,
            JugadoresPartida.id_usuario == user.id_usuario,
        )
    ).first()
    if player_row is None:
        return None
    mp = db.exec(
        select(MisionesPartida).where(
            MisionesPartida.id_mision == mission_id,
            MisionesPartida.id_partida == partida.id_partida,
        )
    ).first()
    if mp is None:
        return None
    return partida, mp, user.id_usuario


def _apply_db_effect(db: Session, action: str, mp: MisionesPartida) -> bool:
    """Aplica efectos en BD. Devuelve False si `completed` era duplicado (sin cambios)."""
    mission_id = mp.id_mision_partida
    if action == "started":
        missions_crud.update_mission_start_time(db, mission_id)
        return True
    if action == "completed":
        _, newly = missions_crud.update_mission_completion(db, mission_id)
        if not newly:
            return False
        mision = missions_crud.get_mission_by_game_mission_id(db, mission_id)
        if mision is not None:
            games_crud.update_reparacion_partida(
                db, mp.id_partida, mision.porcentaje_reparacion
            )
        return True
    if action == "sabotaged":
        missions_crud.update_mission_sabotage(db, mission_id)
        return True
    if action == "desabotaged":
        missions_crud.update_mission_desabotage(db, mission_id)
        return True
    return True


async def _handle_device_event(topic_parts: list[str], payload: dict[str, Any]) -> None:
    """
    Topic: juego/devices/<join_code>/<nombre_juego>/<evento>
    Body:  {"username": "...", "mission_id": <int>}  (extras ignored)
    """
    if len(topic_parts) < 5 or topic_parts[:2] != ["juego", "devices"]:
        log.warning("MQTT topic invalido: %s", "/".join(topic_parts))
        return

    join_code = topic_parts[2]
    nombre_juego = topic_parts[3]
    topic_event = topic_parts[4]

    event_type = ACTION_TO_TYPE.get(topic_event)
    if event_type is None:
        log.info("MQTT evento no soportado: %s", topic_event)
        return

    username, mission_id = _username_mission_from_payload(payload)

    if not username or mission_id is None:
        log.warning(
            "MQTT body sin username/mission_id: topic=%s body=%s",
            "/".join(topic_parts),
            payload,
        )
        return

    with Session(engine) as db:
        ctx = _resolve_context(db, join_code, username, mission_id)
        if ctx is None:
            log.warning(
                "MQTT descartado (usuario o mision no pertenecen a la partida): "
                "join_code=%s username=%s mission_id=%s",
                join_code,
                username,
                mission_id,
            )
            return
        partida, mp, id_usuario = ctx
        proceed = _apply_db_effect(db, topic_event, mp)
        if not proceed:
            return
        if topic_event == "completed":
            games_crud.add_mission_completion_points(
                db, partida.id_partida, id_usuario, mp.id_mision_partida
            )
        mision_row = db.get(Misiones, mp.id_mision)
        mission_name = mision_row.nombre if mision_row is not None else None

    message: dict[str, Any] = {
        "type": event_type,
        "source": "mqtt",
        "action": topic_event,
        "player": str(id_usuario),
        "username": username,
        "mission_id": mission_id,
        "join_code": join_code,
        "nombre_juego": nombre_juego,
    }
    if mission_name:
        message["mission_name"] = mission_name

    try:
        room_uuid = manager.join_code_to_room_uuid(join_code)
    except KeyError:
        log.warning("join_code no registrado para WS: %s", join_code)
        return
    await manager.broadcast_to_room(message, room_uuid)


async def publish_mqtt_message(topic: str, payload: dict[str, Any]) -> None:
    """Open a short-lived connection and publish a JSON payload."""
    async with aiomqtt.Client(
        hostname=MQTT_BROKER,
        port=MQTT_PORT,
        username=USERNAME,
        password=PWD,
    ) as client:
        await client.publish(topic, json.dumps(payload, ensure_ascii=False))


async def publish_mission_event(
    join_code: str,
    event: str,
    payload: dict[str, Any],
    *,
    game: str = "MISSION",
) -> None:
    """Server -> hardware: publish to juego/commands/<join_code>/<game>/<event>."""
    topic = TOPIC_MISSION_COMMAND_TEMPLATE.format(
        join_code=join_code, game=game, event=event
    )
    await publish_mqtt_message(topic, payload)


async def game_mqtt() -> None:
    """Subscribe forever to the devices topic and dispatch each message."""
    while True:
        try:
            async with aiomqtt.Client(
                hostname=MQTT_BROKER,
                port=MQTT_PORT,
                username=USERNAME,
                password=PWD,
            ) as client:
                await client.subscribe(TOPIC_DEVICES_SUBSCRIBE)
                log.info("MQTT conectado. Escuchando: %s", TOPIC_DEVICES_SUBSCRIBE)
                async for message in client.messages:
                    try:
                        topic_parts = message.topic.value.split("/")
                        payload = _decode_payload(message.payload)
                        await _handle_device_event(topic_parts, payload)
                    except Exception as err:
                        log.warning(
                            "Error procesando MQTT (%s): %s",
                            message.topic.value,
                            err,
                        )
        except aiomqtt.MqttError as err:
            log.warning("MQTT desconectado: %s. Reintento en 3s...", err)
            await asyncio.sleep(3)
        except asyncio.CancelledError:
            log.info("MQTT detenido.")
            break
        except RuntimeError as err:
            if "Event loop is closed" in str(err):
                log.info("Event loop cerrado; deteniendo MQTT.")
                break
            raise
