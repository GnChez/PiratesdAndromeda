package cat.hajoya.piratasdeandromeda.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
import cat.hajoya.piratasdeandromeda.data.network.WebSocketManager
import cat.hajoya.piratasdeandromeda.data.repository.GameRepository
import cat.hajoya.piratasdeandromeda.data.repository.ShipRepository

/** Factory para [GameViewModel]. */
class GameViewModelFactory(
    private val shipRepository: ShipRepository,
    private val gameRepository: GameRepository,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager = WebSocketManager(),
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(
                shipRepository,
                gameRepository,
                sessionManager,
                webSocketManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

