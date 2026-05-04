package cat.hajoya.piratasdeandromeda.data.repository

import android.content.Context
import cat.hajoya.piratasdeandromeda.data.local.AppDatabase
import cat.hajoya.piratasdeandromeda.data.local.CachedUser
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
import cat.hajoya.piratasdeandromeda.data.model.UserCreate
import cat.hajoya.piratasdeandromeda.data.model.UserResponse
import cat.hajoya.piratasdeandromeda.data.network.ApiService
import cat.hajoya.piratasdeandromeda.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/** Repositorio de usuario con estrategia online + cache offline. */
class UserRepository private constructor(
    private val apiService: ApiService,
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
) {

    /** Registra usuario; cachea y persiste sesión al éxito. */
    suspend fun register(request: UserCreate): Result<UserResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.register(request)
            database.userDao().insertOrReplace(response.toCachedUser())
            sessionManager.saveUserId(response.idUsuario)
            sessionManager.saveNombreUsuario(response.nombreUsuario)
            sessionManager.saveEmail(response.email)
            response
        }.recoverCatching { throwable ->
            throw throwable.toDomainException(prefix = "Error al registrar")
        }
    }

    /** Hace login; si no hay red intenta resolver contra cache local. */
    suspend fun login(request: UserCreate): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(request)
            database.userDao().insertOrReplace(response.toCachedUser())
            sessionManager.saveUserId(response.idUsuario)
            sessionManager.saveNombreUsuario(response.nombreUsuario)
            sessionManager.saveEmail(response.email)
            Result.success(response)
        } catch (io: IOException) {
            val cached = database.userDao().getUserByIdentity(request.nombreUsuario)
                ?: database.userDao().getUserByIdentity(request.email)
            if (cached != null) {
                sessionManager.saveUserId(cached.id_usuario)
                sessionManager.saveNombreUsuario(cached.nombre_usuario)
                sessionManager.saveEmail(cached.email)
                Result.success(cached.toUserResponse())
            } else {
                Result.failure(Exception("Sin conexión y sin usuario cacheado"))
            }
        } catch (http: HttpException) {
            Result.failure(Exception(http.toHttpMessage()))
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "Error al iniciar sesión"))
        }
    }

    /** Devuelve el id de usuario persistido en sesión. */
    fun getSavedUserId(): Flow<Int?> = sessionManager.userId

    private fun UserResponse.toCachedUser(): CachedUser = CachedUser(
        id_usuario = idUsuario,
        nombre_usuario = nombreUsuario,
        email = email,
        avatar_url = avatarUrl,
        id_rol_sistema = idRolSistema,
        total_partidas_jugadas = totalPartidasJugadas,
        total_puntos_acumulados = totalPuntosAcumulados,
        veces_impostor = vecesImpostor,
        veces_superviviente = vecesSuperviviente,
        veces_eliminado = vecesEliminado,
        fecha_ultima_conexion = fechaUltimaConexion,
    )

    private fun CachedUser.toUserResponse(): UserResponse = UserResponse(
        idUsuario = id_usuario,
        nombreUsuario = nombre_usuario,
        email = email,
        avatarUrl = avatar_url,
        idRolSistema = id_rol_sistema,
        totalPartidasJugadas = total_partidas_jugadas,
        totalPuntosAcumulados = total_puntos_acumulados,
        vecesImpostor = veces_impostor,
        vecesSuperviviente = veces_superviviente,
        vecesEliminado = veces_eliminado,
        fechaUltimaConexion = fecha_ultima_conexion,
    )

    companion object {
        @Volatile
        private var INSTANCE: UserRepository? = null

        /** Crea/recupera singleton del repositorio. */
        fun getInstance(context: Context): UserRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserRepository(
                    apiService = RetrofitClient.apiService,
                    database = AppDatabase.getInstance(context),
                    sessionManager = SessionManager(context.applicationContext),
                )
                INSTANCE = instance
                instance
            }
        }
    }
}

private fun Throwable.toDomainException(prefix: String): Exception {
    return when (this) {
        is IOException -> Exception("Sin conexión")
        is HttpException -> Exception(this.toHttpMessage())
        else -> Exception(message ?: prefix)
    }
}

private fun HttpException.toHttpMessage(): String {
    val body = response()?.errorBody()?.string().orEmpty().trim()
    return if (body.isNotEmpty()) body else "Error del servidor (${code()})"
}



