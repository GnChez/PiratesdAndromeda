from typing import List, Optional

from sqlmodel import Session, select

from models.events import EventosPartida, Votos
from models.look_up import TipoEvento
from schemas.events import EventosPartidaCreate, VotosCreate


ALLOWED_GAME_EVENT_TYPES: tuple[str, ...] = (
    "inicio_partida",
    "fin_partida",
    "mision_iniciada",
    "mision_completada",
    "mision_saboteada",
    "jugador_eliminado",
    "impostor_descubierto",
    "cambio_impostor",
    "jugador_desconectado",
    "jugador_reconectado",
    "reunion_emergencia",
    "votacion_iniciada",
    "voto",
    "pausa",
    "reanudacion",
    "otro",
)


def get_allowed_game_event_types() -> tuple[str, ...]:
    return ALLOWED_GAME_EVENT_TYPES


def get_event_type_by_description(db: Session, description: str) -> Optional[TipoEvento]:
    return db.exec(select(TipoEvento).where(TipoEvento.descripcion == description)).first()


def ensure_event_types_seeded(db: Session) -> List[TipoEvento]:
    """
    Inserta en tipo_evento los valores permitidos que falten.
    """
    existing = db.exec(select(TipoEvento)).all()
    existing_by_desc = {row.descripcion: row for row in existing}

    created = False
    for event_type in ALLOWED_GAME_EVENT_TYPES:
        if event_type in existing_by_desc:
            continue
        db.add(TipoEvento(descripcion=event_type))
        created = True

    if created:
        db.commit()

    return db.exec(select(TipoEvento)).all()


def create_game_event(db: Session, event_data: EventosPartidaCreate) -> EventosPartida:
    event = EventosPartida(**event_data.model_dump())
    db.add(event)
    db.commit()
    db.refresh(event)
    return event


def create_game_event_by_type(
    db: Session,
    id_partida: int,
    event_type: str,
    id_usuario_origen: Optional[int] = None,
    id_usuario_afectado: Optional[int] = None,
    id_mision_relacionada: Optional[int] = None,
    descripcion: Optional[str] = None,
) -> EventosPartida:
    if event_type not in ALLOWED_GAME_EVENT_TYPES:
        raise ValueError(f"Tipo de evento no permitido: {event_type}")

    ensure_event_types_seeded(db)
    tipo_evento = get_event_type_by_description(db, event_type)
    if not tipo_evento:
        raise ValueError(f"No se pudo resolver el tipo de evento: {event_type}")

    payload = EventosPartidaCreate(
        id_partida=id_partida,
        id_tipo_evento=tipo_evento.id_tipo_evento,
        id_usuario_origen=id_usuario_origen,
        id_usuario_afectado=id_usuario_afectado,
        id_mision_relacionada=id_mision_relacionada,
        descripcion=descripcion,
    )
    return create_game_event(db, payload)


def get_game_event(db: Session, event_id: int) -> Optional[EventosPartida]:
    return db.exec(select(EventosPartida).where(EventosPartida.id_evento == event_id)).first()


def get_events_by_game(db: Session, game_id: int) -> List[EventosPartida]:
    stmt = (
        select(EventosPartida)
        .where(EventosPartida.id_partida == game_id)
        .order_by(EventosPartida.timestamp_evento.desc())
    )
    return db.exec(stmt).all()


def delete_game_event(db: Session, event_id: int) -> bool:
    event = get_game_event(db, event_id)
    if not event:
        return False
    db.delete(event)
    db.commit()
    return True


def get_votes_by_event(db: Session, event_id: int) -> List[Votos]:
    stmt = select(Votos).where(Votos.id_evento == event_id)
    return db.exec(stmt).all()
