from datetime import datetime
from typing import List

from sqlalchemy import func
from sqlmodel import Session, select
from models.missions import Misiones
from models.links import MisionesPartida, MisionesPartidaJugador
from schemas.missions import MisionBase


def get_missions_lite(db: Session) -> List[Misiones]:
    statement = select(Misiones).where(Misiones.disponible_modo_lite == True)
    return db.exec(statement).all()


def get_missions_fisico(db: Session) -> List[Misiones]:
    statement = select(Misiones).where(Misiones.disponible_modo_lite == False)
    return db.exec(statement).all()


def get_mission(db: Session, mission_id: int) -> Misiones | None:
    return db.exec(select(Misiones).where(Misiones.id_mision == mission_id)).first()


def get_mission_by_game_mission_id(db: Session, mission_game_id: int) -> Misiones | None:
    return db.exec(
        select(Misiones)
        .join(MisionesPartida, MisionesPartida.id_mision == Misiones.id_mision)
        .where(MisionesPartida.id_mision_partida == mission_game_id)
    ).first()


def get_game_mission_row(db: Session, mission_game_id: int) -> MisionesPartida | None:
    return db.exec(
        select(MisionesPartida).where(MisionesPartida.id_mision_partida == mission_game_id)
    ).first()


def resolve_mision_partida_instance_id(
    db: Session,
    game_id: int,
    player_id: int,
    mission_ref: int,
) -> int | None:
    by_instance = db.exec(
        select(MisionesPartidaJugador.id_mision_partida)
        .join(
            MisionesPartida,
            MisionesPartida.id_mision_partida
            == MisionesPartidaJugador.id_mision_partida,
        )
        .where(
            MisionesPartida.id_partida == game_id,
            MisionesPartidaJugador.id_jugador == player_id,
            MisionesPartidaJugador.id_mision_partida == mission_ref,
        )
    ).first()
    if by_instance is not None:
        return by_instance

    return db.exec(
        select(MisionesPartidaJugador.id_mision_partida)
        .join(
            MisionesPartida,
            MisionesPartida.id_mision_partida
            == MisionesPartidaJugador.id_mision_partida,
        )
        .where(
            MisionesPartida.id_partida == game_id,
            MisionesPartidaJugador.id_jugador == player_id,
            MisionesPartida.id_mision == mission_ref,
        )
    ).first()


def get_random_sabotage_candidate_by_game(
    db: Session, game_id: int
) -> tuple[int, bool] | None:
    """
    Devuelve una misión de la partida que puede sabotearse:
    - id_mision_partida
    - requiere_dispositivo_fisico
    """
    row = db.exec(
        select(
            MisionesPartida.id_mision_partida,
            Misiones.requiere_dispositivo_fisico,
        )
        .join(Misiones, Misiones.id_mision == MisionesPartida.id_mision)
        .where(
            MisionesPartida.id_partida == game_id,
            Misiones.puede_ser_saboteada == True,
        )
        .order_by(func.random())
        .limit(1)
    ).first()
    if not row:
        return None
    return row[0], row[1]


def _get_mision_partida_jugador(db: Session, mission_id: int) -> MisionesPartidaJugador | None:
    """
    Devuelve la fila de MisionesPartidaJugador asociada a la misión de partida indicada.
    """
    return db.exec(
        select(MisionesPartidaJugador).where(
            MisionesPartidaJugador.id_mision_partida == mission_id
        )
    ).first()


def get_mission_player_row(db: Session, mission_id: int) -> MisionesPartidaJugador | None:
    return _get_mision_partida_jugador(db, mission_id)


def update_mission_start_time(db: Session, mission_id: int) -> MisionesPartidaJugador | None:
    row = _get_mision_partida_jugador(db, mission_id)
    if not row:
        return None
    row.fecha_inicio = datetime.now()
    row.id_estado_mision = 2
    db.add(row)
    db.commit()
    db.refresh(row)
    return row


def update_mission_completion(db: Session, mission_id: int) -> MisionesPartidaJugador | None:
    row = _get_mision_partida_jugador(db, mission_id)
    if not row:
        return None
    row.fecha_completada = datetime.now()
    row.id_estado_mision = 3
    db.add(row)
    db.commit()
    db.refresh(row)
    return row


def update_mission_sabotage(db: Session, mission_id: int) -> MisionesPartidaJugador | None:
    row = _get_mision_partida_jugador(db, mission_id)
    if not row:
        return None
    row.id_estado_mision = 5
    db.add(row)
    db.commit()
    db.refresh(row)
    return row

def update_mission_desabotage(db: Session, mission_id: int) -> MisionesPartidaJugador | None:
    row = _get_mision_partida_jugador(db, mission_id)
    if not row:
        return None
    row.id_estado_mision = 16
    db.add(row)
    db.commit()
    db.refresh(row)
    return row