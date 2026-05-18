import asyncio
from collections import Counter
from contextlib import asynccontextmanager
from datetime import datetime
from typing import Optional

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from sqlmodel import Session

import CRUD.games as games_crud
import CRUD.look_up as lookup
import CRUD.missions as missions_crud
import CRUD.users as users_crud
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
KILL_REWARD_POINTS = 50
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
) -> None:
    manager.disconnect(websocket, room_uuid)
    await _handle_reunion_player_exit(room_uuid, player_id)
    player_key = _player_public_id(player_id, ws_code)
    await manager.broadcast_to_room(
        {
            "event": "PLAYER_LEFT",
            "player": player_key,
            "reason": reason,
        },
        room_uuid,
    )
    if player_id is not None:
        try:
            _sync_remove_player_from_db(game_code, player_id)
        except Exception:
            pass
        if game_id is not None:
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


async def _sabotage_mission(
    db: Session,
    player_id: Optional[int],
    player_key: str,
    room_uuid: str,
    game_code: str,
    mission_id: int,
):
    player_score = None
    game_id_for_score = None
    mission_state = missions_crud.get_mission_player_row(db, mission_id)
    sabotaged_before = (
        mission_state is not None and mission_state.id_estado_mision == 5
    )
    missions_crud.update_mission_sabotage(db, mission_id)
    mission = missions_crud.get_mission_by_game_mission_id(db, mission_id)
    game_mission = missions_crud.get_game_mission_row(db, mission_id)
    if (
        not sabotaged_before
        and player_id is not None
        and mission is not None
        and game_mission is not None
    ):
        player_score = games_crud.award_player_sabotage_points(
            db,
            game_mission.id_partida,
            player_id,
            int(mission.puntos_sabotaje or 0),
        )
        game_id_for_score = game_mission.id_partida
    await manager.broadcast_to_room(
        {
            "type": "SABOTAGE",
            "player": player_key,
            "mission_id": mission_id,
        },
        room_uuid,
    )
    if game_id_for_score is not None:
        await _broadcast_score_updated(db, room_uuid, game_id_for_score, player_score)
    await _publish_mission_update(game_code, "sabotaged", player_key, mission_id)


async def _desabotage_mission(
    db: Session,
    player_key: str,
    room_uuid: str,
    game_code: str,
    mission_id: int,
):
    missions_crud.update_mission_desabotage(db, mission_id)
    await manager.broadcast_to_room(
        {
            "type": "DESABOTAGE",
            "player": player_key,
            "mission_id": mission_id,
        },
        room_uuid,
    )
    await _publish_mission_update(game_code, "desabotaged", player_key, mission_id)


async def _start_mission(
    db: Session,
    player_key: str,
    room_uuid: str,
    game_code: str,
    mission_id: int,
):
    missions_crud.update_mission_start_time(db, mission_id)
    await manager.broadcast_to_room(
        {
            "type": "START_MISSION",
            "player": player_key,
            "mission_id": mission_id,
        },
        room_uuid,
    )
    await _publish_mission_update(game_code, "started", player_key, mission_id)


async def _complete_mission(
    db: Session,
    player_key: str,
    room_uuid: str,
    game_code: str,
    mission_id: int,
):
    player_score = None
    game_id_for_score = None
    mission_state = missions_crud.get_mission_player_row(db, mission_id)
    completed_before = (
        mission_state is not None and mission_state.id_estado_mision == 3
    )
    missions_crud.update_mission_completion(db, mission_id)
    mission = missions_crud.get_mission_by_game_mission_id(db, mission_id)
    game_mission = missions_crud.get_game_mission_row(db, mission_id)
    if mission is not None and game_mission is not None:
        if not completed_before:
            games_crud.update_reparacion_partida(
                db, game_mission.id_partida, mission.porcentaje_reparacion
            )
            if mission_state is not None:
                player_score = games_crud.award_player_mission_points(
                    db,
                    game_mission.id_partida,
                    mission_state.id_jugador,
                    int(mission.puntos_otorgados or 0),
                )
                game_id_for_score = game_mission.id_partida
    await manager.broadcast_to_room(
        {
            "type": "COMPLETE_MISSION",
            "player": player_key,
            "mission_id": mission_id,
        },
        room_uuid,
    )
    if game_id_for_score is not None:
        await _broadcast_score_updated(db, room_uuid, game_id_for_score, player_score)
    await _publish_mission_update(game_code, "completed", player_key, mission_id)


async def _broadcast_game_started(room_uuid: str, game_id: int, started: object) -> None:
    with Session(engine) as db:
        game = games_crud.get_game_by_id(db, game_id)
    time_limit_seconds = int(getattr(game, "tiempo_limite_minutos", 60) or 60) * 60
    await manager.broadcast_to_room(
        {
            "type": "GAME_STARTED",
            "id_partida": game_id,
            "impostor_actual": getattr(started, "impostor_actual", None),
            "distribucion_misiones": getattr(started, "distribucion_misiones", {}),
            "time_remaining": time_limit_seconds,
            "remaining_time": time_limit_seconds,
        },
        room_uuid,
    )


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
    with Session(engine) as db:
        player_user = users_crud.get_user(db, player_id)
    player_name = (
        str(player_user.nombre_usuario)
        if player_user is not None and player_user.nombre_usuario
        else player_key
    )

    await manager.connect_player(websocket, room_uuid)

    await manager.broadcast_to_room({
        "event": "PLAYER_JOINED",
        "player": player_key,
        "room": room_uuid,
        "nombre_usuario": player_name,
        "player_name": player_name,
    }, room_uuid)

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
                score_update = None
                if target is not None:
                    with Session(engine) as db:
                        target_row = games_crud.get_player_row(db, game_id, target)
                        was_alive = bool(target_row and target_row.jugador_vivo)
                        games_crud.mark_player_dead(db, game_id, target, killed_by=player_id)
                        if was_alive and target != player_id:
                            player_score = games_crud.award_player_kill_points(
                                db,
                                game_id,
                                player_id,
                                KILL_REWARD_POINTS,
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
                                    "scores": games_crud.get_game_scores(db, game_id),
                                }
                await manager.broadcast_to_room({
                    "type": "PLAYER_DIED",
                    "player": player_key,
                    "target_player": str(target) if target is not None else None,
                }, room_uuid)
                if score_update is not None:
                    await manager.broadcast_to_room(score_update, room_uuid)

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
                        await _sabotage_mission(
                            db, player_id, player_key, room_uuid, game_code, mission_id
                        )
            elif action == "mision_desaboteada":
                mission_id = _parse_int(data.get("mission_id"))
                if mission_id is None:
                    await websocket.send_json({"type": "MISSION_ID_REQUIRED"})
                    continue
                data["id_mision_relacionada"] = mission_id
                with Session(engine) as db:
                    await _desabotage_mission(
                        db, player_key, room_uuid, game_code, mission_id
                    )
            elif action == "mision_iniciada":
                mission_id = _parse_int(data.get("mission_id"))
                if mission_id is None:
                    await websocket.send_json({"type": "MISSION_ID_REQUIRED"})
                    continue
                data["id_mision_relacionada"] = mission_id
                with Session(engine) as db:
                    await _start_mission(
                        db, player_key, room_uuid, game_code, mission_id
                    )
            elif action == "mision_completada":
                mission_id = _parse_int(data.get("mission_id"))
                if mission_id is None:
                    await websocket.send_json({"type": "MISSION_ID_REQUIRED"})
                    continue
                data["id_mision_relacionada"] = mission_id
                with Session(engine) as db:
                    await _complete_mission(
                        db, player_key, room_uuid, game_code, mission_id
                    )
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
                    websocket, room_uuid, game_code, game_id, ws_code, player_id, "intentional"
                )
                handled_exit = True
                return
            else:
                await manager.broadcast_to_room(data, room_uuid)

            if persist_event:
                with Session(engine) as db:
                    create_game_event_by_type(
                        db,
                        game_id,
                        action,
                        player_id,
                        _parse_int(data.get("id_usuario_afectado")),
                        _parse_int(data.get("id_mision_relacionada")),
                        data.get("descripcion"),
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
            live_game = games_crud.get_game_by_code(db, game_code)
            estado_en_curso = lookup.get_game_state(db, "en_curso")
            player_rows = games_crud.player_game(db, live_game.id_partida) if live_game else []
            scores = (
                games_crud.get_game_scores(db, live_game.id_partida)
                if live_game
                else {}
            )
            player_names: dict[str, str] = {}
            connected_players: list[dict[str, object]] = []
            for row in player_rows:
                user = users_crud.get_user(db, row.id_usuario)
                display_name = (
                    str(user.nombre_usuario)
                    if user is not None and user.nombre_usuario
                    else str(row.id_usuario)
                )
                player_id = str(row.id_usuario)
                player_names[player_id] = display_name
                connected_players.append(
                    {
                        "player": player_id,
                        "nombre_usuario": display_name,
                        "player_name": display_name,
                        "connected": True,
                    }
                )

        game_in_progress = (
            live_game is not None
            and estado_en_curso is not None
            and live_game.id_estado_partida == estado_en_curso.id_estado_partida
        )
        total_seconds = int(getattr(live_game, "tiempo_limite_minutos", 60) or 60) * 60
        time_remaining = total_seconds
        if game_in_progress and getattr(live_game, "fecha_inicio", None):
            elapsed = int((datetime.utcnow() - live_game.fecha_inicio).total_seconds())
            time_remaining = max(0, total_seconds - elapsed)

        await websocket.send_json(
            {
                "type": "MONITOR_CONNECTED",
                "game_code": game_code,
                "game_in_progress": game_in_progress,
                "time_remaining": time_remaining if game_in_progress else None,
                "remaining_time": time_remaining if game_in_progress else None,
                "progress": float(live_game.porcentaje_reparacion_actual or 0)
                if live_game is not None
                else 0,
                "scores": scores,
                "player_names": player_names,
                "connected_players": connected_players,
            }
        )
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
