import asyncio
from collections import Counter
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from typing import Optional

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from sqlmodel import Session

import CRUD.games as games_crud
import CRUD.look_up as lookup
import CRUD.missions as missions_crud
from CRUD.users import get_user
from CRUD.events import (
    ALLOWED_GAME_EVENT_TYPES,
    create_game_event_by_type,
)
from core.mqtt_client import game_mqtt, publish_mission_event
from core.socket_manager import manager
from db.dbconnection import engine, init_db
from endpoints.games import router as games_router
from endpoints.users import router as users_router
from models.devices import *  # noqa: F401,F403 - registra tablas
from models.events import *  # noqa: F401,F403
from models.events import EventosPartida, Votos  # uso explícito
from models.games import *  # noqa: F401,F403
from models.links import *  # noqa: F401,F403
from models.look_up import *  # noqa: F401,F403
from models.missions import *  # noqa: F401,F403
from models.rooms import *  # noqa: F401,F403
from models.users import *  # noqa: F401,F403
from schemas.users import *  # noqa: F401,F403
from services.games import PartidasService


REUNION_DURATION_SECONDS = 60 * 5
_active_reunions: dict[str, dict[str, object]] = {}
_reunion_lock = asyncio.Lock()


def _game_has_ended(db: Session, game) -> bool:
    estado = lookup.get_game_state(db, "finalizada")
    return estado is not None and game.id_estado_partida == estado.id_estado_partida


async def _ws_close_with_error(
    websocket: WebSocket,
    *,
    code: str,
    message: str,
) -> None:
    await websocket.accept()
    await websocket.send_json({"type": "ERROR", "code": code, "message": message})
    await websocket.close()


@asynccontextmanager
async def lifespan(app: FastAPI):
    mqtt_task: Optional[asyncio.Task] = None
    try:
        # Primero inicializamos la DB; si falla, no dejamos tareas vivas.
        init_db()
        # Solo arrancamos MQTT cuando la app ya puede operar.
        mqtt_task = asyncio.create_task(game_mqtt())
        print("Sistema IoT Iniciado")
        yield
    finally:
        # Cancelamos timers de reuniones para evitar "Task was destroyed but it is pending".
        timers: list[asyncio.Task] = []
        async with _reunion_lock:
            for state in _active_reunions.values():
                timer_task = state.get("timer_task")
                if isinstance(timer_task, asyncio.Task) and not timer_task.done():
                    timer_task.cancel()
                    timers.append(timer_task)
            _active_reunions.clear()
        if timers:
            await asyncio.gather(*timers, return_exceptions=True)

        if mqtt_task is not None:
            mqtt_task.cancel()
            try:
                await mqtt_task
            except asyncio.CancelledError:
                pass
            except Exception:
                pass
            print("Sistema IoT Detenido")


app = FastAPI(lifespan=lifespan, title="AMONG US")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def _player_public_id(player_id: Optional[int], ws_code: str) -> str:
    return str(player_id) if player_id is not None else ws_code


def _parse_int(value: object) -> int | None:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def _resolve_expelled_player(votes: dict[int, Optional[int]]) -> Optional[int]:
    """Mayoría simple. Si hay empate o todos votan en blanco, no se expulsa a nadie."""
    if not votes:
        return None
    counter = Counter(target for target in votes.values() if target is not None)
    if not counter:
        return None
    most_common = counter.most_common()
    if len(most_common) > 1 and most_common[0][1] == most_common[1][1]:
        return None
    return most_common[0][0]


async def _finish_reunion(room_uuid: str, reason: str) -> None:
    """Cierra la reunión, decide expulsado y emite el resultado."""
    task_to_cancel: Optional[asyncio.Task] = None
    state: Optional[dict[str, object]] = None
    async with _reunion_lock:
        state = _active_reunions.pop(room_uuid, None)
        if not state:
            return
        timer_task = state.get("timer_task")
        if (
            isinstance(timer_task, asyncio.Task)
            and timer_task is not asyncio.current_task()
            and not timer_task.done()
        ):
            task_to_cancel = timer_task
    if task_to_cancel:
        task_to_cancel.cancel()

    votes = state.get("votes") if isinstance(state.get("votes"), dict) else {}
    expelled = _resolve_expelled_player(votes)  # type: ignore[arg-type]
    game_id = state.get("game_id") if isinstance(state.get("game_id"), int) else None

    if expelled is not None and game_id is not None:
        try:
            with Session(engine) as db:
                games_crud.mark_player_dead(db, game_id, expelled)
                create_game_event_by_type(
                    db,
                    game_id,
                    "jugador_eliminado",
                    None,
                    expelled,
                    descripcion=f"expulsado por reunion ({reason})",
                )
        except Exception as exc:
            print(f"⚠️ No se pudo registrar la expulsión: {exc}")

    await manager.broadcast_to_room(
        {
            "type": "REUNION_FINISHED",
            "reason": reason,
            "expelled_player": str(expelled) if expelled is not None else None,
            "votes": {str(voter): (str(t) if t is not None else None) for voter, t in (votes or {}).items()},
        },
        room_uuid,
    )


async def _run_reunion_timeout(room_uuid: str) -> None:
    try:
        await asyncio.sleep(REUNION_DURATION_SECONDS)
        await _finish_reunion(room_uuid, "timeout")
    except asyncio.CancelledError:
        return


async def _start_reunion(
    db: Session,
    room_uuid: str,
    game_id: int,
    game_code: str,
    player_id: Optional[int],
    player_key: str,
) -> bool:
    async with _reunion_lock:
        if room_uuid in _active_reunions:
            return False
        # Persistimos el evento padre de la reunión para que los Votos lo referencien.
        reunion_event = create_game_event_by_type(
            db,
            game_id,
            "reunion_emergencia",
            player_id,
            descripcion=f"Convocada por {player_key}",
        )
        _active_reunions[room_uuid] = {
            "votes": {},  # voter_id -> target_id (None = blanco)
            "game_id": game_id,
            "game_code": game_code,
            "reunion_event_id": reunion_event.id_evento,
            "timer_task": asyncio.create_task(_run_reunion_timeout(room_uuid)),
        }
    await manager.broadcast_to_room(
        {
            "type": "REUNION",
            "player": player_key,
            "duration_seconds": REUNION_DURATION_SECONDS,
        },
        room_uuid,
    )
    return True


async def _register_reunion_vote(
    db: Session,
    room_uuid: str,
    voter_id: int,
    target_id: Optional[int],
) -> None:
    should_finish = False
    reunion_event_id: Optional[int] = None
    async with _reunion_lock:
        state = _active_reunions.get(room_uuid)
        if not state:
            return
        votes = state.get("votes")
        if not isinstance(votes, dict):
            return
        votes[voter_id] = target_id
        reunion_event_id = state.get("reunion_event_id") if isinstance(state.get("reunion_event_id"), int) else None
        connected_players = manager.count_players(room_uuid)
        should_finish = connected_players > 0 and len(votes) >= connected_players

    if reunion_event_id is not None:
        try:
            db.add(
                Votos(
                    id_evento=reunion_event_id,
                    id_jugador_votante=voter_id,
                    id_jugador_votado=target_id,
                )
            )
            db.commit()
        except Exception as exc:
            db.rollback()
            print(f"⚠️ No se pudo persistir el voto: {exc}")

    if should_finish:
        await _finish_reunion(room_uuid, "all_voted")


async def _handle_reunion_player_exit(room_uuid: str, player_id: Optional[int]) -> None:
    if player_id is None:
        return
    should_finish = False
    async with _reunion_lock:
        state = _active_reunions.get(room_uuid)
        if not state:
            return
        votes = state.get("votes")
        if isinstance(votes, dict):
            votes.pop(player_id, None)
        connected_players = manager.count_players(room_uuid)
        should_finish = (
            connected_players > 0
            and isinstance(votes, dict)
            and len(votes) >= connected_players
        )
    if should_finish:
        await _finish_reunion(room_uuid, "all_voted")


def _sync_remove_player_from_db(game_code: str, player_id: int) -> None:
    with Session(engine) as db:
        games_crud.leave_game(db, game_code, player_id)


async def _complete_player_exit(
    websocket: WebSocket,
    room_uuid: str,
    game_code: str,
    game_id: Optional[int],
    ws_code: str,
    player_id: Optional[int],
    reason: str,
    *,
    remove_from_db: bool = False,
) -> None:
    """Cierra el WS del jugador, invalida su token y notifica la sala.

    La fila en `jugadores_partida` solo se borra si `remove_from_db` es True
    (acción explícita `salir`). Una desconexión no debe eliminar al jugador de la partida.
    """
    manager.disconnect(websocket, room_uuid)
    await _handle_reunion_player_exit(room_uuid, player_id)
    player_key = _player_public_id(player_id, ws_code)
    display_name: Optional[str] = None
    if player_id is not None:
        with Session(engine) as db:
            if game_id is not None:
                display_name = games_crud.display_name_for_game_player(db, game_id, player_id)
            else:
                u = get_user(db, player_id)
                if u is not None and (u.nombre_usuario or "").strip():
                    display_name = (u.nombre_usuario or "").strip()
    left_payload: dict[str, object] = {
        "event": "PLAYER_LEFT",
        "player": player_key,
        "reason": reason,
        "nombre_usuario": display_name,
        "player_name": display_name,
    }
    await manager.broadcast_to_room(left_payload, room_uuid)
    if player_id is not None and remove_from_db:
        try:
            _sync_remove_player_from_db(game_code, player_id)
        except Exception:
            pass
    if player_id is not None and game_id is not None:
        try:
            with Session(engine) as db:
                create_game_event_by_type(
                    db,
                    game_id,
                    "jugador_desconectado",
                    player_id,
                    descripcion=reason,
                )
        except Exception:
            pass
    # Solo invalidar token al salir explícitamente de la partida; una caída de red no debe
    # obligar a hacer POST /join otra vez con un token nuevo.
    if remove_from_db:
        manager.unregister_ws_code(ws_code)


async def _publish_mission_update(
    game_code: str,
    mqtt_event: str,
    player_key: str,
    mission_id: int,
    status: str = "confirmed",
) -> None:
    try:
        await publish_mission_event(
            game_code,
            mqtt_event,
            {
                "mission_id": mission_id,
                "player": player_key,
                "status": status,
            },
        )
    except Exception as mqtt_error:
        print(f"⚠️ No se pudo publicar evento MQTT de misión ({mqtt_event}): {mqtt_error}")


def _app_mission_ws_envelope(
    db: Session,
    *,
    game_code: str,
    mission_partida_id: int,
    player_id: int,
    action: str,
    mission_display_name: Optional[str] = None,
) -> dict[str, object]:
    """Campos alineados con broadcast MQTT (`mqtt_client`): misión, origen APP, join_code."""
    name = mission_display_name
    if name is None:
        mision = missions_crud.get_mission_by_game_mission_id(db, mission_partida_id)
        if mision is not None and (mision.nombre or "").strip():
            name = str(mision.nombre).strip()
    u = get_user(db, player_id)
    username = (u.nombre_usuario or "").strip() if u is not None else ""
    env: dict[str, object] = {
        "source": "app",
        "action": action,
        "nombre_juego": "APP",
        "join_code": game_code,
    }
    if name:
        env["mission_name"] = name
    if username:
        env["username"] = username
    return env


async def _sabotage_mission(
    db: Session,
    player_key: str,
    room_uuid: str,
    game_code: str,
    mission_id: int,
) -> bool:
    row = missions_crud.update_mission_sabotage(db, mission_id)
    if row is None:
        return False
    await manager.broadcast_to_room(
        {
            "type": "SABOTAGE",
            "player": player_key,
            "mission_id": mission_id,
        },
        room_uuid,
    )
    await _publish_mission_update(game_code, "sabotaged", player_key, mission_id)
    return True


async def _desabotage_mission(
    db: Session,
    player_key: str,
    room_uuid: str,
    game_code: str,
    mission_id: int,
) -> bool:
    row = missions_crud.update_mission_desabotage(db, mission_id)
    if row is None:
        return False
    await manager.broadcast_to_room(
        {
            "type": "DESABOTAGE",
            "player": player_key,
            "mission_id": mission_id,
        },
        room_uuid,
    )
    await _publish_mission_update(game_code, "desabotaged", player_key, mission_id)
    return True


async def _start_mission(
    db: Session,
    player_key: str,
    room_uuid: str,
    game_code: str,
    mission_id: int,
    player_id: int,
) -> bool:
    row = missions_crud.update_mission_start_time(db, mission_id)
    if row is None:
        return False
    extra = _app_mission_ws_envelope(
        db,
        game_code=game_code,
        mission_partida_id=mission_id,
        player_id=player_id,
        action="started",
    )
    await manager.broadcast_to_room(
        {
            "type": "START_MISSION",
            "player": player_key,
            "mission_id": mission_id,
            **extra,
        },
        room_uuid,
    )
    await _publish_mission_update(game_code, "started", player_key, mission_id)
    return True


async def _complete_mission(
    db: Session,
    player_key: str,
    room_uuid: str,
    game_code: str,
    mission_id: int,
    player_id: int,
) -> str:
    """Devuelve 'ok' | 'not_found' | 'already_done'."""
    mj_row, newly = missions_crud.update_mission_completion(db, mission_id)
    if mj_row is None:
        return "not_found"
    mission = missions_crud.get_mission_by_game_mission_id(db, mission_id)
    game_mission = missions_crud.get_game_mission_row(db, mission_id)
    if not newly:
        return "already_done"
    if game_mission is not None:
        games_crud.add_mission_completion_points(
            db, game_mission.id_partida, player_id, mission_id
        )
    if mission is not None and game_mission is not None:
        games_crud.update_reparacion_partida(
            db, game_mission.id_partida, mission.porcentaje_reparacion
        )
    mission_name = (
        str(mission.nombre).strip()
        if mission is not None and (mission.nombre or "").strip()
        else None
    )
    extra = _app_mission_ws_envelope(
        db,
        game_code=game_code,
        mission_partida_id=mission_id,
        player_id=player_id,
        action="completed",
        mission_display_name=mission_name,
    )
    await manager.broadcast_to_room(
        {
            "type": "COMPLETE_MISSION",
            "player": player_key,
            "mission_id": mission_id,
            **extra,
        },
        room_uuid,
    )
    await _publish_mission_update(game_code, "completed", player_key, mission_id)
    return "ok"


def _naive_utc(dt: object) -> Optional[datetime]:
    """Convierte fechas de BD a UTC naive para comparar con datetime.utcnow()."""
    if dt is None or not isinstance(dt, datetime):
        return None
    if dt.tzinfo is not None:
        return dt.astimezone(timezone.utc).replace(tzinfo=None)
    return dt


def _game_clock_payload(db: Session, g) -> dict[str, object]:
    """Límite y tiempo restante (segundos) para monitor / broadcast; alineado con `partidas.tiempo_limite_minutos`."""
    limit_min = max(0, int(getattr(g, "tiempo_limite_minutos", 0) or 0))
    limit_sec = limit_min * 60
    out: dict[str, object] = {
        "time_limit_seconds": limit_sec,
    }
    en_curso = lookup.get_game_state(db, "en_curso")
    in_progress = en_curso is not None and g.id_estado_partida == en_curso.id_estado_partida
    remaining = limit_sec
    if in_progress and g.fecha_inicio:
        start = _naive_utc(g.fecha_inicio)
        if start is not None:
            elapsed = int((datetime.utcnow() - start).total_seconds())
            remaining = max(0, limit_sec - elapsed)
    out["time_remaining"] = remaining
    return out


async def _broadcast_game_started(room_uuid: str, game_id: int, started: object) -> None:
    payload: dict[str, object] = {
        "type": "GAME_STARTED",
        "id_partida": game_id,
        "impostor_actual": getattr(started, "impostor_actual", None),
        "distribucion_misiones": getattr(started, "distribucion_misiones", {}),
    }
    with Session(engine) as db:
        g = games_crud.get_game_by_id(db, game_id)
        if g is not None:
            payload.update(_game_clock_payload(db, g))
    await manager.broadcast_to_room(payload, room_uuid)


async def _broadcast_game_ended(room_uuid: str, ended: object) -> None:
    fecha_fin = getattr(ended, "fecha_fin", None)
    await manager.broadcast_to_room(
        {
            "type": "GAME_ENDED",
            "id_partida": getattr(ended, "id_partida", None),
            "ganador_tripulacion": getattr(ended, "ganador_tripulacion", None),
            "fecha_fin": fecha_fin.isoformat() if fecha_fin else None,
        },
        room_uuid,
    )


# --- Endpoint WebSocket para la App Móvil ---
# ws_code = token devuelto en PartidaJoinResponse.ws_code tras POST /games/join
@app.websocket("/ws/join/{game_code}/{ws_code}")
async def websocket_endpoint(websocket: WebSocket, game_code: str, ws_code: str):
    with Session(engine) as db:
        game = games_crud.get_game_by_code(db, game_code)

    if game is None:
        await _ws_close_with_error(
            websocket,
            code="GAME_NOT_FOUND",
            message="Juego inexistente: no hay ninguna partida con este código.",
        )
        return

    with Session(engine) as db:
        ended = _game_has_ended(db, game)
    if ended:
        await _ws_close_with_error(
            websocket,
            code="GAME_ENDED",
            message="Esta partida ya ha finalizado.",
        )
        return

    room_uuid = manager.code_to_uuid.get(game_code)
    if not room_uuid:
        await _ws_close_with_error(
            websocket,
            code="GAME_NOT_LIVE",
            message="No hay sesión activa para este código. El anfitrión debe tener la partida creada en este servidor o se reinició; vuelve a crear la partida o pide un código nuevo.",
        )
        return

    player_id = manager.resolve_player_from_ws_code(ws_code, game_code)
    if player_id is None:
        await _ws_close_with_error(
            websocket,
            code="WS_CODE_INVALID",
            message="El token de conexión no es válido o no corresponde a esta partida. Vuelve a unirte desde la app.",
        )
        return

    game_id = game.id_partida
    is_creator = game.id_creador == player_id
    player_key = _player_public_id(player_id, ws_code)

    await manager.connect_player(websocket, room_uuid)

    display_name: Optional[str] = None
    if player_id is not None:
        with Session(engine) as db:
            display_name = games_crud.display_name_for_game_player(db, game_id, player_id)
    manager.register_live_player(websocket, room_uuid, player_key, display_name)
    joined_payload: dict[str, object] = {
        "event": "PLAYER_JOINED",
        "player": player_key,
        "room": room_uuid,
        "nombre_usuario": display_name,
        "player_name": display_name,
    }
    await manager.broadcast_to_room(joined_payload, room_uuid)

    handled_exit = False
    exit_reason: Optional[str] = None
    partida_service = PartidasService()
    try:
        while True:
            data = await websocket.receive_json()
            action = data.get("action")
            persist_event = action in ALLOWED_GAME_EVENT_TYPES
            # `reunion_emergencia` y `voto` se persisten dentro de su flujo dedicado.
            if action in {"reunion_emergencia", "voto"}:
                persist_event = False

            if action == "reunion_emergencia":
                with Session(engine) as db:
                    reunion_started = await _start_reunion(
                        db, room_uuid, game_id, game_code, player_id, player_key
                    )
                if not reunion_started:
                    await websocket.send_json({"type": "REUNION_ALREADY_ACTIVE"})

            elif action == "jugador_eliminado":
                target = _parse_int(data.get("id_usuario_afectado"))
                if target is not None:
                    with Session(engine) as db:
                        games_crud.mark_player_dead(db, game_id, target, killed_by=player_id)
                await manager.broadcast_to_room({
                    "type": "PLAYER_DIED",
                    "player": player_key,
                    "target_player": str(target) if target is not None else None,
                }, room_uuid)

            elif action == "mision_saboteada":
                with Session(engine) as db:
                    sabotage_candidate = missions_crud.get_random_sabotage_candidate_by_game(
                        db, game_id
                    )
                    if sabotage_candidate is None:
                        await websocket.send_json({"type": "SABOTAGE_NOT_AVAILABLE"})
                        continue
                    mission_id, requires_device = sabotage_candidate
                    data["id_mision_relacionada"] = mission_id
                    if game.presencial and requires_device:
                        await manager.broadcast_to_room(
                            {
                                "type": "SABOTAGE_PENDING",
                                "player": player_key,
                                "mission_id": mission_id,
                            },
                            room_uuid,
                        )
                        await _publish_mission_update(
                            game_code,
                            "sabotage_requested",
                            player_key,
                            mission_id,
                            status="pending_confirmation",
                        )
                    else:
                        ok = await _sabotage_mission(
                            db, player_key, room_uuid, game_code, mission_id
                        )
                        if not ok:
                            await websocket.send_json(
                                {
                                    "type": "MISSION_NOT_FOUND",
                                    "mission_id": mission_id,
                                    "message": "No hay registro de misión de jugador para esta instancia.",
                                }
                            )
                            continue
            elif action == "mision_desaboteada":
                mission_id = _parse_int(data.get("mission_id"))
                if mission_id is None:
                    await websocket.send_json({"type": "MISSION_ID_REQUIRED"})
                    continue
                with Session(engine) as db:
                    resolved = missions_crud.resolve_mision_partida_instance_id(
                        db, game_id, player_id, mission_id
                    )
                if resolved is None:
                    await websocket.send_json(
                        {
                            "type": "MISSION_NOT_FOUND",
                            "mission_id": mission_id,
                            "message": "Misión no encontrada o no asignada a este jugador en la partida.",
                        }
                    )
                    continue
                data["id_mision_relacionada"] = resolved
                with Session(engine) as db:
                    ok = await _desabotage_mission(
                        db, player_key, room_uuid, game_code, resolved
                    )
                if not ok:
                    await websocket.send_json(
                        {
                            "type": "MISSION_NOT_FOUND",
                            "mission_id": mission_id,
                            "resolved_mision_partida_id": resolved,
                            "message": "No hay registro de misión de jugador para esta instancia.",
                        }
                    )
                    continue
            elif action == "mision_iniciada":
                mission_id = _parse_int(data.get("mission_id"))
                if mission_id is None:
                    await websocket.send_json({"type": "MISSION_ID_REQUIRED"})
                    continue
                with Session(engine) as db:
                    resolved = missions_crud.resolve_mision_partida_instance_id(
                        db, game_id, player_id, mission_id
                    )
                if resolved is None:
                    await websocket.send_json(
                        {
                            "type": "MISSION_NOT_FOUND",
                            "mission_id": mission_id,
                            "message": "Misión no encontrada o no asignada a este jugador en la partida.",
                        }
                    )
                    continue
                data["id_mision_relacionada"] = resolved
                with Session(engine) as db:
                    ok = await _start_mission(
                        db, player_key, room_uuid, game_code, resolved, player_id
                    )
                if not ok:
                    await websocket.send_json(
                        {
                            "type": "MISSION_NOT_FOUND",
                            "mission_id": mission_id,
                            "resolved_mision_partida_id": resolved,
                            "message": "No hay registro de misión de jugador para esta instancia.",
                        }
                    )
                    continue
            elif action == "mision_completada":
                mission_id = _parse_int(data.get("mission_id"))
                print(mission_id)
                if mission_id is None:
                    await websocket.send_json({"type": "MISSION_ID_REQUIRED"})
                    continue
                with Session(engine) as db:
                    resolved = missions_crud.resolve_mision_partida_instance_id(
                        db, game_id, player_id, mission_id
                    )
                if resolved is None:
                    await websocket.send_json(
                        {
                            "type": "MISSION_NOT_FOUND",
                            "mission_id": mission_id,
                            "message": "Misión no encontrada o no asignada a este jugador en la partida.",
                        }
                    )
                    continue
                data["id_mision_relacionada"] = resolved
                with Session(engine) as db:
                    outcome = await _complete_mission(
                        db, player_key, room_uuid, game_code, resolved, player_id
                    )
                if outcome == "not_found":
                    await websocket.send_json(
                        {
                            "type": "MISSION_NOT_FOUND",
                            "mission_id": mission_id,
                            "resolved_mision_partida_id": resolved,
                            "message": "No hay registro de misión de jugador para esta instancia.",
                        }
                    )
                    continue
                if outcome == "already_done":
                    await websocket.send_json(
                        {
                            "type": "MISSION_ALREADY_COMPLETED",
                            "mission_id": mission_id,
                            "resolved_mision_partida_id": resolved,
                        }
                    )
                    continue
            elif action == "voto":
                target = _parse_int(data.get("id_usuario_afectado"))
                await manager.broadcast_to_room(
                    {
                        "type": "VOTE_CAST",
                        "player": player_key,
                        "target_player": str(target) if target is not None else None,
                    },
                    room_uuid,
                )
                with Session(engine) as db:
                    await _register_reunion_vote(db, room_uuid, player_id, target)
            elif action == "inicio_partida":
                if not is_creator:
                    await websocket.send_json({"type": "GAME_START_DENIED"})
                    continue
                try:
                    with Session(engine) as db:
                        started = partida_service.start_new_game(
                            db=db, creator=player_id, game_id=game_id
                        )
                except ValueError as exc:
                    await websocket.send_json(
                        {"type": "GAME_START_FAILED", "reason": str(exc)}
                    )
                    continue
                await _broadcast_game_started(room_uuid, game_id, started)
            elif action == "fin_partida":
                if not is_creator:
                    await websocket.send_json({"type": "GAME_END_DENIED"})
                    continue
                ganador = data.get("ganador_tripulacion")
                if isinstance(ganador, str):
                    ganador = ganador.lower() in {"true", "1", "yes"}
                try:
                    with Session(engine) as db:
                        ended = partida_service.end_game(
                            db=db, game_id=game_id, crew_won=ganador if isinstance(ganador, bool) else None
                        )
                except ValueError as exc:
                    await websocket.send_json(
                        {"type": "GAME_END_FAILED", "reason": str(exc)}
                    )
                    continue
                await _broadcast_game_ended(room_uuid, ended)
            elif action == "salir":
                await _complete_player_exit(
                    websocket,
                    room_uuid,
                    game_code,
                    game_id,
                    ws_code,
                    player_id,
                    "intentional",
                    remove_from_db=True,
                )
                handled_exit = True
                return
            else:
                # Generic relay: never let clients spoof device-ingress framing.
                if isinstance(data, dict) and data.get("source") == "mqtt":
                    continue
                await manager.broadcast_to_room(data, room_uuid)

            if persist_event:
                with Session(engine) as db:
                    raw_mision_rel = _parse_int(data.get("id_mision_relacionada"))
                    fk_mision_catalog: Optional[int] = None
                    if raw_mision_rel is not None:
                        fk_mision_catalog = missions_crud.resolve_catalog_mision_id_for_eventos(
                            db, raw_mision_rel
                        )
                    try:
                        create_game_event_by_type(
                            db,
                            game_id,
                            action,
                            player_id,
                            _parse_int(data.get("id_usuario_afectado")),
                            fk_mision_catalog,
                            data.get("descripcion"),
                        )
                    except Exception as persist_exc:
                        print(
                            f"⚠️ No se pudo persistir evento de partida "
                            f"(action={action!r}): {persist_exc}"
                        )
    except WebSocketDisconnect:
        exit_reason = "disconnect"
    except Exception:
        exit_reason = "connection_error"
    finally:
        if not handled_exit and exit_reason is not None:
            await _complete_player_exit(
                websocket, room_uuid, game_code, game_id, ws_code, player_id, exit_reason
            )


def _monitor_live_snapshot(db: Session, game_id: int) -> dict[str, object]:
    """Estado de partida en curso para el monitor: % reparación, puntuaciones, nombres."""
    out: dict[str, object] = {}
    g = games_crud.get_game_by_id(db, game_id)
    if g is None:
        return out
    en_curso = lookup.get_game_state(db, "en_curso")
    if en_curso is not None and g.id_estado_partida == en_curso.id_estado_partida:
        out["game_in_progress"] = True
    raw = g.porcentaje_reparacion_actual
    if raw is not None:
        try:
            out["progress"] = max(0, min(100, int(round(float(str(raw))))))
        except ValueError:
            out["progress"] = 0
    rows = games_crud.player_game(db, game_id)
    out["scores"] = {str(r.id_usuario): int(r.puntos_partida or 0) for r in rows}
    names: dict[str, str] = {}
    for r in rows:
        nm = games_crud.display_name_for_game_player(db, game_id, r.id_usuario) or ""
        nm = str(nm).strip()
        if nm:
            names[str(r.id_usuario)] = nm
    if names:
        out["player_names"] = names
    out.update(_game_clock_payload(db, g))
    return out


def _monitor_connected_players_payload(room_uuid: str, game_code: str, game_id: int) -> list[dict[str, object]]:
    """WS de juego activo + jugadores con token REST (create/join) sin WS (p. ej. creador en lobby)."""
    live_rows = manager.snapshot_connected_players(room_uuid)
    live_ids: set[int] = set()
    for row in live_rows:
        pid = _parse_int(row.get("player"))
        if pid is not None:
            live_ids.add(pid)

    pending_ids: set[int] = set()
    for _tok, (cp, uid) in manager.ws_sessions.items():
        if cp == game_code and uid not in live_ids:
            pending_ids.add(uid)

    extras: list[dict[str, object]] = []
    for uid in sorted(pending_ids):
        with Session(engine) as db:
            display_name = games_crud.display_name_for_game_player(db, game_id, uid)
        extras.append(
            {
                "player": str(uid),
                "nombre_usuario": display_name,
                "player_name": display_name,
                "connected": False,
            }
        )
    return live_rows + extras


@app.websocket("/ws/monitor/{game_code}")
async def monitor_websocket_endpoint(websocket: WebSocket, game_code: str):
    with Session(engine) as db:
        game = games_crud.get_game_by_code(db, game_code)

    if game is None:
        await _ws_close_with_error(
            websocket,
            code="GAME_NOT_FOUND",
            message="Juego inexistente: no hay ninguna partida con este código.",
        )
        return

    with Session(engine) as db:
        ended = _game_has_ended(db, game)

    # Partida terminada: datos desde BD, sin unir al canal en vivo (evita mezclar
    # con otra partida nueva que reutilice el mismo código).
    if ended:
        await websocket.accept()
        await websocket.send_json(
            {
                "type": "MONITOR_READ_ONLY",
                "reason": "game_ended",
                "id_partida": game.id_partida,
                "codigo_partida": game.codigo_partida,
                "nombre_partida": game.nombre_partida,
                "ganador_tripulacion": game.ganador_tripulacion,
                "fecha_fin": game.fecha_fin.isoformat() if game.fecha_fin else None,
                "porcentaje_reparacion_actual": str(game.porcentaje_reparacion_actual)
                if game.porcentaje_reparacion_actual is not None
                else None,
                "porcentaje_reparacion_victoria": str(game.porcentaje_reparacion_victoria)
                if game.porcentaje_reparacion_victoria is not None
                else None,
                "mensaje": "Esta partida ya terminó. Ves un resumen desde la base de datos; "
                "no recibirás eventos en vivo. Si alguien crea otra partida con el mismo código, "
                "será una partida distinta.",
            }
        )
        try:
            while True:
                await websocket.receive_text()
        except WebSocketDisconnect:
            pass
        return

    room_uuid = manager.code_to_uuid.get(game_code)
    if not room_uuid:
        await _ws_close_with_error(
            websocket,
            code="GAME_NOT_LIVE",
            message="No hay sesión en tiempo real para esta partida (por ejemplo, tras reiniciar el servidor). "
            "El código existe en base de datos pero no hay sala activa.",
        )
        return

    await manager.connect_monitor(websocket, room_uuid)
    try:
        with Session(engine) as db:
            snap = _monitor_live_snapshot(db, game.id_partida)
        payload: dict[str, object] = {
            "type": "MONITOR_CONNECTED",
            "game_code": game_code,
            "connected_players": _monitor_connected_players_payload(
                room_uuid, game_code, game.id_partida
            ),
        }
        payload.update(snap)
        await websocket.send_json(payload)
        while True:
            # El monitor es de solo lectura; mantenemos la conexión viva.
            await websocket.receive_text()
    except WebSocketDisconnect:
        pass
    finally:
        manager.disconnect(websocket, room_uuid)


@app.get("/")
def read_root():
    return {"mensaje": "Bienvenido al servidor del juego"}


app.include_router(games_router, prefix='/games')
app.include_router(users_router, prefix='/users')
