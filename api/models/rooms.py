from typing import Optional
from decimal import Decimal
from sqlmodel import SQLModel, Field

class Habitaciones(SQLModel, table=True):
    __tablename__ = "habitaciones"

    id_habitacion: Optional[int] = Field(default=None, primary_key=True)

    nombre: str = Field(max_length=100)
    descripcion: Optional[str] = Field(default=None)

    id_tipo_habitacion: int = Field(foreign_key="tipo_habitacion.id_tipo_habitacion", default=9)

    tiene_camaras: bool = Field(default=True)
    tiene_microfonos: bool = Field(default=False)

    posicion_x: Optional[Decimal] = Field(default=None, max_digits=10, decimal_places=2)
    posicion_y: Optional[Decimal] = Field(default=None, max_digits=10, decimal_places=2)

    es_sala_espiritus: bool = Field(default=False)
    disponible_modo_lite: bool = Field(default=True)