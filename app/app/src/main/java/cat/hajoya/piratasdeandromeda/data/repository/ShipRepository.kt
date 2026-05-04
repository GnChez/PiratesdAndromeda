package cat.hajoya.piratasdeandromeda.data.repository

import cat.hajoya.piratasdeandromeda.RoomItem
import cat.hajoya.piratasdeandromeda.SavedShip
import cat.hajoya.piratasdeandromeda.data.local.RoomDao
import cat.hajoya.piratasdeandromeda.data.local.RoomEntity
import cat.hajoya.piratasdeandromeda.data.local.ShipDao
import cat.hajoya.piratasdeandromeda.data.local.ShipEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio local para naves y habitaciones.
 *
 * Solo opera con Room para mantener la UI persistida sin red.
 */
class ShipRepository(
    private val shipDao: ShipDao,
    private val roomDao: RoomDao,
) {

    /** Flow de todas las naves guardadas. */
    val allShips: Flow<List<SavedShip>> = shipDao.getAllShips().map { list ->
        list.map { SavedShip(it.id, it.name) }
    }

    /** Flow de habitaciones de la nave actualmente seleccionada. */
    fun getRoomsForShip(shipId: Long): Flow<List<RoomItem>> =
        roomDao.getRoomsForShip(shipId).map { list ->
            list.map { it.toRoomItem() }
        }

    /** Flow de todas las habitaciones, sin filtro. */
    val allRooms: Flow<List<RoomItem>> = roomDao.getAllRooms().map { list ->
        list.map { it.toRoomItem() }
    }

    /** Inserta una nave nueva y devuelve su ID. */
    suspend fun addShip(name: String): Long = shipDao.insert(ShipEntity(name = name))

    /** Elimina una nave; el cascade borra sus habitaciones. */
    suspend fun deleteShip(id: Long) = shipDao.deleteById(id)

    /** Añade una habitación a la nave activa. */
    suspend fun addRoom(shipId: Long, name: String): Long =
        roomDao.insert(RoomEntity(shipId = shipId, name = name))

    /** Elimina una habitación. */
    suspend fun deleteRoom(id: Long) = roomDao.deleteById(id)
}

private fun RoomEntity.toRoomItem(): RoomItem = RoomItem(
    id = id,
    name = name,
    shipId = shipId,
)

