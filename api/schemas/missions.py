from decimal import Decimal
from typing import  Optional, Dict, Any
from pydantic import BaseModel, ConfigDict


class MisionBase(BaseModel):
    codigo_mision: str
    nombre: str
    descripcion: Optional[str] = None
    id_habitacion: int
    id_tipo_mision: int
    id_dificultad: int
    puntos_otorgados: int = 100
    tiempo_estimado_segundos: int = 60
    porcentaje_reparacion: Decimal = Decimal("5.00")
    requiere_dispositivo_fisico: bool = False
    pasos_completar: Optional[Dict[str, Any]] = None
    puede_ser_saboteada: bool = True
    puntos_sabotaje: int = 50
    requiere_multiples_jugadores: bool = False
    num_jugadores_requeridos: int = 1
    disponible_modo_lite: bool = True
    esta_activa: bool = True

class MisionCreate(MisionBase):
    pass

class MisionResponse(MisionBase):
    id_mision: int
    model_config = ConfigDict(from_attributes=True)