package cat.hajoya.piratasdeandromeda.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Base de datos local para cache offline. */
@Database(
    entities = [CachedUser::class, CachedPartida::class, ShipEntity::class, RoomEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    /** DAO de usuarios cacheados. */
    abstract fun userDao(): UserDao

    /** DAO de partidas cacheadas. */
    abstract fun partidaDao(): PartidaDao

    /** DAO de naves persistidas. */
    abstract fun shipDao(): ShipDao

    /** DAO de salas persistidas. */
    abstract fun roomDao(): RoomDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Obtiene una instancia singleton de la base de datos. */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "piratas_offline_db",
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

