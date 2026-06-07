package cat.hajoya.piratasdeandromeda.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** DAO de partidas cacheadas. */
@Dao
interface PartidaDao {

    /** Inserta o reemplaza partida en cache. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(partida: CachedPartida)

    /** Recupera una partida por código. */
    @Query("SELECT * FROM cached_partidas WHERE codigo_partida = :codigo LIMIT 1")
    suspend fun getPartidaByCodigo(codigo: String): CachedPartida?

    /** Recupera la última partida guardada. */
    @Query("SELECT * FROM cached_partidas ORDER BY id_partida DESC LIMIT 1")
    suspend fun getLastPartida(): CachedPartida?

    /** Borra todas las partidas cacheadas. */
    @Query("DELETE FROM cached_partidas")
    suspend fun deleteAll()
}


