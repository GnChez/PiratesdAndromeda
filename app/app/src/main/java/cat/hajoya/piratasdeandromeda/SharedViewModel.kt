package cat.hajoya.piratasdeandromeda

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel compartido entre los Fragments de la Activity.
 * Al usar activityViewModels() en los Fragments, todos comparten esta misma instancia.
 */
class SharedViewModel : ViewModel() {
	private var nextShipId = 3L

	private val _savedShips = MutableLiveData(
		listOf(
			SavedShip(1L, "Nave Ron Derramado"),
			SavedShip(2L, "La Gaviota Negra"),
		)
	)
	val savedShips: LiveData<List<SavedShip>> = _savedShips

	fun addSavedShip(name: String) {
		val trimmedName = name.trim()
		if (trimmedName.isEmpty()) return

		val updatedList = _savedShips.value.orEmpty() + SavedShip(nextShipId++, trimmedName)
		_savedShips.value = updatedList
	}

	fun deleteSavedShip(shipId: Long) {
		_savedShips.value = _savedShips.value.orEmpty().filterNot { it.id == shipId }
	}
}