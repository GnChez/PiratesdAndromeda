from typing import Optional
from datetime import datetime
from sqlmodel import SQLModel, Field

class DispositivosFisicos(SQLModel, table=True):
    __tablename__ = "dispositivos_fisicos"

    id_dispositivo: Optional[int] = Field(default=None, primary_key=True)

    codigo_dispositivo: str = Field(unique=True, index=True, max_length=50)
    nombre: str = Field(max_length=100)

    id_tipo_dispositivo: int = Field(foreign_key="tipo_dispositivo.id_tipo_dispositivo")
    id_habitacion: Optional[int] = Field(default=None, foreign_key="habitaciones.id_habitacion")

    direccion_ip: Optional[str] = Field(default=None, max_length=45)
    puerto: Optional[int] = Field(default=None)

    id_estado_conexion: int = Field(foreign_key="estado_conexion.id_estado_conexion")
    ultima_comunicacion: Optional[datetime] = Field(default=None)
    notas_tecnicas: Optional[str] = Field(default=None)