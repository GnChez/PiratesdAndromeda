from typing import Optional
from datetime import datetime, timezone
from decimal import Decimal
from sqlmodel import SQLModel, Field
from models.users import Usuarios  # noqa: F401 - needed so 'usuarios' table is registered

class Partidas(SQLModel, table=True):
    __tablename__ = "partidas"

    id_partida: Optional[int] = Field(default=None, primary_key=True)

    codigo_partida: str = Field(unique=True, index=True, max_length=10)
    nombre_partida: Optional[str] = Field(default=None, max_length=100)

    id_creador: int = Field(foreign_key="usuarios.id_usuario")
    presencial: bool = Field(default=True)

    id_estado_partida: int = Field(foreign_key="estado_partida.id_estado_partida")

    fecha_inicio: Optional[datetime] = Field(default=None)
    fecha_fin: Optional[datetime] = Field(default=None)

    num_jugadores: int
    num_min_tripulantes_victoria: int = Field(default=2)

    porcentaje_reparacion_actual: Decimal = Field(default=Decimal("0.00"), max_digits=5, decimal_places=2)
    porcentaje_reparacion_victoria: Decimal = Field(default=Decimal("100.00"), max_digits=5, decimal_places=2)

    impostor_actual: Optional[int] = Field(default=None, foreign_key="usuarios.id_usuario")
    numero_impostores_descubiertos: int = Field(default=0)
    ganador_tripulacion: Optional[bool] = Field(default=None)
    numero_impostores: int = Field(default=1)

    tiempo_limite_minutos: int = Field(default=60)
    id_dificultad: int = Field(foreign_key="dificultad.id_dificultad")

class JugadoresPartida(SQLModel, table=True):
    __tablename__ = "jugadores_partida"

    id_jugador_partida: Optional[int] = Field(default=None, primary_key=True)

    id_partida: int = Field(foreign_key="partidas.id_partida", ondelete="CASCADE")
    id_usuario: int = Field(foreign_key="usuarios.id_usuario")

    apodo_partida: Optional[str] = Field(default=None, max_length=50)
    veces_fue_impostor: int = Field(default=0)
    jugador_vivo: bool = Field(default=True)

    puntos_partida: int = Field(default=0)
    misiones_completadas: int = Field(default=0)
    sabotajes_realizados: int = Field(default=0)
    eliminaciones_realizadas: int = Field(default=0)

    eliminado_por: Optional[int] = Field(default=None, foreign_key="usuarios.id_usuario")
    fecha_entrada_sala_espiritus: Optional[datetime] = Field(default=None)
    tiempo_jugado_segundos: int = Field(default=0)