package cat.hajoya.piratasdeandromeda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Nave persistida localmente. */
@Entity(tableName = "ships")
data class ShipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

