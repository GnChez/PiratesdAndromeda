package cat.hajoya.piratasdeandromeda.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ActivityMainBinding
import cat.hajoya.piratasdeandromeda.data.local.AppDatabase
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
import cat.hajoya.piratasdeandromeda.data.network.WebSocketManager
import cat.hajoya.piratasdeandromeda.data.repository.GameRepository
import cat.hajoya.piratasdeandromeda.data.repository.ShipRepository
import cat.hajoya.piratasdeandromeda.ui.preparacio.StartPartidaFragment
import cat.hajoya.piratasdeandromeda.viewmodels.SettingsViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    internal val sessionManager by lazy { SessionManager(applicationContext) }
    internal val gameViewModelFactory: ViewModelProvider.Factory by lazy {
        val db = AppDatabase.getInstance(applicationContext)
        val shipRepo = ShipRepository(db.shipDao(), db.roomDao())
        val gameRepo = GameRepository.getInstance(applicationContext)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel(shipRepo, gameRepo, sessionManager,
                        WebSocketManager()
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
    internal val settingsViewModelFactory by lazy {
        SettingsViewModelFactory(sessionManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, StartPartidaFragment())
            }
        }
    }
}

