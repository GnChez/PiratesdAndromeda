from typing import Optional
from datetime import datetime
from sqlmodel import SQLModel, Field

class Usuarios(SQLModel, table=True):
    __tablename__ = "usuarios"

    id_usuario: Optional[int] = Field(default=None, primary_key=True)

    nombre_usuario: str = Field(index=True, unique=True, max_length=50)
    email: str = Field(index=True, unique=True, max_length=100)
    password: str = Field(max_length=255)

    avatar_url: Optional[str] = Field(default=None, max_length=255)

    fecha_registro: datetime = Field(default_factory=datetime.utcnow)
    fecha_ultima_conexion: Optional[datetime] = Field(default=None)

    total_partidas_jugadas: int = Field(default=0)
    total_puntos_acumulados: int = Field(default=0)
    veces_impostor: int = Field(default=0)
    veces_superviviente: int = Field(default=0)
    veces_eliminado: int = Field(default=0)

    id_rol_sistema: int = Field(default=1, foreign_key="rol_sistema.id_rol")