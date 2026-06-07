package cat.hajoya.piratasdeandromeda.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import cat.hajoya.piratasdeandromeda.RoomItem
import cat.hajoya.piratasdeandromeda.SavedShip
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
import cat.hajoya.piratasdeandromeda.data.model.HabitacionBase
import cat.hajoya.piratasdeandromeda.data.model.PartidaJoinRequest
import cat.hajoya.piratasdeandromeda.data.model.PartidaPedido
import cat.hajoya.piratasdeandromeda.data.model.WsNotification
import cat.hajoya.piratasdeandromeda.data.network.WebSocketManager
import cat.hajoya.piratasdeandromeda.data.repository.GameRepository
import cat.hajoya.piratasdeandromeda.data.repository.ShipRepository
import cat.hajoya.piratasdeandromeda.models.ConfigPartida
import cat.hajoya.piratasdeandromeda.models.Dificultad
import cat.hajoya.piratasdeandromeda.models.EstatPartida
import cat.hajoya.piratasdeandromeda.models.Habitacio
import cat.hajoya.piratasdeandromeda.models.JugadorPartida
import cat.hajoya.piratasdeandromeda.models.Missio
import cat.hajoya.piratasdeandromeda.models.Partida
import cat.hajoya.piratasdeandromeda.models.Personaje
import cat.hajoya.piratasdeandromeda.models.RolJoc
import cat.hajoya.piratasdeandromeda.models.TipusHabitacio
import cat.hajoya.piratasdeandromeda.models.UserTaskUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("unused")
class GameViewModel(
    private val shipRepository: ShipRepository,
    private val gameRepository: GameRepository,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager? = null,
) : ViewModel() {

    companion object {
        private const val TAG = "GameViewModel"
    }

    // Inicializar el nombre de usuario desde la sesión
    init {
        viewModelScope.launch {
            sessionManager.nombreUsuario.collect { username ->
                _usuarioActual.postValue(username)
            }
        }
    }

    private val _selectedShipId = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    private val _myMissions = MutableStateFlow<List<UserTaskUi>>(emptyList())
    private val _myUserId = MutableStateFlow<Int?>(null)

    private val _usuarioActual = MutableLiveData<String?>()
    val usuarioActual: LiveData<String?> = _usuarioActual

    val selectedShipId: StateFlow<Long?> = _selectedShipId
    val myMissions: StateFlow<List<UserTaskUi>> = _myMissions.asStateFlow()

    /** ID del usuario que crea la partida */
    private val _idCreador = MutableStateFlow<Int?>(null)
    val idCreador: StateFlow<Int?> = _idCreador

    /** Conectados desde el monitor WebSocket */
    private val _connectedPlayers = MutableStateFlow<List<cat.hajoya.piratasdeandromeda.data.network.ConnectedPlayer>>(emptyList())
    val connectedPlayers: StateFlow<List<cat.hajoya.piratasdeandromeda.data.network.ConnectedPlayer>> = _connectedPlayers

    /** Evento que se emite cuando se crea una nave exitosamente */
    private val _shipCreatedEvent = MutableSharedFlow<Long>(replay = 0)
    val shipCreatedEvent = _shipCreatedEvent.asSharedFlow()

    /** Evento que se emite cuando se crea una habitación exitosamente */
    private val _roomCreatedEvent = MutableSharedFlow<Long>(replay = 0)
    val roomCreatedEvent = _roomCreatedEvent.asSharedFlow()

    val savedShips: StateFlow<List<SavedShip>> = shipRepository.allShips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allRooms: StateFlow<List<RoomItem>> = shipRepository.allRooms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rooms: StateFlow<List<RoomItem>> = _selectedShipId.flatMapLatest { shipId ->
        if (shipId != null) {
            shipRepository.getRoomsForShip(shipId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _partidaActual = MutableLiveData<Partida?>()
    val partidaActual: LiveData<Partida?> = _partidaActual

    private val _jugadorActual = MutableLiveData<JugadorPartida?>()
    val jugadorActual: LiveData<JugadorPartida?> = _jugadorActual

    private val _estatPartida = MutableLiveData<EstatPartida>(EstatPartida.ESPERANT_JUGADORS)
    val estatPartida: LiveData<EstatPartida> = _estatPartida

    private val _tempsRestant = MutableLiveData<Int>()
    val tempsRestant: LiveData<Int> = _tempsRestant

    private val _percentatgeReparacio = MutableLiveData<Float>()
    val percentatgeReparacio: LiveData<Float> = _percentatgeReparacio

    private val _jugadorsVius = MutableLiveData<List<JugadorPartida>>()
    val jugadorsVius: LiveData<List<JugadorPartida>> = _jugadorsVius

    private val _missionsAssignades = MutableLiveData<List<Missio>>()
    val missionsAssignades: LiveData<List<Missio>> = _missionsAssignades

    private val _cooldownSabotatge = MutableLiveData<Int>()
    val cooldownSabotatge: LiveData<Int> = _cooldownSabotatge

    private val _habitacionsPartida = MutableLiveData<List<Habitacio>>()
    val habitacionsPartida: LiveData<List<Habitacio>> = _habitacionsPartida

    private val _wsGameCode = MutableStateFlow<String?>(null)
    val wsGameCode: StateFlow<String?> = _wsGameCode

    private val _wsCode = MutableStateFlow<String?>(null)
    val wsCode: StateFlow<String?> = _wsCode

    private val _missionsByRoom = MutableStateFlow<Map<String, Any>>(emptyMap())
    val missionsByRoom: StateFlow<Map<String, Any>> = _missionsByRoom

    private val _playerScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playerScores: StateFlow<Map<String, Int>> = _playerScores

    private val _wsNotifications = MutableSharedFlow<String>(replay = 0)
    val wsNotifications = _wsNotifications.asSharedFlow()

    private val _currentMissionStartTime = MutableStateFlow<Long?>(null)
    val currentMissionStartTime: StateFlow<Long?> = _currentMissionStartTime
    private val _sabotageEvent = MutableSharedFlow<Int>(replay = 0)
    val sabotageEvent = _sabotageEvent.asSharedFlow()
    fun selectShip(shipId: Long?) {
        _selectedShipId.value = shipId
    }

    fun iniciarPartida() {
        _estatPartida.value = EstatPartida.EN_CURS
        _tempsRestant.value = 600
    }

    fun completarMissio(missioId: Int) {
        // Mock
    }

    fun sabotearHabitacio(habitacioId: Int) {
        viewModelScope.launch {
            // Emitir el ID de la sala saboteada para que los fragments suscritos
            // muestren SabotageAlertDialogFragment
            _sabotageEvent.emit(habitacioId)

            // Emitir via WebSocket la notificación de sabotaje al servidor/otros jugadores
            val notification = "sabotage:${habitacioId}"
            _wsNotifications.emit(notification)

            // TODO: llamar al endpoint REST de sabotaje cuando esté disponible
            // gameRepository.sabotearHabitacion(habitacioId)
        }
    }

    fun leaveGame() {
        // Desconectar WebSockets
        webSocketManager?.disconnect()

        // Resetear estado de la partida
        _partidaActual.postValue(null)
        _jugadorActual.postValue(null)
        _estatPartida.postValue(EstatPartida.ESPERANT_JUGADORS)
        _connectedPlayers.value = emptyList()
        _myMissions.value = emptyList()
        _missionsAssignades.postValue(emptyList())
        _jugadorsVius.postValue(emptyList())
        _habitacionsPartida.postValue(emptyList())

        // Limpiar códigos y sesión de partida
        _wsGameCode.value = null
        _wsCode.value = null
        _idCreador.value = null
        _myUserId.value = null
        _missionsByRoom.value = emptyMap()
        _playerScores.value = emptyMap()
        _currentMissionStartTime.value = null
    }

    fun convocarReunio() {
        // Mock
    }

    fun votar(jugadorId: Int) {
        // Mock
    }

    fun setHabitacions(habitaciones: List<Habitacio>) {
        _habitacionsPartida.value = habitaciones
    }

    suspend fun createGameFromSelectedShip(): Result<String> {
        val shipId = _selectedShipId.value
            ?: return Result.failure(IllegalStateException("Selecciona una nave antes de continuar"))

        val userId = sessionManager.userId.first()
            ?: return Result.failure(IllegalStateException("No hay sesión activa. Inicia sesión de nuevo"))

        val selectedRooms = shipRepository.getRoomsForShip(shipId).first()
        if (selectedRooms.isEmpty()) {
            return Result.failure(IllegalStateException("Añade al menos una habitación antes de crear la partida"))
        }

        val shipName = savedShips.value.firstOrNull { it.id == shipId }?.name
        val request = PartidaPedido(
            idCreador = userId,
            nombrePartida = shipName,
            presencial = false,
            habitaciones = selectedRooms.map { HabitacionBase(nombre = it.name) },
        )

        return gameRepository.createGame(request).mapCatching { response ->
            val partida = Partida(
                id = response.idPartida,
                codiPartida = response.codigoPartida,
                nomPartida = shipName,
                presencial = false,
                estatPartida = EstatPartida.ESPERANT_JUGADORS,
                numJugadors = 1,
                numImpostors = 1,
                tempsLimitMinuts = 10,
                percentatgeReparacio = 0f,
                dificultad = Dificultad.MITJA,
            )
            _partidaActual.postValue(partida)
            
            // Guardar códigos y ID del creador
            _wsGameCode.value = response.codigoPartida
            _wsCode.value = response.wsCode
            _missionsByRoom.value = response.habitacionesMisiones
            _idCreador.value = userId
            _myUserId.value = userId
            
            // Convertir habitaciones_misiones a Habitacio
            val habitaciones = response.habitacionesMisiones.keys.mapIndexed { index, nombre ->
                Habitacio(id = index + 1, nom = nombre, tipus = TipusHabitacio.OTRA)
            }
            _habitacionsPartida.postValue(habitaciones)
            
            // Conectar WebSocket de jugador
            connectWebSocket(response.codigoPartida, response.wsCode)
            setupWebSocketListeners()
            
            // Conectar Monitor WebSocket para obtener lista de jugadores
            connectMonitor(response.codigoPartida)
            
            response.codigoPartida
        }
    }

    fun addSavedShip(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val newId = shipRepository.addShip(trimmed)
            // Emitir evento para notificar que la nave se creó exitosamente
            _shipCreatedEvent.emit(newId)
        }
    }

    fun deleteSavedShip(shipId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            shipRepository.deleteShip(shipId)
            if (_selectedShipId.value == shipId) {
                _selectedShipId.value = null
            }
        }
    }

    fun addRoom(name: String) {
        val shipId = _selectedShipId.value ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val roomId = shipRepository.addRoom(shipId, trimmed)
            // Emitir evento para notificar que la habitación se creó exitosamente
            _roomCreatedEvent.emit(roomId)
        }
    }

    fun deleteRoom(roomId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            shipRepository.deleteRoom(roomId)
        }
    }

    suspend fun joinGame(codigoPartida: String): Result<String> {
        val userId = sessionManager.userId.first()
            ?: return Result.failure(IllegalStateException("No hay sesión activa. Inicia sesión de nuevo"))

        val request = PartidaJoinRequest(
            codigoPartida = codigoPartida,
            idJugador = userId
        )

        return gameRepository.joinGame(request).mapCatching { response ->
            val partida = Partida(
                id = 1,
                codiPartida = codigoPartida,
                nomPartida = response.nombrePartida ?: "Partida",
                presencial = false,
                estatPartida = EstatPartida.ESPERANT_JUGADORS,
                numJugadors = 1,
                numImpostors = 1,
                tempsLimitMinuts = 10,
                percentatgeReparacio = 0f,
                dificultad = Dificultad.MITJA,
            )
            _partidaActual.postValue(partida)
            
            // Guardar códigos
            _wsGameCode.value = codigoPartida
            _wsCode.value = response.wsCode
            _myUserId.value = userId

            // Conectar WebSocket de jugador
            connectWebSocket(codigoPartida, response.wsCode)
            setupWebSocketListeners()
            
            // Conectar Monitor WebSocket para obtener lista de jugadores
            connectMonitor(codigoPartida)
            
            response.wsCode
        }
    }
    
    /**
     * Conecta el WebSocket y registra el resultado
     */
    private fun connectWebSocket(gameCode: String, wsCode: String) {
        try {
            if (webSocketManager == null) {
                Log.e(TAG, "WebSocketManager es null, no se puede conectar")
                return
            }
            
            Log.i(TAG, "Intentando conectar WebSocket - Código: $gameCode, WsCode: $wsCode")
            webSocketManager.connect(gameCode, wsCode)
            Log.i(TAG, "✅ WebSocket conectado exitosamente para partida: $gameCode")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al conectar WebSocket: ${e.message}", e)
        }
    }

    /**
     * Conecta el WebSocket del monitor para obtener la lista de jugadores
     */
    fun connectMonitor(gameCode: String) {
        try {
            if (webSocketManager == null) {
                Log.e(TAG, "WebSocketManager es null, no se puede conectar monitor")
                return
            }
            
            Log.i(TAG, "Conectando Monitor WebSocket para partida: $gameCode")
            webSocketManager.connectMonitor(gameCode)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al conectar Monitor: ${e.message}", e)
        }
    }

    private fun setupWebSocketListeners() {
        webSocketManager?.let { manager ->
            viewModelScope.launch {
                // Escuchar notificaciones del WS de jugador
                manager.notifications.collect { notification: WsNotification ->
                    _wsNotifications.emit(notification.message)
                }
            }

            // Escuchar cambios en jugadores conectados (desde Monitor)
            viewModelScope.launch {
                manager.connectedPlayers.collect { players ->
                    _connectedPlayers.value = players
                    Log.i(TAG, "Jugadores conectados: ${players.size}")
                }
            }

            viewModelScope.launch {
                manager.messages.collect { message ->
                    if (message?.type == "GAME_STARTED") {
                        val userId = sessionManager.userId.first() ?: return@collect
                        val distrib = message.getDistribucionMisiones() ?: return@collect
                        val rawIds = (distrib[userId.toString()] as? List<*>) ?: return@collect
                        val missionIds = rawIds.mapNotNull {
                            when (it) {
                                is Double -> it.toInt(); is Int -> it; else -> null
                            }
                        }
                        val rooms = _missionsByRoom.value
                        _myMissions.value = missionIds.map { id ->
                            val roomName = rooms.entries
                                .firstOrNull { (_, v) -> (v as? List<*>)?.any { m -> (m as? Double)?.toInt() == id || m == id } == true }
                                ?.key ?: "Misión $id"
                            UserTaskUi(id = id.toLong(), nombre = "Misión en $roomName")
                        }
                    } else if (message?.type == "COMPLETE_MISSION") {
                        // Marcar misión como completada
                        val missionId = message.missionId?.toLong() ?: return@collect
                        _myMissions.value = _myMissions.value.map { mission ->
                            if (mission.id == missionId) mission.copy(completada = true) else mission
                        }
                    }
                }
            }
        }
    }

    fun sendMissionStarted(missionId: Int) {
        _currentMissionStartTime.value = System.currentTimeMillis()
        webSocketManager?.sendMissionStarted(missionId)
    }

    fun getMyPoints(scores: Map<String, Int>): Int {
        val id = _myUserId.value?.toString() ?: return 0
        return scores[id] ?: 0
    }

    fun sendMissionCompleted(missionId: Int) {
        _currentMissionStartTime.value = null
        webSocketManager?.sendMissionCompleted(missionId)
    }

    /**
     * Envía el evento de inicio de partida por WebSocket
     */
    fun iniciarPartidaPorWebSocket() {
        webSocketManager?.send(WebSocketManager.INICIO_PARTIDA)
    }

    /**
     * Envía el evento de fin de partida por WebSocket
     */
    fun finalizarPartidaPorWebSocket(ganadorTripulacion: Boolean? = null) {
        val extraFields = mutableMapOf<String, Any?>()
        if (ganadorTripulacion != null) {
            extraFields["ganador_tripulacion"] = ganadorTripulacion
        }
        webSocketManager?.send(WebSocketManager.FIN_PARTIDA, extraFields)
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager?.disconnect()
    }
}







