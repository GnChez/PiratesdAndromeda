from fastapi import APIRouter, Depends, HTTPException
from sqlmodel import Session

from CRUD.events import create_game_event_by_type
from CRUD.games import get_game_by_code
from db.dbconnection import get_session
from services.games import PartidasService
from schemas.games import (
    PartidaEndRequest,
    PartidaEndResponse,
    PartidaInitialResponse,
    PartidaJoinRequest,
    PartidaJoinResponse,
    PartidaLeaveRequest,
    PartidaLeaveResponse,
    PartidaPedido,
    PartidaStartResponse,
)
from core.socket_manager import manager

router = APIRouter()


@router.post("/create", response_model=PartidaInitialResponse, tags=["GAMES"])
def create_game(game_info: PartidaPedido, db: Session = Depends(get_session)):
    partida_service = PartidasService()
    partida = partida_service.create_game(db=db, request=game_info)
    manager.create_game(partida.codigo_partida, partida.ws_room_code)
    manager.register_ws_session(partida.ws_code, partida.codigo_partida, partida.id_creador)
    return partida


@router.post("/join", response_model=PartidaJoinResponse, tags=["GAMES"])
def join_game(join_req: PartidaJoinRequest, db: Session = Depends(get_session)):
    partida_service = PartidasService()
    try:
        partida = partida_service.join_game(db=db, join_req=join_req)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    manager.register_ws_session(partida.ws_code, join_req.codigo_partida, partida.id_jugador)
    return partida


@router.post("/leave", response_model=PartidaLeaveResponse, tags=["GAMES"])
async def leave_game(leave_req: PartidaLeaveRequest, db: Session = Depends(get_session)):
    partida_service = PartidasService()
    game = get_game_by_code(db, leave_req.codigo_partida)
    ok = partida_service.leave_game(db=db, leave_req=leave_req)
    if ok:
        manager.unregister_by_game_and_player(leave_req.codigo_partida, leave_req.id_jugador)
        room_uuid = manager.code_to_uuid.get(leave_req.codigo_partida)
        if room_uuid:
            await manager.broadcast_to_room(
                {
                    "event": "PLAYER_LEFT",
                    "player": str(leave_req.id_jugador),
                    "reason": "rest",
                },
                room_uuid,
            )
        if game is not None:
            try:
                create_game_event_by_type(
                    db,
                    game.id_partida,
                    "jugador_desconectado",
                    leave_req.id_jugador,
                )
            except Exception:
                pass
    return PartidaLeaveResponse(left=ok)


@router.post("/start", response_model=PartidaStartResponse, tags=["GAMES"])
async def start_game(creator: int, game_id: int, db: Session = Depends(get_session)):
    partida_service = PartidasService()
    try:
        result = partida_service.start_new_game(db=db, creator=creator, game_id=game_id)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    game = get_game_by_code_by_id(db, result.id_partida)
    if game is not None:
        room_uuid = manager.code_to_uuid.get(game.codigo_partida)
        if room_uuid:
            await manager.broadcast_to_room(
                {
                    "type": "GAME_STARTED",
                    "id_partida": result.id_partida,
                    "impostor_actual": result.impostor_actual,
                    "distribucion_misiones": result.distribucion_misiones,
                },
                room_uuid,
            )
        try:
            create_game_event_by_type(db, result.id_partida, "inicio_partida", creator)
        except Exception:
            pass
    return result


@router.post("/end", response_model=PartidaEndResponse, tags=["GAMES"])
async def end_game(end_req: PartidaEndRequest, db: Session = Depends(get_session)):
    partida_service = PartidasService()
    try:
        result = partida_service.end_game(
            db=db,
            game_id=end_req.id_partida,
            crew_won=end_req.ganador_tripulacion,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    game = get_game_by_code_by_id(db, end_req.id_partida)
    if game is not None:
        room_uuid = manager.code_to_uuid.get(game.codigo_partida)
        if room_uuid:
            await manager.broadcast_to_room(
                {
                    "type": "GAME_ENDED",
                    "id_partida": result.id_partida,
                    "ganador_tripulacion": result.ganador_tripulacion,
                    "fecha_fin": result.fecha_fin.isoformat() if result.fecha_fin else None,
                },
                room_uuid,
            )
        try:
            create_game_event_by_type(
                db,
                result.id_partida,
                "fin_partida",
                descripcion=("crew_won" if result.crew_won else "impostor_won"),
            )
        except Exception:
            pass
    return result


def get_game_by_code_by_id(db: Session, game_id: int):
    """Helper local: recupera la partida por id (usa el CRUD reutilizable)."""
    from CRUD.games import get_game_by_id
    return get_game_by_id(db, game_id)
