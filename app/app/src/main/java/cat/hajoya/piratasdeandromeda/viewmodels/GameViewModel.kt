package cat.hajoya.piratasdeandromeda.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.hajoya.piratasdeandromeda.RoomItem
import cat.hajoya.piratasdeandromeda.SavedShip
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("unused")
class GameViewModel(
    private val shipRepository: ShipRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _selectedShipId = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val selectedShipId: StateFlow<Long?> = _selectedShipId

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
            shipRepository.allRooms
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

    fun selectShip(shipId: Long) {
        _selectedShipId.value = shipId
    }

    fun crearPartidaLite(config: ConfigPartida) {
        val partida = Partida(
            id = 1,
            codiPartida = "12345",
            nomPartida = config.nomPartida,
            presencial = false,
            estatPartida = EstatPartida.ESPERANT_JUGADORS,
            numJugadors = 1,
            numImpostors = config.numImpostors,
            tempsLimitMinuts = config.tempsLimitMinuts,
            percentatgeReparacio = 0f,
            dificultad = config.dificultad,
        )
        _partidaActual.value = partida
        _percentatgeReparacio.value = 0f
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.saveGameCode(partida.codiPartida)
        }
    }

    fun unirsePartida(codi: String) {
        val partida = Partida(
            id = 1,
            codiPartida = codi,
            nomPartida = "Partida del codigo $codi",
            presencial = false,
            estatPartida = EstatPartida.ESPERANT_JUGADORS,
            numJugadors = 4,
            numImpostors = 1,
            tempsLimitMinuts = 10,
            percentatgeReparacio = 0f,
            dificultad = Dificultad.MITJA,
        )
        _partidaActual.value = partida
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.saveGameCode(codi)
        }
    }

    fun triarPersonatge(personatge: Personaje) {
        val jugador = JugadorPartida(
            id = 1,
            userId = 1,
            apodoPartida = "PirataValient",
            rol = RolJoc.TRIPULANT,
            viu = true,
            missionsCompletades = 0,
            punts = 0,
            personatge = personatge,
        )
        _jugadorActual.value = jugador
    }

    fun iniciarPartida() {
        _estatPartida.value = EstatPartida.EN_CURS
        _tempsRestant.value = 600
    }

    fun completarMissio(missioId: Int) {
        // Mock
    }

    fun sabotejar() {
        // Mock
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

    fun addSavedShip(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val newId = shipRepository.addShip(trimmed)
            _selectedShipId.value = newId
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
}

