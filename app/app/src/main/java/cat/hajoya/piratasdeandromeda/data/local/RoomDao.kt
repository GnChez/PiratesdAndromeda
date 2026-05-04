package cat.hajoya.piratasdeandromeda.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** DAO de habitaciones persistidas. */
@Dao
interface RoomDao {

    /** Devuelve las habitaciones de una nave concreta. */
    @Query("SELECT * FROM rooms WHERE shipId = :shipId")
    fun getRoomsForShip(shipId: Long): Flow<List<RoomEntity>>

    /** Devuelve todas las habitaciones. */
    @Query("SELECT * FROM rooms")
    fun getAllRooms(): Flow<List<RoomEntity>>

    /** Inserta una habitación y devuelve su id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(room: RoomEntity): Long

    /** Elimina una habitación por id. */
    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Elimina todas las habitaciones de una nave. */
    @Query("DELETE FROM rooms WHERE shipId = :shipId")
    suspend fun deleteByShipId(shipId: Long)
}

