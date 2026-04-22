package cat.hajoya.piratasdeandromeda.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cat.hajoya.piratasdeandromeda.models.AuthState
import cat.hajoya.piratasdeandromeda.models.RolUsuari
import cat.hajoya.piratasdeandromeda.models.User

class AuthViewModel : ViewModel() {
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _authState = MutableLiveData<AuthState>(AuthState.IDLE)
    val authState: LiveData<AuthState> = _authState

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _errorMessage.value = "Por favor completa todos los campos"
            _authState.value = AuthState.ERROR
            return
        }

        _authState.value = AuthState.LOADING

        // Mock login (temporalmente)
        val role = when {
            email.contains("admin", ignoreCase = true) -> RolUsuari.ADMIN
            email.contains("trab", ignoreCase = true) -> RolUsuari.TREBALLADOR
            else -> RolUsuari.JUGADOR
        }

        val user = User(
            id = 1,
            username = "Jugador_Pirata",
            email = email,
            rol = role,
            avatar = null
        )
        _currentUser.value = user
        _authState.value = AuthState.SUCCESS
        _errorMessage.value = null
    }

    fun register(username: String, email: String, password: String) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _errorMessage.value = "Por favor completa todos los campos"
            _authState.value = AuthState.ERROR
            return
        }

        _authState.value = AuthState.LOADING

        // Mock register (temporalmente)
        val role = when {
            email.contains("admin", ignoreCase = true) -> RolUsuari.ADMIN
            email.contains("trab", ignoreCase = true) -> RolUsuari.TREBALLADOR
            else -> RolUsuari.JUGADOR
        }

        val user = User(
            id = 2,
            username = username,
            email = email,
            rol = role,
            avatar = null
        )
        _currentUser.value = user
        _authState.value = AuthState.SUCCESS
        _errorMessage.value = null
    }

    fun logout() {
        _currentUser.value = null
        _authState.value = AuthState.IDLE
        _errorMessage.value = null
    }

    fun resetAuthState() {
        _authState.value = AuthState.IDLE
    }
}

