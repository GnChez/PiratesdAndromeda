from typing import Optional
from datetime import datetime
from sqlmodel import SQLModel, Field

class PartidaHabitacion(SQLModel, table=True):
    __tablename__ = "partida_habitacion"
    id_partida: int = Field(primary_key=True, foreign_key="partidas.id_partida")
    id_habitacion: int = Field(primary_key=True, foreign_key="habitaciones.id_habitacion")


class MisionesPartida(SQLModel, table=True):
    __tablename__ = "misiones_partida"
    id_mision_partida: Optional[int] = Field(default=None, primary_key=True)

    id_partida: int = Field(foreign_key="partidas.id_partida", ondelete="CASCADE")
    id_mision: int = Field(foreign_key="misiones.id_mision")
    id_jugador_asignado: Optional[int] = Field(default=None, foreign_key="usuarios.id_usuario")

    veces_saboteada: int = Field(default=0)
    fecha_ultimo_sabotaje: Optional[datetime] = Field(default=None)


class MisionesPartidaJugador(SQLModel, table=True):
    __tablename__ = "misiones_partida_jugador"
    id_misiones_partida_jugador: Optional[int] = Field(default=None, primary_key=True)

    id_mision_partida: int = Field(foreign_key="misiones_partida.id_mision_partida")
    id_jugador: int = Field(foreign_key="usuarios.id_usuario")

    tiempo_empleado: Optional[int] = Field(default=None)
    id_estado_mision: int = Field(foreign_key="estado_mision_jugador.id_estado_mision")

    fecha_inicio: Optional[datetime] = Field(default=None)
    fecha_completada: Optional[datetime] = Field(default=None)
