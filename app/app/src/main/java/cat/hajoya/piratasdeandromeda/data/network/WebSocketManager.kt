package cat.hajoya.piratasdeandromeda.data.network

import cat.hajoya.piratasdeandromeda.data.model.WsMessage
import cat.hajoya.piratasdeandromeda.data.model.WsNotification
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/** Estado de conexión de websocket. */
enum class WsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

/**
 * Gestor de websocket con reconexión automática y flujo de eventos.
 */
class WebSocketManager(
    private val gson: Gson = Gson(),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build(),
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _messages = MutableStateFlow<WsMessage?>(null)
    val messages: StateFlow<WsMessage?> = _messages.asStateFlow()

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _notifications = MutableSharedFlow<WsNotification>(replay = 0)
    val notifications: SharedFlow<WsNotification> = _notifications.asSharedFlow()

    private val _playerScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playerScores: StateFlow<Map<String, Int>> = _playerScores.asStateFlow()

    private val _missionsByRoom = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    val missionsByRoom: StateFlow<Map<String, List<Int>>> = _missionsByRoom.asStateFlow()

    private var webSocket: WebSocket? = null
    private var isManualDisconnect = false
    private var reconnectAttempts = 0
    private var lastGameCode: String? = null
    private var lastWsCode: String? = null

    /** Abre conexión contra /ws/join/{gameCode}/{wsCode}. */
    fun connect(gameCode: String, wsCode: String) {
        disconnectCurrentSocket()
        isManualDisconnect = false
        reconnectAttempts = 0
        lastGameCode = gameCode
        lastWsCode = wsCode
        _connectionState.value = WsConnectionState.CONNECTING

        val url = "${NetworkConfig.baseWsUrl.trimEnd('/')}/ws/join/$gameCode/$wsCode"
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    /** Envía un payload de acción al servidor. */
    fun send(action: String, extraFields: Map<String, Any?> = emptyMap()) {
        val payload = LinkedHashMap<String, Any?>()
        payload["action"] = action
        payload.putAll(extraFields)
        webSocket?.send(gson.toJson(payload))
    }

    /** Envía evento de inicio de misión. */
    fun sendMissionStarted(missionId: Int) {
        send(MISION_INICIADA, mapOf("mission_id" to missionId))
    }

    /** Envía evento de misión completada. */
    fun sendMissionCompleted(missionId: Int) {
        send(MISION_COMPLETADA, mapOf("mission_id" to missionId))
    }

    /** Envía evento de reunión de emergencia. */
    fun sendEmergencyReunion() {
        send(REUNION_EMERGENCIA)
    }

    /** Envía voto. */
    fun sendVote(targetPlayerId: Int? = null) {
        val fields = if (targetPlayerId != null) {
            mapOf("id_usuario_afectado" to targetPlayerId)
        } else {
            emptyMap()
        }
        send(VOTO, fields)
    }

    /** Cierra la conexión actual de forma explícita. */
    fun disconnect() {
        isManualDisconnect = true
        disconnectCurrentSocket()
        _connectionState.value = WsConnectionState.DISCONNECTED
    }

    private fun disconnectCurrentSocket() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    private fun scheduleReconnectIfNeeded() {
        if (isManualDisconnect) return
        val gameCode = lastGameCode ?: return
        val wsCode = lastWsCode ?: return
        if (reconnectAttempts >= MAX_RETRIES) {
            _connectionState.value = WsConnectionState.ERROR
            return
        }

        reconnectAttempts += 1
        _connectionState.value = WsConnectionState.CONNECTING
        scope.launch {
            delay(RETRY_DELAY_MS)
            val url = "${NetworkConfig.baseWsUrl.trimEnd('/')}/ws/join/$gameCode/$wsCode"
            val request = Request.Builder().url(url).build()
            webSocket = okHttpClient.newWebSocket(request, listener)
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempts = 0
            _connectionState.value = WsConnectionState.CONNECTED
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val message = gson.fromJson(text, WsMessage::class.java)
                _messages.value = message

                // Procesar tipos específicos de mensaje
                when {
                    message.type == "GAME_STARTED" -> {
                        message.distribucionMisiones?.let {
                            _missionsByRoom.value = it
                        }
                    }
                    message.type == "START_MISSION" || 
                    message.type == "COMPLETE_MISSION" ||
                    message.type == "PLAYER_DIED" ||
                    message.type == "REUNION_EMERGENCIA" -> {
                        // Emitir notificación temporal
                        scope.launch {
                            _notifications.emit(
                                WsNotification(
                                    type = message.type ?: "EVENT",
                                    message = message.message ?: "Actualización del juego",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }

                // Actualizar puntuaciones si vienen en el mensaje
                message.scores?.let { scores ->
                    _playerScores.value = scores
                }
                
                // También capturar puntos individuales si se envían de otra manera
                if (message.puntos != null) {
                    val currentScores = _playerScores.value.toMutableMap()
                    message.player?.toString()?.let { playerId ->
                        currentScores[playerId] = message.puntos
                        _playerScores.value = currentScores
                    }
                }
            } catch (_: Exception) {
                // Si llega un payload no parseable, no rompemos la sesión.
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
            _connectionState.value = WsConnectionState.DISCONNECTED
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = WsConnectionState.DISCONNECTED
            scheduleReconnectIfNeeded()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connectionState.value = WsConnectionState.ERROR
            scheduleReconnectIfNeeded()
        }
    }

    companion object {
        const val REUNION_EMERGENCIA = "REUNION_EMERGENCIA"
        const val JUGADOR_ELIMINADO = "JUGADOR_ELIMINADO"
        const val MISION_SABOTEADA = "MISION_SABOTEADA"
        const val MISION_DESABOTEADA = "MISION_DESABOTEADA"
        const val MISION_INICIADA = "MISION_INICIADA"
        const val MISION_COMPLETADA = "MISION_COMPLETADA"
        const val VOTO = "VOTO"
        const val INICIO_PARTIDA = "INICIO_PARTIDA"
        const val FIN_PARTIDA = "FIN_PARTIDA"
        const val SALIR = "SALIR"

        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 3_000L
    }
}



