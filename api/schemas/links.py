from typing import Optional
from pydantic import BaseModel, ConfigDict
from datetime import datetime

class PartidaHabitacionBase(BaseModel):
    id_partida: int
    id_habitacion: int

class PartidaHabitacionCreate(PartidaHabitacionBase):
    pass

class PartidaHabitacionResponse(PartidaHabitacionBase):
    model_config = ConfigDict(from_attributes=True)


class MisionesPartidaBase(BaseModel):
    id_partida: int
    id_mision: int
    id_jugador_asignado: Optional[int] = None

class MisionesPartidaCreate(MisionesPartidaBase):
    pass

class MisionesPartidaResponse(MisionesPartidaBase):
    id_mision_partida: int
    veces_saboteada: int
    fecha_ultimo_sabotaje: Optional[datetime] = None
    model_config = ConfigDict(from_attributes=True)

class MisionesPartidaJugadorBase(BaseModel):
    id_mision_partida: int
    id_jugador: int
    id_estado_mision: int

class MisionesPartidaJugadorCreate(MisionesPartidaJugadorBase):
    pass

class MisionesPartidaJugadorUpdate(BaseModel):
    id_estado_mision: Optional[int] = None
    tiempo_empleado: Optional[int] = None
    fecha_inicio: Optional[datetime] = None
    fecha_completada: Optional[datetime] = None

class MisionesPartidaJugadorResponse(MisionesPartidaJugadorBase):
    id_misiones_partida_jugador: int
    tiempo_empleado: Optional[int] = None
    fecha_inicio: Optional[datetime] = None
    fecha_completada: Optional[datetime] = None
    model_config = ConfigDict(from_attributes=True)