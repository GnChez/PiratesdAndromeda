from decimal import Decimal
from typing import List

from sqlalchemy.exc import IntegrityError
from sqlmodel import Session, select
from datetime import datetime
from sqlalchemy import func
from models.games import Partidas, JugadoresPartida
from models.links import MisionesPartida, MisionesPartidaJugador
from schemas.games import PartidaCreateData, PartidaJoinRequest
import CRUD.look_up as lookup


def get_game_by_id(db: Session, game_id: int) -> Partidas | None:
    return db.exec(select(Partidas).where(Partidas.id_partida == game_id)).first()


def get_game_by_player(db: Session, creator_id: int) -> Partidas | None:
    return db.exec(select(Partidas).where(Partidas.id_creador == creator_id)).first()


def get_game_by_code(db: Session, game_code: str) -> Partidas | None:
    return db.exec(select(Partidas).where(Partidas.codigo_partida == game_code)).first()


def start_game(db: Session, game_id: int) -> Partidas:
    """Marca la partida como `en_curso`, fija el impostor y la fecha de inicio."""
    played_game = get_game_by_id(db, game_id)
    if played_game is None:
        raise ValueError(f"La partida {game_id} no existe.")
    estado_en_curso = lookup.get_game_state(db, "en_curso")
    if estado_en_curso is None:
        raise ValueError("Estado de partida 'en_curso' no inicializado en el catálogo.")
    try:
        played_game.id_estado_partida = estado_en_curso.id_estado_partida
        played_game.fecha_inicio = datetime.utcnow()
        played_game.impostor_actual = random_player(db, played_game.id_partida)
        db.add(played_game)
        db.commit()
        db.refresh(played_game)
    except IntegrityError as e:
        db.rollback()
        raise e

    return played_game


def end_game(
    db: Session,
    game_id: int,
    crew_won: bool | None = None,
) -> Partidas | None:
    """Cierra la partida. Si `crew_won` es None, se deriva del % de reparación."""
    game = get_game_by_id(db, game_id)
    if game is None:
        return None
    estado_finalizada = lookup.get_game_state(db, "finalizada")
    if estado_finalizada is None:
        raise ValueError("Estado de partida 'finalizada' no inicializado en el catálogo.")

    if crew_won is None:
        actual = game.porcentaje_reparacion_actual or Decimal("0")
        objetivo = game.porcentaje_reparacion_victoria or Decimal("100")
        crew_won = actual >= objetivo

    game.id_estado_partida = estado_finalizada.id_estado_partida
    game.fecha_fin = datetime.utcnow()
    game.ganador_tripulacion = bool(crew_won)
    db.add(game)
    db.commit()
    db.refresh(game)
    return game


def random_player(db: Session, game_id: int) -> int:
    random = db.exec(
        select(JugadoresPartida)
        .where(JugadoresPartida.id_partida == game_id)
        .order_by(func.random())
        .limit(1)
    ).first()
    if random is None:
        raise ValueError(f"La partida {game_id} no tiene jugadores para elegir impostor.")
    return random.id_usuario


def create_game(db: Session, game_info: PartidaCreateData):
    game = Partidas(**game_info.model_dump())
    db.add(game)
    db.commit()
    db.refresh(game)
    return game


def get_player_row(db: Session, game_id: int, user_id: int) -> JugadoresPartida | None:
    return db.exec(
        select(JugadoresPartida).where(
            JugadoresPartida.id_partida == game_id,
            JugadoresPartida.id_usuario == user_id,
        )
    ).first()


def count_players_in_game(db: Session, game_id: int) -> int:
    return db.exec(
        select(func.count())
        .select_from(JugadoresPartida)
        .where(JugadoresPartida.id_partida == game_id)
    ).one()


def _refresh_player_count(db: Session, game_id: int) -> None:
    game = get_game_by_id(db, game_id)
    if game is None:
        return
    game.num_jugadores = count_players_in_game(db, game_id)
    db.add(game)
    db.commit()


def add_player_to_game(
    db: Session, game_id: int, user_id: int, nickname: str | None = None
) -> JugadoresPartida:
    existing = get_player_row(db, game_id, user_id)
    if existing:
        return existing

    row = JugadoresPartida(
        id_partida=game_id,
        id_usuario=user_id,
        apodo_partida=nickname,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    _refresh_player_count(db, game_id)
    return row


def join_game(db: Session, player_game: PartidaJoinRequest) -> JugadoresPartida:
    estado_esperando = lookup.get_game_state(db, "esperando")
    if estado_esperando is None:
        raise ValueError("Estado de partida 'esperando' no inicializado en el catálogo.")
    game = db.exec(
        select(Partidas).where(
            Partidas.codigo_partida == player_game.codigo_partida,
            Partidas.id_estado_partida == estado_esperando.id_estado_partida,
        )
    ).first()
    if game is None:
        raise ValueError("La partida no existe o ya no admite nuevos jugadores.")
    return add_player_to_game(
        db=db,
        game_id=game.id_partida,
        user_id=player_game.id_jugador,
    )


def player_game(db: Session, game_id: int) -> List[JugadoresPartida]:
    stmt = select(JugadoresPartida).where(JugadoresPartida.id_partida == game_id)
    return db.exec(stmt).all()


def mark_player_dead(
    db: Session,
    game_id: int,
    user_id: int,
    killed_by: int | None = None,
) -> JugadoresPartida | None:
    row = get_player_row(db, game_id, user_id)
    if row is None:
        return None
    row.jugador_vivo = False
    row.eliminado_por = killed_by
    db.add(row)
    db.commit()
    db.refresh(row)
    return row


def leave_game(db: Session, codigo_partida: str, id_usuario: int) -> bool:
    """Elimina al jugador de la partida en base de datos. Devuelve False si no había fila."""
    game = db.exec(select(Partidas).where(Partidas.codigo_partida == codigo_partida)).first()
    if not game:
        return False
    row = db.exec(
        select(JugadoresPartida).where(
            JugadoresPartida.id_partida == game.id_partida,
            JugadoresPartida.id_usuario == id_usuario,
        )
    ).first()
    if not row:
        return False
    db.delete(row)
    db.commit()
    _refresh_player_count(db, game.id_partida)
    return True


def create_game_missions(db: Session, game_id: int, mission_ids: list[int]) -> list[MisionesPartida]:
    """Registra en misiones_partida las misiones activas de una partida."""
    if not mission_ids:
        return []

    saved_rows: list[MisionesPartida] = []
    for mission_id in mission_ids:
        row = MisionesPartida(id_partida=game_id, id_mision=mission_id)
        db.add(row)
        saved_rows.append(row)
    db.commit()
    for row in saved_rows:
        db.refresh(row)
    return saved_rows


def get_mission_rows_by_game(db: Session, game_id: int) -> List[MisionesPartida]:
    stmt = select(MisionesPartida).where(MisionesPartida.id_partida == game_id)
    return db.exec(stmt).all()


def create_player_mission_rows(
    db: Session,
    game_id: int,
    mission_distribution: dict[int, list[int]],
    initial_state_id: int,
) -> list[MisionesPartidaJugador]:
    """
    Crea filas en misiones_partida_jugador según la distribución misión->jugador.
    """
    mission_rows = get_mission_rows_by_game(db, game_id)
    mission_row_by_mission_id: dict[int, int] = {
        row.id_mision: row.id_mision_partida for row in mission_rows
    }

    saved_rows: list[MisionesPartidaJugador] = []
    for player_id, mission_ids in mission_distribution.items():
        for mission_id in mission_ids:
            mission_row_id = mission_row_by_mission_id.get(mission_id)
            if mission_row_id is None:
                continue
            row = MisionesPartidaJugador(
                id_mision_partida=mission_row_id,
                id_jugador=player_id,
                id_estado_mision=initial_state_id,
            )
            db.add(row)
            saved_rows.append(row)

    if not saved_rows:
        return []

    db.commit()
    for row in saved_rows:
        db.refresh(row)
    return saved_rows


def update_reparacion_partida(
    db: Session, game_id: int, porcentaje_reparacion: Decimal
) -> Partidas | None:
    game = db.exec(select(Partidas).where(Partidas.id_partida == game_id)).first()
    if not game:
        return None
    actual = game.porcentaje_reparacion_actual or Decimal("0")
    objetivo = game.porcentaje_reparacion_victoria or Decimal("100")
    nuevo = min(objetivo, actual + (porcentaje_reparacion or Decimal("0")))
    game.porcentaje_reparacion_actual = nuevo
    db.add(game)
    db.commit()
    db.refresh(game)
    return game
