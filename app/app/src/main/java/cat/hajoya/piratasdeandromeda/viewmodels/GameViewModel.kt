package cat.hajoya.piratasdeandromeda.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cat.hajoya.piratasdeandromeda.models.ConfigPartida
import cat.hajoya.piratasdeandromeda.models.Dificultad
import cat.hajoya.piratasdeandromeda.models.EstatPartida
import cat.hajoya.piratasdeandromeda.models.Habitacio
import cat.hajoya.piratasdeandromeda.models.JugadorPartida
import cat.hajoya.piratasdeandromeda.models.Missio
import cat.hajoya.piratasdeandromeda.models.Partida
import cat.hajoya.piratasdeandromeda.models.Personaje
import cat.hajoya.piratasdeandromeda.models.RolJoc
import cat.hajoya.piratasdeandromeda.RoomItem
import cat.hajoya.piratasdeandromeda.SavedShip

class GameViewModel : ViewModel() {
    private var nextShipId = 3L
    private var nextRoomId = 3L

    private val _savedShips = MutableLiveData(
        listOf(
            SavedShip(1L, "Nave Ron Derramado"),
            SavedShip(2L, "La Gaviota Negra"),
        )
    )
    val savedShips: LiveData<List<SavedShip>> = _savedShips

    private val _rooms = MutableLiveData(
        listOf(
            RoomItem(1L, "Sala de combate"),
            RoomItem(2L, "Sala de motores"),
        )
    )
    val rooms: LiveData<List<RoomItem>> = _rooms

    private val _partidaActual = MutableLiveData<Partida?>()
    val partidaActual: LiveData<Partida?> = _partidaActual

    private val _jugadorActual = MutableLiveData<JugadorPartida?>()
    val jugadorActual: LiveData<JugadorPartida?> = _jugadorActual

    private val _estatPartida = MutableLiveData<EstatPartida>()
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
            dificultad = config.dificultad
        )
        _partidaActual.value = partida
        _percentatgeReparacio.value = 0f
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
            dificultad = Dificultad.MITJA
        )
        _partidaActual.value = partida
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
            personatge = personatge
        )
        _jugadorActual.value = jugador
    }

    fun iniciarPartida() {
        _estatPartida.value = EstatPartida.EN_CURS
        _tempsRestant.value = 600 // 10 minutos en segundos
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
        _savedShips.value = _savedShips.value.orEmpty() + SavedShip(nextShipId++, trimmed)
    }

    fun deleteSavedShip(shipId: Long) {
        _savedShips.value = _savedShips.value.orEmpty().filterNot { it.id == shipId }
    }

    fun addRoom(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _rooms.value = _rooms.value.orEmpty() + RoomItem(nextRoomId++, trimmed)
    }

    fun deleteRoom(roomId: Long) {
        _rooms.value = _rooms.value.orEmpty().filterNot { it.id == roomId }
    }
}

