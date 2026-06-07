package cat.hajoya.piratasdeandromeda.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel de ajustes con persistencia en DataStore.
 */
@Suppress("unused")
class SettingsViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {

    /** Nombre de usuario actual. */
    val username: StateFlow<String> = sessionManager.nombreUsuario
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Email actual. */
    val email: StateFlow<String> = sessionManager.email
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Estado del modo oscuro. */
    val darkModeEnabled: StateFlow<Boolean> = sessionManager.darkModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Idioma seleccionado. */
    val selectedLanguage: StateFlow<String> = sessionManager.selectedLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Català")

    /** Guarda el nombre de usuario. */
    fun saveUsername(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.saveNombreUsuario(trimmed)
        }
    }

    /** Guarda el email. */
    fun saveEmail(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.saveEmail(trimmed)
        }
    }

    /** Guarda el modo oscuro. */
    fun saveDarkMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.saveDarkMode(enabled)
        }
    }

    /** Guarda el idioma. */
    fun saveLanguage(lang: String) {
        val trimmed = lang.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.saveLanguage(trimmed)
        }
    }

    /** Limpia la sesión y ejecuta la navegación final en main thread. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.clearSession()
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}


