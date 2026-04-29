from pydantic import BaseModel, ConfigDict, Field
from typing import Optional, List
from decimal import Decimal
from datetime import datetime
from schemas.rooms import HabitacionBase

class PartidaPedido(BaseModel):
    id_creador: int
    nombre_partida: Optional[str] = None
    presencial: bool = False
    habitaciones: List[HabitacionBase]


class PartidaCreateData(BaseModel):
    id_creador: int
    nombre_partida: Optional[str] = None
    presencial: bool = False
    codigo_partida: str
    id_estado_partida: int
    num_jugadores: int
    num_min_tripulantes_victoria: int = 2
    porcentaje_reparacion_victoria: Decimal = Decimal("100.00")
    numero_impostores: int = 1
    tiempo_limite_minutos: int = 60
    id_dificultad: int = 2


class PartidaUpdate(BaseModel):
    id_estado_partida: Optional[int] = None
    porcentaje_reparacion_actual: Optional[Decimal] = None
    impostor_actual: Optional[int] = None
    numero_impostores_descubiertos: Optional[int] = None
    ganador_tripulacion: Optional[bool] = None
    fecha_fin: Optional[datetime] = None


class PartidaInitialResponse(PartidaPedido):
    id_partida: int
    codigo_partida: str
    id_estado_partida: int
    habitaciones_misiones: dict
    ws_code: str
    ws_room_code: str
    model_config = ConfigDict(from_attributes=True)

class PartidaJoinRequest(BaseModel):
    codigo_partida:str
    id_jugador: int

class PartidaJoinResponse(BaseModel):
    id_jugador: int
    nombre_partida: Optional[str] = None
    ws_code: str = Field(
        description="Token de conexión; ruta WebSocket: /ws/join/{codigo_partida}/{ws_code}",
    )


class PartidaLeaveRequest(BaseModel):
    codigo_partida: str
    id_jugador: int


class PartidaLeaveResponse(BaseModel):
    left: bool


class PartidaStartResponse(BaseModel):
    id_partida: int
    id_estado_partida: int
    impostor_actual: Optional[int] = None
    distribucion_misiones: dict[int, list[int]]


class PartidaEndRequest(BaseModel):
    id_partida: int
    ganador_tripulacion: Optional[bool] = None


class PartidaEndResponse(BaseModel):
    id_partida: int
    id_estado_partida: int
    ganador_tripulacion: Optional[bool] = None
    fecha_fin: Optional[datetime] = None
    crew_won: bool

#-----------------------------------------------
class JugadorPartidaBase(BaseModel):
    id_partida: int
    id_usuario: int
    apodo_partida: Optional[str] = None

class JugadorPartidaCreate(JugadorPartidaBase):
    pass

class JugadorPartidaResponse(JugadorPartidaBase):
    id_jugador_partida: int
    veces_fue_impostor: int
    jugador_vivo: bool
    puntos_partida: int
    misiones_completadas: int
    sabotajes_realizados: int
    eliminaciones_realizadas: int
    eliminado_por: Optional[int] = None
    fecha_entrada_sala_espiritus: Optional[datetime] = None
    tiempo_jugado_segundos: int
    model_config = ConfigDict(from_attributes=True)