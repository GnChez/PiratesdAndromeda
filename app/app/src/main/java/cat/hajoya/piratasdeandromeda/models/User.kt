package cat.hajoya.piratasdeandromeda.models

enum class RolUsuari {
    JUGADOR, TREBALLADOR, ADMIN
}

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val rol: RolUsuari,
    val avatar: String? = null
)

enum class AuthState {
    LOADING, SUCCESS, ERROR, IDLE
}

