package cat.hajoya.piratasdeandromeda.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Sala persistida localmente y asociada a una nave. */
@Entity(
    tableName = "rooms",
    indices = [Index(value = ["shipId"])],
    foreignKeys = [
        ForeignKey(
            entity = ShipEntity::class,
            parentColumns = ["id"],
            childColumns = ["shipId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shipId: Long,
    val name: String,
)

