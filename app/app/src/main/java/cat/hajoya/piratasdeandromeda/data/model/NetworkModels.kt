package cat.hajoya.piratasdeandromeda.data.model

import com.google.gson.annotations.SerializedName

/** Body de alta/login de usuario. */
data class UserCreate(
    @SerializedName("nombre_usuario") val nombreUsuario: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
)

/** Respuesta de usuario del backend. */
data class UserResponse(
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre_usuario") val nombreUsuario: String,
    @SerializedName("email") val email: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("id_rol_sistema") val idRolSistema: Int,
    @SerializedName("total_partidas_jugadas") val totalPartidasJugadas: Int,
    @SerializedName("total_puntos_acumulados") val totalPuntosAcumulados: Int,
    @SerializedName("veces_impostor") val vecesImpostor: Int,
    @SerializedName("veces_superviviente") val vecesSuperviviente: Int,
    @SerializedName("veces_eliminado") val vecesEliminado: Int,
    @SerializedName("fecha_ultima_conexion") val fechaUltimaConexion: String?,
)

/** Habitacion mínima para crear partida. */
data class HabitacionBase(
    @SerializedName("nombre") val nombre: String,
)

/** Payload de creación de partida. */
data class PartidaPedido(
    @SerializedName("id_creador") val idCreador: Int,
    @SerializedName("nombre_partida") val nombrePartida: String? = null,
    @SerializedName("presencial") val presencial: Boolean = false,
    @SerializedName("habitaciones") val habitaciones: List<HabitacionBase>,
)

/** Respuesta inicial al crear partida. */
data class PartidaInitialResponse(
    @SerializedName("id_partida") val idPartida: Int,
    @SerializedName("codigo_partida") val codigoPartida: String,
    @SerializedName("id_estado_partida") val idEstadoPartida: Int,
    @SerializedName("habitaciones_misiones") val habitacionesMisiones: Map<String, List<Int>>,
    @SerializedName("ws_code") val wsCode: String,
    @SerializedName("ws_room_code") val wsRoomCode: String,
)

/** Request para unirse a partida. */
data class PartidaJoinRequest(
    @SerializedName("codigo_partida") val codigoPartida: String,
    @SerializedName("id_jugador") val idJugador: Int,
)

/** Respuesta al unirse a partida. */
data class PartidaJoinResponse(
    @SerializedName("id_jugador") val idJugador: Int,
    @SerializedName("nombre_partida") val nombrePartida: String?,
    @SerializedName("ws_code") val wsCode: String,
)

/** Request para salir de partida. */
data class PartidaLeaveRequest(
    @SerializedName("codigo_partida") val codigoPartida: String,
    @SerializedName("id_jugador") val idJugador: Int,
)

/** Request para iniciar partida. */
data class PartidaStartRequest(
    @SerializedName("id_partida") val idPartida: Int,
    @SerializedName("id_creador") val idCreador: Int,
)

/** Respuesta de inicio de partida. */
data class PartidaStartResponse(
    @SerializedName("id_partida") val idPartida: Int,
    @SerializedName("id_estado_partida") val idEstadoPartida: Int,
    @SerializedName("impostor_actual") val impostorActual: Int?,
    @SerializedName("distribucion_misiones") val distribucionMisiones: Map<String, List<Int>>,
)

/** Request de finalización de partida. */
data class PartidaEndRequest(
    @SerializedName("id_partida") val idPartida: Int,
    @SerializedName("ganador_tripulacion") val ganadorTripulacion: Boolean? = null,
)

/** Respuesta de finalización de partida. */
data class PartidaEndResponse(
    @SerializedName("id_partida") val idPartida: Int,
    @SerializedName("id_estado_partida") val idEstadoPartida: Int,
    @SerializedName("ganador_tripulacion") val ganadorTripulacion: Boolean?,
    @SerializedName("fecha_fin") val fechaFin: String?,
    @SerializedName("crew_won") val crewWon: Boolean,
)

/** Mensaje genérico entrante por websocket. */
data class WsMessage(
    @SerializedName("type") val type: String?,
    @SerializedName("event") val event: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("player") val player: Any?,
    @SerializedName("room") val room: Any?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("expelled_player") val expelledPlayer: Any?,
    @SerializedName("votes") val votes: Any?,
    @SerializedName("id_partida") val idPartida: Int?,
    @SerializedName("impostor_actual") val impostorActual: Int?,
    @SerializedName("distribucion_misiones") val distribucionMisiones: Map<String, List<Int>>?,
    @SerializedName("ganador_tripulacion") val ganadorTripulacion: Boolean?,
    @SerializedName("fecha_fin") val fechaFin: String?,
    @SerializedName("mission_id") val missionId: Int?,
    @SerializedName("id_usuario_afectado") val idUsuarioAfectado: Int?,
    @SerializedName("scores") val scores: Map<String, Int>?,
    @SerializedName("pontos") val puntos: Int?,
)

/** Notificación temporal del WebSocket. */
data class WsNotification(
    val type: String,
    val message: String,
    val timestamp: Long,
    val durationMs: Long = 4000L,  // Duración por defecto de 4 segundos
)
