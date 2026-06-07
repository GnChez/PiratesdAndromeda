from typing import List

from sqlmodel import Session, select
from models.rooms import Habitaciones
from schemas.missions import MisionBase


def get_rooms(db: Session) -> List[type(Habitaciones)]:
    return db.exec(select(Habitaciones)).all()
