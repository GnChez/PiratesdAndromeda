package cat.hajoya.piratasdeandromeda.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cat.hajoya.piratasdeandromeda.models.User

class AdminViewModel : ViewModel() {
    private val _currentAdmin = MutableLiveData<User?>()
    val currentAdmin: LiveData<User?> = _currentAdmin

    private val _partidesActives = MutableLiveData<List<Any>>()
    val partidesActives: LiveData<List<Any>> = _partidesActives

    private val _partidaSeleccionada = MutableLiveData<Any?>()
    val partidaSeleccionada: LiveData<Any?> = _partidaSeleccionada

    private val _dispositius = MutableLiveData<List<Any>>()
    val dispositius: LiveData<List<Any>> = _dispositius

    private val _treballadors = MutableLiveData<List<User>>()
    val treballadors: LiveData<List<User>> = _treballadors

    fun iniciarPartidaSCROOM() {
        // Mock
    }

    fun buscarPartida(codi: String) {
        // Mock
    }

    fun crearTreballador(username: String, email: String, password: String) {
        // Mock
    }
}

