package cat.hajoya.piratasdeandromeda.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** DAO de naves persistidas. */
@Dao
interface ShipDao {

    /** Obtiene todas las naves ordenadas por creación. */
    @Query("SELECT * FROM ships ORDER BY createdAt DESC")
    fun getAllShips(): Flow<List<ShipEntity>>

    /** Inserta una nave y devuelve su id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ship: ShipEntity): Long

    /** Elimina una nave por id. */
    @Query("DELETE FROM ships WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Borra todas las naves. */
    @Query("DELETE FROM ships")
    suspend fun deleteAll()
}

