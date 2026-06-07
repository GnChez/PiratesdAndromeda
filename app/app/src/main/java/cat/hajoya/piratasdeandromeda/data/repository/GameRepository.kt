package cat.hajoya.piratasdeandromeda.data.repository

import android.content.Context
import cat.hajoya.piratasdeandromeda.data.local.AppDatabase
import cat.hajoya.piratasdeandromeda.data.local.CachedPartida
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
import cat.hajoya.piratasdeandromeda.data.model.PartidaEndRequest
import cat.hajoya.piratasdeandromeda.data.model.PartidaEndResponse
import cat.hajoya.piratasdeandromeda.data.model.PartidaInitialResponse
import cat.hajoya.piratasdeandromeda.data.model.PartidaJoinRequest
import cat.hajoya.piratasdeandromeda.data.model.PartidaJoinResponse
import cat.hajoya.piratasdeandromeda.data.model.PartidaLeaveRequest
import cat.hajoya.piratasdeandromeda.data.model.PartidaPedido
import cat.hajoya.piratasdeandromeda.data.model.PartidaStartRequest
import cat.hajoya.piratasdeandromeda.data.model.PartidaStartResponse
import cat.hajoya.piratasdeandromeda.data.network.ApiService
import cat.hajoya.piratasdeandromeda.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/** Repositorio de partida con cache local y manejo de errores amigable. */
class GameRepository private constructor(
    private val apiService: ApiService,
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
) {

    /** Crea partida y la cachea localmente. */
    suspend fun createGame(request: PartidaPedido): Result<PartidaInitialResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.createGame(request)
                database.partidaDao().insertOrReplace(
                    CachedPartida(
                        id_partida = response.idPartida,
                        codigo_partida = response.codigoPartida,
                        nombre_partida = request.nombrePartida,
                        id_estado_partida = response.idEstadoPartida,
                        ws_code = response.wsCode,
                        ws_room_code = response.wsRoomCode,
                    ),
                )
                sessionManager.saveGameCode(response.codigoPartida)
                sessionManager.saveWsCode(response.wsCode)
                Result.success(response)
            } catch (io: IOException) {
                Result.failure(Exception("Sin conexión"))
            } catch (http: HttpException) {
                Result.failure(Exception(http.toHttpMessage()))
            } catch (t: Throwable) {
                Result.failure(Exception(t.message ?: "No se pudo crear la partida"))
            }
        }

    /** Une jugador a partida. */
    suspend fun joinGame(request: PartidaJoinRequest): Result<PartidaJoinResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.joinGame(request)
                sessionManager.saveGameCode(request.codigoPartida)
                sessionManager.saveWsCode(response.wsCode)
                Result.success(response)
            } catch (io: IOException) {
                Result.failure(Exception("Sin conexión"))
            } catch (http: HttpException) {
                Result.failure(Exception(http.toHttpMessage()))
            } catch (t: Throwable) {
                Result.failure(Exception(t.message ?: "No se pudo unir a la partida"))
            }
        }

    /** Sale de partida. */
    suspend fun leaveGame(request: PartidaLeaveRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.leaveGame(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error del servidor (${response.code()})"))
            }
        } catch (io: IOException) {
            Result.failure(Exception("Sin conexión"))
        } catch (http: HttpException) {
            Result.failure(Exception(http.toHttpMessage()))
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "No se pudo salir de la partida"))
        }
    }

    /** Inicia partida. */
    suspend fun startGame(request: PartidaStartRequest): Result<PartidaStartResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(apiService.startGame(request))
            } catch (io: IOException) {
                Result.failure(Exception("Sin conexión"))
            } catch (http: HttpException) {
                Result.failure(Exception(http.toHttpMessage()))
            } catch (t: Throwable) {
                Result.failure(Exception(t.message ?: "No se pudo iniciar la partida"))
            }
        }

    /** Finaliza partida. */
    suspend fun endGame(request: PartidaEndRequest): Result<PartidaEndResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(apiService.endGame(request))
            } catch (io: IOException) {
                Result.failure(Exception("Sin conexión"))
            } catch (http: HttpException) {
                Result.failure(Exception(http.toHttpMessage()))
            } catch (t: Throwable) {
                Result.failure(Exception(t.message ?: "No se pudo finalizar la partida"))
            }
        }

    /** Recupera la última partida cacheada para modo offline. */
    suspend fun getLastCachedGame(): CachedPartida? = withContext(Dispatchers.IO) {
        database.partidaDao().getLastPartida()
    }

    companion object {
        @Volatile
        private var INSTANCE: GameRepository? = null

        /** Crea/recupera singleton del repositorio. */
        fun getInstance(context: Context): GameRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = GameRepository(
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

private fun HttpException.toHttpMessage(): String {
    val body = response()?.errorBody()?.string().orEmpty().trim()
    return if (body.isNotEmpty()) body else "Error del servidor (${code()})"
}


