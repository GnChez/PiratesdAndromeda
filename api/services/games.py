from nanoid import generate
from sqlmodel import Session

import CRUD.games as games_crud
from CRUD.users import bulk_apply_game_stats
from CRUD.missions import get_missions_lite, get_missions_fisico
from CRUD.rooms import get_rooms
from CRUD.look_up import get_difficulty, get_game_state, get_player_mission_state
from schemas.games import (
    PartidaCreateData,
    PartidaEndResponse,
    PartidaInitialResponse,
    PartidaJoinRequest,
    PartidaJoinResponse,
    PartidaLeaveRequest,
    PartidaPedido,
    PartidaStartResponse,
)
from schemas.rooms import HabitacionBase
import random


class PartidasService:
    def create_game(self, db: Session, request: PartidaPedido) -> PartidaInitialResponse:
        estado_esperando = get_game_state(db, "esperando")
        dificultad_media = get_difficulty(db, "medio")
        missions = get_missions_lite(db) if not request.presencial else get_missions_fisico(db)
        rooms = (
            [HabitacionBase.model_validate(room) for room in get_rooms(db)]
            if request.presencial
            else request.habitaciones
        )

        if not estado_esperando or not dificultad_media:
            raise ValueError("Las tablas de catálogo no están inicializadas.")
        if not rooms:
            raise ValueError("No hay habitaciones disponibles para crear la partida.")
        if not missions:
            raise ValueError("No hay misiones disponibles para crear la partida.")

        data = PartidaCreateData(
            codigo_partida=self._generate_game_code(),
            nombre_partida=request.nombre_partida or self._default_name(),
            id_creador=request.id_creador,
            presencial=request.presencial,
            id_estado_partida=estado_esperando.id_estado_partida,
            num_jugadores=1,
            id_dificultad=dificultad_media.id_dificultad,
        )
        distribucion = self._distribute_missions_rooms(missions, rooms)
        game = games_crud.create_game(db, data)
        games_crud.add_player_to_game(
            db=db,
            game_id=game.id_partida,
            user_id=request.id_creador,
            nickname=None,
        )
        mission_ids = sorted(
            {mission_id for mission_list in distribucion.values() for mission_id in mission_list}
        )
        games_crud.create_game_missions(db, game.id_partida, mission_ids)
        return PartidaInitialResponse(
            id_creador=game.id_creador,
            nombre_partida=game.nombre_partida,
            presencial=game.presencial,
            habitaciones=rooms,
            id_partida=game.id_partida,
            codigo_partida=game.codigo_partida,
            id_estado_partida=game.id_estado_partida,
            habitaciones_misiones=distribucion,
            ws_code=self._generate_ws_code(),
            ws_room_code=self._generate_ws_code(),
        )

    def join_game(self, db: Session, join_req: PartidaJoinRequest) -> PartidaJoinResponse:
        join_row = games_crud.join_game(db, join_req)
        game = games_crud.get_game_by_id(db, join_row.id_partida)
        return PartidaJoinResponse(
            id_jugador=join_row.id_usuario,
            nombre_partida=game.nombre_partida if game else None,
            ws_code=self._generate_ws_code(),
        )

    def leave_game(self, db: Session, leave_req: PartidaLeaveRequest) -> bool:
        return games_crud.leave_game(db, leave_req.codigo_partida, leave_req.id_jugador)

    def _generate_game_code(self) -> str:
        return generate('ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890', 6)

    def _generate_ws_code(self) -> str:
        return generate('0123456789_abcdefghijklmnopqrstuvwxyz-', 20)

    def _default_name(self) -> str:
        return 'Partida INCREIBLE'

    def _distribute_missions_rooms(self, misiones, habitaciones) -> dict:
        misiones_tmp = list(misiones)
        random.shuffle(misiones_tmp)
        if not habitaciones:
            raise ValueError("No hay habitaciones para distribuir las misiones.")
        asignacion = {(hab.model_dump())["nombre"]: [] for hab in habitaciones}
        num_habitaciones = len(habitaciones)
        for i, mision in enumerate(misiones_tmp):
            hab_aleatoria = (habitaciones[i % num_habitaciones]).model_dump()["nombre"]
            asignacion[hab_aleatoria].append(mision.model_dump()["id_mision"])
        return asignacion

    def _distribute_missions_players(self, mission_rows, jugadores) -> dict:
        if not jugadores:
            return {}
        misiones_tmp = list(mission_rows)
        random.shuffle(misiones_tmp)
        asignacion = {jug.id_usuario: [] for jug in jugadores}
        num_jugadores = len(jugadores)
        for i, mision in enumerate(misiones_tmp):
            jugador_id = jugadores[i % num_jugadores].id_usuario
            asignacion[jugador_id].append(mision.id_mision)
        return asignacion

    def start_new_game(self, db: Session, creator: int, game_id: int) -> PartidaStartResponse:
        game = games_crud.get_game_by_id(db, game_id)
        if game is None:
            raise ValueError(f"La partida {game_id} no existe.")
        if game.id_creador != creator:
            raise ValueError("Solo el creador puede iniciar la partida.")
        mission_rows = games_crud.get_mission_rows_by_game(db, game_id)
        players = games_crud.player_game(db, game_id)
        if not players:
            raise ValueError("No hay jugadores en la partida.")
        distribucion = self._distribute_missions_players(mission_rows, players)
        estado_pendiente = get_player_mission_state(db, "pendiente")
        if not estado_pendiente:
            raise ValueError("No existe el estado de misión inicial 'pendiente'.")
        games_crud.create_player_mission_rows(
            db=db,
            game_id=game_id,
            mission_distribution=distribucion,
            initial_state_id=estado_pendiente.id_estado_mision,
        )
        played_game = games_crud.start_game(db, game_id)

        return PartidaStartResponse(
            id_partida=played_game.id_partida,
            id_estado_partida=played_game.id_estado_partida,
            impostor_actual=played_game.impostor_actual,
            distribucion_misiones=distribucion,
        )

    def end_game(
        self,
        db: Session,
        game_id: int,
        crew_won: bool | None = None,
    ) -> PartidaEndResponse:
        game = games_crud.get_game_by_id(db, game_id)
        if game is None:
            raise ValueError(f"La partida {game_id} no existe.")
        finished = games_crud.end_game(db, game_id, crew_won=crew_won)
        if finished is None:
            raise ValueError("No se pudo cerrar la partida.")
        players = games_crud.player_game(db, game_id)
        bulk_apply_game_stats(
            db,
            impostor_id=finished.impostor_actual,
            crew_won=bool(finished.ganador_tripulacion),
            player_rows=players,
        )
        return PartidaEndResponse(
            id_partida=finished.id_partida,
            id_estado_partida=finished.id_estado_partida,
            ganador_tripulacion=finished.ganador_tripulacion,
            fecha_fin=finished.fecha_fin,
            crew_won=bool(finished.ganador_tripulacion),
        )
