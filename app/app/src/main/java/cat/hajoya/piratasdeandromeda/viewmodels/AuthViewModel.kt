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

    fun login(userInput: String, password: String) {

        if (userInput.isEmpty() || password.isEmpty()) {
            _errorMessage.value = "Por favor completa todos los campos"
            _authState.value = AuthState.ERROR
            return
        }

        _authState.value = AuthState.LOADING

        viewModelScope.launch(Dispatchers.IO) {

            try {

                val isEmail = userInput.contains("@")

                val request = UserCreate(
                    nombreUsuario = if (isEmail) "" else userInput,
                    email = if (isEmail) userInput else "",
                    password = password
                )

                val userResponse = apiService.login(request)

                // Mapear id_rol_sistema a RolUsuari
                val role = when (userResponse.idRolSistema) {
                    1 -> RolUsuari.JUGADOR
                    2 -> RolUsuari.TREBALLADOR
                    3 -> RolUsuari.ADMIN
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

            } catch (_: SocketTimeoutException) {

                _authState.postValue(AuthState.ERROR)
                _errorMessage.postValue(
                    "La API no responde a tiempo. Revisa tu conexión a internet o que el servidor esté disponible."
                )

                _currentUser.postValue(null)

            } catch (e: Exception) {

                _authState.postValue(AuthState.ERROR)
                _errorMessage.postValue(
                    "Error en login: ${e.message}"
                )

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
                
                // Mapear id_rol_sistema a RolUsuari
                val role = when (userResponse.idRolSistema) {
                    1 -> RolUsuari.JUGADOR
                    2 -> RolUsuari.TREBALLADOR
                    3 -> RolUsuari.ADMIN
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
            } catch (_: SocketTimeoutException) {
                _authState.postValue(AuthState.ERROR)
                _errorMessage.postValue("La API no responde a tiempo. Revisa tu conexión a internet o que el servidor esté disponible.")
                _currentUser.postValue(null)
            } catch (_: Exception) {
                _authState.postValue(AuthState.ERROR)
                _errorMessage.postValue("Error en registro: Desconocido")
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
            // Mapear RolUsuari a id_rol_sistema
            val roleId = when (user.rol) {
                RolUsuari.JUGADOR -> 1
                RolUsuari.TREBALLADOR -> 2
                RolUsuari.ADMIN -> 3
            }
            sessionManager.saveUserRole(roleId)
        }
    }
}

