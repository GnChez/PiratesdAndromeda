from pydantic import BaseModel

class RolSistemaResponse(BaseModel):
    id_rol: int
    nombre_rol: str

class EstadoPartidaResponse(BaseModel):
    id_estado_partida: int
    nombre_estado: str

class DificultadResponse(BaseModel):
    id_dificultad: int
    nombre_dificultad: str

class TipoHabitacionResponse(BaseModel):
    id_tipo_habitacion:int
    nombre_tipo:str

class TipoMisionResponse(BaseModel):
    id_tipo_mision:int
    nombre_tipo:str

class TipoDispositivoResponse(BaseModel):
    id_tipo_dispositivo:int
    nombre_tipo:str

class EstadoConexionResponse(BaseModel):
    id_estado_conexion:int
    nombre_estado:str

class EstadoMisionJugadorResponse(BaseModel):
    id_estado_mision:int
    nombre_estado:str

class TipoEventoResponse(BaseModel):
    id_tipo_evento:int
    descripcion:str