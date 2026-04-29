from pydantic import BaseModel, ConfigDict
from typing import Optional
from decimal import Decimal

class HabitacionBase(BaseModel):
    nombre: str

class HabitacionCreate(HabitacionBase):
    pass

class HabitacionResponse(HabitacionBase):
    id_habitacion: int
    descripcion: Optional[str] = "Perro"
    id_tipo_habitacion: int
    posicion_x: Optional[Decimal] = None
    posicion_y: Optional[Decimal] = None
    es_sala_espiritus: bool
    model_config = ConfigDict(from_attributes=True)
