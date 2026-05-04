package cat.hajoya.piratasdeandromeda.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
import cat.hajoya.piratasdeandromeda.data.model.UserCreate
import cat.hajoya.piratasdeandromeda.data.network.RetrofitClient
import cat.hajoya.piratasdeandromeda.models.AuthState
import cat.hajoya.piratasdeandromeda.models.RolUsuari
import cat.hajoya.piratasdeandromeda.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

class AuthViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val apiService = RetrofitClient.apiService
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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Crear request con email y password
                val request = UserCreate(
                    nombreUsuario = email.substringBefore("@"), // Extender username del email
                    email = email,
                    password = password
                )
                // Llamar al endpoint de login
                val userResponse = apiService.login(request)
                
                // Mapear respuesta a modelo local
                val role = when {
                    email.contains("admin", ignoreCase = true) -> RolUsuari.ADMIN
                    email.contains("trab", ignoreCase = true) -> RolUsuari.TREBALLADOR
                    else -> RolUsuari.JUGADOR
                }
                
                val user = User(
                    id = userResponse.idUsuario,
                    username = userResponse.nombreUsuario,
                    email = userResponse.email,
                    rol = role,
                    avatar = userResponse.avatarUrl
                )
                
                _currentUser.postValue(user)
                _authState.postValue(AuthState.SUCCESS)
                _errorMessage.postValue(null)
                persistSession(user)
            } catch (e: SocketTimeoutException) {
                _authState.postValue(AuthState.ERROR)
                _errorMessage.postValue("La API no responde a tiempo. Revisa que el backend esté levantado y accesible en 10.0.2.2:8000.")
                _currentUser.postValue(null)
            } catch (e: Exception) {
                _authState.postValue(AuthState.ERROR)
                _errorMessage.postValue("Error en login: ${e.message ?: "Desconocido"}")
                _currentUser.postValue(null)
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _errorMessage.value = "Por favor completa todos los campos"
            _authState.value = AuthState.ERROR
            return
        }

        _authState.value = AuthState.LOADING

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Crear request
                val request = UserCreate(
                    nombreUsuario = username,
                    email = email,
                    password = password
                )
                // Llamar al endpoint de registro
                val userResponse = apiService.register(request)
                
                // Mapear respuesta a modelo local
                val role = when {
                    email.contains("admin", ignoreCase = true) -> RolUsuari.ADMIN
                    email.contains("trab", ignoreCase = true) -> RolUsuari.TREBALLADOR
                    else -> RolUsuari.JUGADOR
                }
                
                val user = User(
                    id = userResponse.idUsuario,
                    username = userResponse.nombreUsuario,
                    email = userResponse.email,
                    rol = role,
                    avatar = userResponse.avatarUrl
                )
                
                _currentUser.postValue(user)
                _authState.postValue(AuthState.SUCCESS)
                _errorMessage.postValue(null)
                persistSession(user)
            } catch (e: SocketTimeoutException) {
                _authState.postValue(AuthState.ERROR)
                _errorMessage.postValue("La API no responde a tiempo. Revisa que el backend esté levantado y accesible en 10.0.2.2:8000.")
                _currentUser.postValue(null)
            } catch (e: Exception) {
                _authState.postValue(AuthState.ERROR)
                _errorMessage.postValue("Error en registro: ${e.message ?: "Desconocido"}")
                _currentUser.postValue(null)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _authState.value = AuthState.IDLE
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.clearSession()
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.IDLE
    }

    private fun persistSession(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.saveUserId(user.id)
            sessionManager.saveNombreUsuario(user.username)
            sessionManager.saveEmail(user.email)
        }
    }
}

