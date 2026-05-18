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

enum class WsConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR,
}

data class ConnectedPlayer(
    val playerId: String,
    val nombreUsuario: String,
    val playerName: String,
    val connected: Boolean,
    val puntos: Int = 0,
)

class WebSocketManager(
    private val gson: Gson = Gson(),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build(),
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Jugador ──────────────────────────────────────────────────────────────

    private val _messages = MutableStateFlow<WsMessage?>(null)
    val messages: StateFlow<WsMessage?> = _messages.asStateFlow()

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _notifications = MutableSharedFlow<WsNotification>(replay = 0)
    val notifications: SharedFlow<WsNotification> = _notifications.asSharedFlow()

    private val _playerScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playerScores: StateFlow<Map<String, Int>> = _playerScores.asStateFlow()

    private val _missionsByRoom = MutableStateFlow<Map<String, Any>>(emptyMap())
    val missionsByRoom: StateFlow<Map<String, Any>> = _missionsByRoom.asStateFlow()

    // ── Monitor ──────────────────────────────────────────────────────────────

    private val _monitorConnectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val monitorConnectionState: StateFlow<WsConnectionState> = _monitorConnectionState.asStateFlow()

    private val _connectedPlayers = MutableStateFlow<List<ConnectedPlayer>>(emptyList())
    val connectedPlayers: StateFlow<List<ConnectedPlayer>> = _connectedPlayers.asStateFlow()

    // ── Internos ─────────────────────────────────────────────────────────────

    private var webSocket: WebSocket? = null
    private var monitorWebSocket: WebSocket? = null
    private var isManualDisconnect = false
    private var isManualMonitorDisconnect = false
    private var reconnectAttempts = 0
    private var monitorReconnectAttempts = 0
    private var lastGameCode: String? = null
    private var lastWsCode: String? = null

    // ── API pública ──────────────────────────────────────────────────────────

    fun connect(gameCode: String, wsCode: String) {
        disconnectCurrentSocket()
        isManualDisconnect = false
        reconnectAttempts = 0
        lastGameCode = gameCode
        lastWsCode = wsCode
        _connectionState.value = WsConnectionState.CONNECTING
        val url = "${NetworkConfig.baseWsUrl.trimEnd('/')}/ws/join/$gameCode/$wsCode"
        webSocket = okHttpClient.newWebSocket(Request.Builder().url(url).build(), playerListener)
    }

    fun connectMonitor(gameCode: String) {
        disconnectMonitorSocket()
        isManualMonitorDisconnect = false
        monitorReconnectAttempts = 0
        _monitorConnectionState.value = WsConnectionState.CONNECTING
        val url = "${NetworkConfig.baseWsUrl.trimEnd('/')}/ws/monitor/$gameCode"
        monitorWebSocket = okHttpClient.newWebSocket(Request.Builder().url(url).build(), monitorListener)
    }

    fun send(action: String, extraFields: Map<String, Any?> = emptyMap()) {
        val payload = LinkedHashMap<String, Any?>()
        payload["action"] = action
        payload.putAll(extraFields)
        webSocket?.send(gson.toJson(payload))
    }

    fun sendMissionStarted(missionId: Int) = send(MISION_INICIADA, mapOf("mission_id" to missionId))
    fun sendMissionCompleted(missionId: Int) = send(MISION_COMPLETADA, mapOf("mission_id" to missionId))
    fun sendEmergencyReunion() = send(REUNION_EMERGENCIA)
    fun sendVote(targetPlayerId: Int? = null) {
        val fields = if (targetPlayerId != null) mapOf("id_usuario_afectado" to targetPlayerId) else emptyMap()
        send(VOTO, fields)
    }

    fun disconnect() {
        isManualDisconnect = true
        disconnectCurrentSocket()
        _connectionState.value = WsConnectionState.DISCONNECTED
    }

    fun disconnectMonitor() {
        isManualMonitorDisconnect = true
        disconnectMonitorSocket()
        _monitorConnectionState.value = WsConnectionState.DISCONNECTED
    }

    // ── Internos ─────────────────────────────────────────────────────────────

    private fun disconnectCurrentSocket() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    private fun disconnectMonitorSocket() {
        monitorWebSocket?.close(1000, "Monitor disconnect")
        monitorWebSocket = null
    }

    private fun scheduleReconnect() {
        if (isManualDisconnect) return
        val gameCode = lastGameCode ?: return
        val wsCode = lastWsCode ?: return
        if (reconnectAttempts >= MAX_RETRIES) { _connectionState.value = WsConnectionState.ERROR; return }
        reconnectAttempts++
        _connectionState.value = WsConnectionState.CONNECTING
        scope.launch {
            delay(RETRY_DELAY_MS)
            val url = "${NetworkConfig.baseWsUrl.trimEnd('/')}/ws/join/$gameCode/$wsCode"
            webSocket = okHttpClient.newWebSocket(Request.Builder().url(url).build(), playerListener)
        }
    }

    private fun scheduleMonitorReconnect() {
        if (isManualMonitorDisconnect) return
        val gameCode = lastGameCode ?: return
        if (monitorReconnectAttempts >= MAX_RETRIES) { _monitorConnectionState.value = WsConnectionState.ERROR; return }
        monitorReconnectAttempts++
        _monitorConnectionState.value = WsConnectionState.CONNECTING
        scope.launch {
            delay(RETRY_DELAY_MS)
            val url = "${NetworkConfig.baseWsUrl.trimEnd('/')}/ws/monitor/$gameCode"
            monitorWebSocket = okHttpClient.newWebSocket(Request.Builder().url(url).build(), monitorListener)
        }
    }

    private fun emitNotification(type: String, message: String, durationMs: Long = 3000L) {
        scope.launch {
            _notifications.emit(WsNotification(type = type, message = message, timestamp = System.currentTimeMillis(), durationMs = durationMs))
        }
    }

    /** Aplica un mapa de scores a _playerScores y a los puntos de connectedPlayers. */
    private fun applyScores(scores: Map<String, Int>) {
        _playerScores.value = scores
        _connectedPlayers.value = _connectedPlayers.value.map { player ->
            val pts = scores[player.playerId]
            if (pts != null) player.copy(puntos = pts) else player
        }
    }

    // ── Listener jugador ─────────────────────────────────────────────────────

    private val playerListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempts = 0
            _connectionState.value = WsConnectionState.CONNECTED
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val message = gson.fromJson(text, WsMessage::class.java)
                _messages.value = message

                when {
                    message.type == "GAME_STARTED" -> {
                        message.getDistribucionMisiones()?.let { _missionsByRoom.value = it }
                        emitNotification("GAME_STARTED", "¡La partida ha comenzado! 🎮")
                    }
                    message.type == "GAME_ENDED" -> {
                        val result = if (message.ganadorTripulacion == true)
                            "¡La tripulación ha ganado! 🎉" else "¡Los impostores han ganado! 👻"
                        emitNotification("GAME_ENDED", result)
                    }
                    message.type == "ERROR" -> {
                        emitNotification("ERROR", message.message ?: "Error desconocido", durationMs = 5000L)
                    }
                    // Solo notificar eventos importantes, no misiones individuales
                    message.type == "PLAYER_DIED" || message.type == "REUNION" -> {
                        emitNotification(message.type!!, message.message ?: "Actualización del juego")
                    }
                }

                message.getScores()?.let { applyScores(it) }

                if (message.puntos != null) {
                    val updated = _playerScores.value.toMutableMap()
                    message.player?.toString()?.let { id -> updated[id] = message.puntos }
                    _playerScores.value = updated
                }
            } catch (_: Exception) { }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
            _connectionState.value = WsConnectionState.DISCONNECTED
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = WsConnectionState.DISCONNECTED
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connectionState.value = WsConnectionState.ERROR
            scheduleReconnect()
        }
    }

    // ── Listener monitor ─────────────────────────────────────────────────────

    private val monitorListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            monitorReconnectAttempts = 0
            _monitorConnectionState.value = WsConnectionState.CONNECTED
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val message = gson.fromJson(text, WsMessage::class.java)

                if (message.type == "MONITOR_CONNECTED") {
                    val rawList: List<*> = message.connectedPlayers as? List<*> ?: emptyList<Any>()
                    _connectedPlayers.value = rawList.mapNotNull { item ->
                        (item as? Map<*, *>)?.let { m ->
                            ConnectedPlayer(
                                playerId = m["player"]?.toString() ?: "",
                                nombreUsuario = m["nombre_usuario"]?.toString() ?: "",
                                playerName = m["player_name"]?.toString() ?: "",
                                connected = (m["connected"] as? Boolean) ?: false,
                                puntos = 0,
                            )
                        }
                    }
                    // Scores del snapshot inicial
                    message.getScores()?.let { applyScores(it) }
                }

                // Actualizar scores en cualquier mensaje del monitor que los incluya
                message.getScores()?.let { applyScores(it) }

                if (message.event == "PLAYER_JOINED") {
                    val incomingId = message.player?.toString() ?: return
                    val current = _connectedPlayers.value.toMutableList()
                    val idx = current.indexOfFirst { it.playerId == incomingId }
                    if (idx >= 0) {
                        current[idx] = current[idx].copy(connected = true)
                    } else {
                        current.add(ConnectedPlayer(
                            playerId = incomingId,
                            nombreUsuario = message.nombreUsuario ?: incomingId,
                            playerName = message.playerName ?: message.nombreUsuario ?: incomingId,
                            connected = true,
                            puntos = message.puntos?:0,
                        ))
                    }
                    _connectedPlayers.value = current
                }

                if (message.event == "PLAYER_LEFT") {
                    val leavingId = message.player?.toString() ?: return
                    _connectedPlayers.value = _connectedPlayers.value.filterNot { it.playerId == leavingId }
                }
            } catch (_: Exception) { }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
            _monitorConnectionState.value = WsConnectionState.DISCONNECTED
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _monitorConnectionState.value = WsConnectionState.DISCONNECTED
            scheduleMonitorReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _monitorConnectionState.value = WsConnectionState.ERROR
            scheduleMonitorReconnect()
        }
    }

    companion object {
        const val REUNION_EMERGENCIA = "reunion_emergencia"
        const val JUGADOR_ELIMINADO  = "jugador_eliminado"
        const val MISION_SABOTEADA   = "mision_saboteada"
        const val MISION_DESABOTEADA = "mision_desaboteada"
        const val MISION_INICIADA    = "mision_iniciada"
        const val MISION_COMPLETADA  = "mision_completada"
        const val VOTO               = "voto"
        const val INICIO_PARTIDA     = "inicio_partida"
        const val FIN_PARTIDA        = "fin_partida"
        const val SALIR              = "salir"

        private const val MAX_RETRIES    = 3
        private const val RETRY_DELAY_MS = 3_000L
    }
}