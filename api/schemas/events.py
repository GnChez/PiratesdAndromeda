from pydantic import BaseModel, ConfigDict
from typing import Optional
from datetime import datetime

class EventosPartidaBase(BaseModel):
    id_partida: int
    id_tipo_evento: int
    id_usuario_origen: Optional[int] = None
    id_usuario_afectado: Optional[int] = None
    id_mision_relacionada: Optional[int] = None
    descripcion: Optional[str] = None

class EventosPartidaCreate(EventosPartidaBase):
    pass

class EventosPartidaResponse(EventosPartidaBase):
    id_evento: int
    timestamp_evento: datetime
    model_config = ConfigDict(from_attributes=True)


class VotosBase(BaseModel):
    id_evento: int  # Relacionado al evento de "Reunión de emergencia"
    id_jugador_votante: int
    id_jugador_votado: Optional[int] = None # Puede ser nulo si vota "Skip" (saltar)

class VotosCreate(VotosBase):
    pass

class VotosResponse(VotosBase):
    id_voto: int
    model_config = ConfigDict(from_attributes=True)