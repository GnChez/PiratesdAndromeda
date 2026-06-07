package cat.hajoya.piratasdeandromeda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Partida cacheada para lectura offline. */
@Entity(tableName = "cached_partidas")
data class CachedPartida(
    @PrimaryKey val id_partida: Int,
    val codigo_partida: String,
    val nombre_partida: String?,
    val id_estado_partida: Int,
    val ws_code: String,
    val ws_room_code: String,
)


