package cat.hajoya.piratasdeandromeda.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** DAO de usuario cacheado. */
@Dao
interface UserDao {

    /** Inserta o reemplaza un usuario local. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(user: CachedUser)

    /** Obtiene usuario por id. */
    @Query("SELECT * FROM cached_users WHERE id_usuario = :id LIMIT 1")
    suspend fun getUserById(id: Int): CachedUser?

    /** Busca usuario por nombre o email para login offline. */
    @Query(
        "SELECT * FROM cached_users " +
            "WHERE nombre_usuario = :identity OR email = :identity LIMIT 1",
    )
    suspend fun getUserByIdentity(identity: String): CachedUser?

    /** Borra todos los usuarios cacheados. */
    @Query("DELETE FROM cached_users")
    suspend fun deleteAll()
}


