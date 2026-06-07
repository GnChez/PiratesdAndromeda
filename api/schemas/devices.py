from pydantic import BaseModel, ConfigDict
from typing import Optional
from datetime import datetime

class DispositivoFisicoBase(BaseModel):
    codigo_dispositivo: str
    nombre: str
    id_tipo_dispositivo: int
    id_habitacion: Optional[int] = None
    direccion_ip: Optional[str] = None
    puerto: Optional[int] = None
    id_estado_conexion: int
    notas_tecnicas: Optional[str] = None


class DispositivoFisicoCreate(DispositivoFisicoBase):
    pass


class DispositivoFisicoUpdate(BaseModel):
    nombre: Optional[str] = None
    id_habitacion: Optional[int] = None
    direccion_ip: Optional[str] = None
    puerto: Optional[int] = None
    id_estado_conexion: Optional[int] = None
    notas_tecnicas: Optional[str] = None


class DispositivoFisicoResponse(DispositivoFisicoBase):
    id_dispositivo: int
    ultima_comunicacion: Optional[datetime] = None

    # Esto permite que Pydantic lea directamente del objeto SQLModel
    model_config = ConfigDict(from_attributes=True)