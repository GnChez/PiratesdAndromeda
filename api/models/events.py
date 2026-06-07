from typing import Optional
from datetime import datetime
from sqlmodel import SQLModel, Field

class EventosPartida(SQLModel, table=True):
    __tablename__ = "eventos_partida"
    id_evento: Optional[int] = Field(default=None, primary_key=True)

    id_partida: int = Field(foreign_key="partidas.id_partida", ondelete="CASCADE")
    id_tipo_evento: int = Field(foreign_key="tipo_evento.id_tipo_evento")

    id_usuario_origen: Optional[int] = Field(default=None, foreign_key="usuarios.id_usuario")
    id_usuario_afectado: Optional[int] = Field(default=None, foreign_key="usuarios.id_usuario")
    id_mision_relacionada: Optional[int] = Field(default=None, foreign_key="misiones.id_mision")

    descripcion: Optional[str] = Field(default=None)
    timestamp_evento: datetime = Field(default_factory=datetime.utcnow)

class Votos(SQLModel, table=True):
    __tablename__ = "votos"
    id_voto: Optional[int] = Field(default=None, primary_key=True)

    id_evento: int = Field(foreign_key="eventos_partida.id_evento", ondelete="CASCADE")
    id_jugador_votante: int = Field(foreign_key="usuarios.id_usuario")
    id_jugador_votado: Optional[int] = Field(default=None, foreign_key="usuarios.id_usuario")