from typing import Optional
from sqlmodel import SQLModel, Field

class RolSistema(SQLModel, table=True):
    __tablename__ = "rol_sistema"
    id_rol: Optional[int] = Field(default=None, primary_key=True)
    nombre_rol: str = Field(unique=True, max_length=100)

class EstadoPartida(SQLModel, table=True):
    __tablename__ = "estado_partida"
    id_estado_partida: Optional[int] = Field(default=None, primary_key=True)
    nombre_estado: str = Field(unique=True, max_length=50)

class Dificultad(SQLModel, table=True):
    __tablename__ = "dificultad"
    id_dificultad: Optional[int] = Field(default=None, primary_key=True)
    nombre_dificultad: str = Field(unique=True, max_length=50)

class TipoHabitacion(SQLModel, table=True):
    __tablename__ = "tipo_habitacion"
    id_tipo_habitacion: Optional[int] = Field(default=None, primary_key=True)
    nombre_tipo: str = Field(unique=True, max_length=50)

class TipoMision(SQLModel, table=True):
    __tablename__ = "tipo_mision"
    id_tipo_mision: Optional[int] = Field(default=None, primary_key=True)
    nombre_tipo: str = Field(unique=True, max_length=50)

class TipoDispositivo(SQLModel, table=True):
    __tablename__ = "tipo_dispositivo"
    id_tipo_dispositivo: Optional[int] = Field(default=None, primary_key=True)
    nombre_tipo: str = Field(unique=True, max_length=50)

class EstadoConexion(SQLModel, table=True):
    __tablename__ = "estado_conexion"
    id_estado_conexion: Optional[int] = Field(default=None, primary_key=True)
    nombre_estado: str = Field(unique=True, max_length=50)

class EstadoMisionJugador(SQLModel, table=True):
    __tablename__ = "estado_mision_jugador"
    id_estado_mision: Optional[int] = Field(default=None, primary_key=True)
    nombre_estado: str = Field(unique=True, max_length=50)

class TipoEvento(SQLModel, table=True):
    __tablename__ = "tipo_evento"
    id_tipo_evento: Optional[int] = Field(default=None, primary_key=True)
    descripcion: str = Field(unique=True, max_length=100)