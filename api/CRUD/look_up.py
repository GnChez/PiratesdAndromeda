from sqlmodel import Session, select
from sqlmodel import select, and_

from models.look_up import EstadoPartida, Dificultad, EstadoMisionJugador
from schemas.games import PartidaCreateData, PartidaJoinRequest, JugadorPartidaCreate


def get_game_state(db: Session, state: str) -> EstadoPartida:
    return db.exec(select(EstadoPartida).filter_by(nombre_estado=state)).first()


def get_difficulty(db: Session, difficulty: str) -> Dificultad:
    return db.exec(select(Dificultad).filter_by(nombre_dificultad=difficulty)).first()


def get_player_mission_state(db: Session, state: str) -> EstadoMisionJugador:
    return db.exec(select(EstadoMisionJugador).filter_by(nombre_estado=state)).first()
