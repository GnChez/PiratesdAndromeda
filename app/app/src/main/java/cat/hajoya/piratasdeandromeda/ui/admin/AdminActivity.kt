package cat.hajoya.piratasdeandromeda.ui.admin

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
import cat.hajoya.piratasdeandromeda.viewmodels.AdminViewModel
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import cat.hajoya.piratasdeandromeda.viewmodels.SettingsViewModelFactory

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adminViewModel: AdminViewModel
    
    internal val sessionManager by lazy { SessionManager(applicationContext) }
    
    internal val gameViewModelFactory: ViewModelProvider.Factory by lazy {
        val db = AppDatabase.getInstance(applicationContext)
        val shipRepo = ShipRepository(db.shipDao(), db.roomDao())
        val gameRepo = GameRepository.getInstance(applicationContext)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return GameViewModel(shipRepo, gameRepo, sessionManager, WebSocketManager()) as T
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

        adminViewModel = ViewModelProvider(this)[AdminViewModel::class.java]

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, InicioAdminFragment())
            }
        }
    }
}


