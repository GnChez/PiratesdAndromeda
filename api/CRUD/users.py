from datetime import datetime
from typing import Iterable

from sqlmodel import Session, select
from models.users import Usuarios
from schemas.users import *


def get_user(db: Session, user_id: int) -> Usuarios:
    return db.exec(select(Usuarios).where(Usuarios.id_usuario == user_id)).first()


def validate_unique_email(db: Session, user_email: str):
    return db.exec(select(Usuarios).where(Usuarios.email == user_email)).first()

def validate_unique_username(db: Session, user_username: str):
    return db.exec(select(Usuarios).where(Usuarios.nombre_usuario == user_username)).first()

def create_user(db: Session, user_info: UserCreate):
    payload = user_info.model_dump()
    payload["fecha_ultima_conexion"] = datetime.utcnow()
    user = Usuarios(**payload)
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


def touch_last_login(db: Session, user: Usuarios) -> Usuarios:
    user.fecha_ultima_conexion = datetime.utcnow()
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


def increment_user_stats(
    db: Session,
    user_id: int,
    *,
    partidas: int = 0,
    puntos: int = 0,
    impostor: int = 0,
    superviviente: int = 0,
    eliminado: int = 0,
) -> Usuarios | None:
    user = get_user(db, user_id)
    if user is None:
        return None
    user.total_partidas_jugadas = (user.total_partidas_jugadas or 0) + partidas
    user.total_puntos_acumulados = (user.total_puntos_acumulados or 0) + puntos
    user.veces_impostor = (user.veces_impostor or 0) + impostor
    user.veces_superviviente = (user.veces_superviviente or 0) + superviviente
    user.veces_eliminado = (user.veces_eliminado or 0) + eliminado
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


def bulk_apply_game_stats(
    db: Session,
    *,
    impostor_id: int | None,
    crew_won: bool,
    player_rows: Iterable,
) -> None:
    """Aplica al final de la partida los contadores agregados en `usuarios`."""
    for row in player_rows:
        is_impostor = impostor_id is not None and row.id_usuario == impostor_id
        is_alive = bool(row.jugador_vivo)
        increment_user_stats(
            db,
            row.id_usuario,
            partidas=1,
            puntos=int(row.puntos_partida or 0),
            impostor=1 if is_impostor else 0,
            superviviente=1 if (crew_won and not is_impostor and is_alive) else 0,
            eliminado=0 if is_alive else 1,
        )
