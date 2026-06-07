package cat.hajoya.piratasdeandromeda.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
import cat.hajoya.piratasdeandromeda.databinding.ActivityAuthBinding
import cat.hajoya.piratasdeandromeda.models.AuthState
import cat.hajoya.piratasdeandromeda.models.RolUsuari
import cat.hajoya.piratasdeandromeda.ui.admin.AdminActivity
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModel
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModelFactory
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    internal val sessionManager by lazy { SessionManager(applicationContext) }
    internal val authViewModelFactory by lazy { AuthViewModelFactory(sessionManager) }
    private val viewModel: AuthViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]
    }
    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar el SplashScreen ANTES de llamar a super.onCreate()
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Mantener el SplashScreen visible mientras carga la sesión
        splashScreen.setKeepOnScreenCondition { isLoading }

        // LIMPIEZA: Borrar SIEMPRE la sesión al iniciar la app
        lifecycleScope.launch {
            sessionManager.clearSession()
        }

        // Mostrar Splash Screen durante mínimo 800ms
        lifecycleScope.launch {
            kotlinx.coroutines.delay(800)
            
            // SIEMPRE mostrar LoginFragment al iniciar
            isLoading = false
            if (savedInstanceState == null) {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.fragment_container, LoginFragment())
                }
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.authState.observe(this) { state ->
            if (state == AuthState.SUCCESS) {
                val user = viewModel.currentUser.value
                if (user != null) {
                    isLoading = false
                    navigateByRole(user.rol)
                }
            }
        }
    }

    private fun navigateByRole(rol: RolUsuari) {
        val intent = when (rol) {
            RolUsuari.JUGADOR -> Intent(this, MainActivity::class.java)
            RolUsuari.TREBALLADOR, RolUsuari.ADMIN -> Intent(this, AdminActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}


