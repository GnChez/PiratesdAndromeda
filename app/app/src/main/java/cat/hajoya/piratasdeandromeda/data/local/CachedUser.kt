
package cat.hajoya.piratasdeandromeda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Usuario cacheado para funcionamiento offline. */
@Entity(tableName = "cached_users")
data class CachedUser(
    @PrimaryKey val id_usuario: Int,
    val nombre_usuario: String,
    val email: String,
    val avatar_url: String?,
    val id_rol_sistema: Int,
    val total_partidas_jugadas: Int,
    val total_puntos_acumulados: Int,
    val veces_impostor: Int,
    val veces_superviviente: Int,
    val veces_eliminado: Int,
    val fecha_ultima_conexion: String?,
)


