from typing import Optional, Dict, Any
from decimal import Decimal
from sqlmodel import SQLModel, Field, Column, JSON


class Misiones(SQLModel, table=True):
    __tablename__ = "misiones"

    id_mision: Optional[int] = Field(default=None, primary_key=True)

    codigo_mision: str = Field(unique=True, index=True, max_length=20)
    nombre: str = Field(max_length=100)
    descripcion: Optional[str] = Field(default=None)

    id_habitacion: int = Field(foreign_key="habitaciones.id_habitacion")
    id_tipo_mision: int = Field(foreign_key="tipo_mision.id_tipo_mision")
    id_dificultad: int = Field(foreign_key="dificultad.id_dificultad")

    puntos_otorgados: int = Field(default=100)
    tiempo_estimado_segundos: int = Field(default=60)
    porcentaje_reparacion: Decimal = Field(default=Decimal("5.00"), max_digits=5, decimal_places=2)

    requiere_dispositivo_fisico: bool = Field(default=False)

    # Manejo de JSON en SQLModel usando SQLAlchemy
    pasos_completar: Optional[Dict[str, Any]] = Field(default=None, sa_column=Column(JSON))

    puede_ser_saboteada: bool = Field(default=True)
    puntos_sabotaje: int = Field(default=50)

    requiere_multiples_jugadores: bool = Field(default=False)
    num_jugadores_requeridos: int = Field(default=1)

    disponible_modo_lite: bool = Field(default=True)
    esta_activa: bool = Field(default=True)