from pydantic import BaseModel, EmailStr, ConfigDict
from typing import Optional
from datetime import datetime


class UsuarioBase(BaseModel):
    nombre_usuario: str
    avatar_url: Optional[str] = None
    email:EmailStr


class UserCreate(UsuarioBase):
    password: str


class UserResponse(UsuarioBase):
    id_usuario: int
    id_rol_sistema: int
    total_partidas_jugadas: int
    total_puntos_acumulados: int
    veces_impostor: int
    veces_superviviente: int
    veces_eliminado: int

    fecha_ultima_conexion: Optional[datetime] = None
    model_config = ConfigDict(from_attributes=True)


class UserUpdate(BaseModel):
    nombre_usuario: Optional[str] = None
    email: Optional[EmailStr] = None
    avatar_url: Optional[str] = None
    password: Optional[str] = None

    total_partidas_jugadas: Optional[int] = None
    total_puntos_acumulados: Optional[int] = None
    veces_impostor: Optional[int] = None
    veces_superviviente: Optional[int] = None
    veces_eliminado: Optional[int] = None

    fecha_ultima_conexion: Optional[datetime] = None
