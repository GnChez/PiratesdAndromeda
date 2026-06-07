"""
Inserta los registros mínimos de catálogo necesarios para que los flujos del
juego (crear/unirse/iniciar partida, eventos, misiones) puedan operar sobre
una base de datos recién creada.

Es idempotente: solo inserta lo que falta.
"""
from sqlmodel import Session, select

from models.look_up import (
    Dificultad,
    EstadoConexion,
    EstadoMisionJugador,
    EstadoPartida,
    RolSistema,
    TipoDispositivo,
    TipoEvento,
    TipoHabitacion,
    TipoMision,
)


_ROL_SISTEMA = [
    (1, "jugador"),
    (2, "admin"),
]

_ESTADO_PARTIDA = [
    (1, "esperando"),
    (2, "en_curso"),
    (3, "finalizada"),
    (4, "cancelada"),
]

_DIFICULTAD = [
    (1, "facil"),
    (2, "medio"),
    (3, "dificil"),
]

_TIPO_HABITACION = [
    (1, "puente"),
    (2, "motor"),
    (3, "comunicaciones"),
    (4, "armeria"),
    (5, "laboratorio"),
    (6, "cocina"),
    (7, "almacen"),
    (8, "sala_espiritus"),
    (9, "generico"),
]

_TIPO_MISION = [
    (1, "tecnica"),
    (2, "fisica"),
    (3, "logica"),
    (4, "exploracion"),
]

_TIPO_DISPOSITIVO = [
    (1, "sensor"),
    (2, "actuador"),
    (3, "panel"),
]

_ESTADO_CONEXION = [
    (1, "conectado"),
    (2, "desconectado"),
    (3, "error"),
]

_ESTADO_MISION_JUGADOR = [
    (1, "pendiente"),
    (2, "en_curso"),
    (3, "completada"),
    (4, "fallida"),
    (5, "saboteada"),
    (16, "desabotaged"),
]

_TIPO_EVENTO = [
    "inicio_partida",
    "fin_partida",
    "mision_iniciada",
    "mision_completada",
    "mision_saboteada",
    "mision_desaboteada",
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
]

def _seed_with_id(db: Session, model, rows: list[tuple[int, str]], id_field: str, name_field: str) -> None:
    # Remove the [0] index; the results are already the values you need
    existing_ids = {val for val in db.exec(select(getattr(model, id_field))).all()}
    existing_names = {val for val in db.exec(select(getattr(model, name_field))).all()}

    for row_id, name in rows:
        if row_id in existing_ids or name in existing_names:
            continue
        db.add(model(**{id_field: row_id, name_field: name}))


def seed_catalog(db: Session) -> None:
    _seed_with_id(db, RolSistema, _ROL_SISTEMA, "id_rol", "nombre_rol")
    _seed_with_id(db, EstadoPartida, _ESTADO_PARTIDA, "id_estado_partida", "nombre_estado")
    _seed_with_id(db, Dificultad, _DIFICULTAD, "id_dificultad", "nombre_dificultad")
    _seed_with_id(db, TipoHabitacion, _TIPO_HABITACION, "id_tipo_habitacion", "nombre_tipo")
    _seed_with_id(db, TipoMision, _TIPO_MISION, "id_tipo_mision", "nombre_tipo")
    _seed_with_id(db, TipoDispositivo, _TIPO_DISPOSITIVO, "id_tipo_dispositivo", "nombre_tipo")
    _seed_with_id(db, EstadoConexion, _ESTADO_CONEXION, "id_estado_conexion", "nombre_estado")
    _seed_with_id(db, EstadoMisionJugador, _ESTADO_MISION_JUGADOR, "id_estado_mision", "nombre_estado")

    existing_event_types = {row for row in db.exec(select(TipoEvento.descripcion)).all()}
    for descripcion in _TIPO_EVENTO:
        if descripcion in existing_event_types:
            continue
        db.add(TipoEvento(descripcion=descripcion))

    db.commit()
