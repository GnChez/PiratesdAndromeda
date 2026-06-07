package cat.hajoya.piratasdeandromeda.data.network

import cat.hajoya.piratasdeandromeda.data.model.PartidaEndRequest
import cat.hajoya.piratasdeandromeda.data.model.PartidaEndResponse
import cat.hajoya.piratasdeandromeda.data.model.PartidaInitialResponse
import cat.hajoya.piratasdeandromeda.data.model.PartidaJoinRequest
import cat.hajoya.piratasdeandromeda.data.model.PartidaJoinResponse
import cat.hajoya.piratasdeandromeda.data.model.PartidaLeaveRequest
import cat.hajoya.piratasdeandromeda.data.model.PartidaPedido
import cat.hajoya.piratasdeandromeda.data.model.PartidaStartRequest
import cat.hajoya.piratasdeandromeda.data.model.PartidaStartResponse
import cat.hajoya.piratasdeandromeda.data.model.UserCreate
import cat.hajoya.piratasdeandromeda.data.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Endpoints HTTP de backend. */
interface ApiService {

    /** Health/home endpoint. */
    @GET("/")
    suspend fun root(): Map<String, String>

    /** Registra un usuario nuevo. */
    @POST("/users/register")
    suspend fun register(@Body request: UserCreate): UserResponse

    /** Login de usuario existente. */
    @POST("/users/login")
    suspend fun login(@Body request: UserCreate): UserResponse

    /** Crea una partida. */
    @POST("/games/create")
    suspend fun createGame(@Body request: PartidaPedido): PartidaInitialResponse

    /** Une un jugador a una partida. */
    @POST("/games/join")
    suspend fun joinGame(@Body request: PartidaJoinRequest): PartidaJoinResponse

    /** Saca a un jugador de la partida. */
    @POST("/games/leave")
    suspend fun leaveGame(@Body request: PartidaLeaveRequest): Response<Unit>

    /** Inicia partida. */
    @POST("/games/start")
    suspend fun startGame(@Body request: PartidaStartRequest): PartidaStartResponse

    /** Finaliza partida. */
    @POST("/games/end")
    suspend fun endGame(@Body request: PartidaEndRequest): PartidaEndResponse
}

