package cat.hajoya.piratasdeandromeda.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
import cat.hajoya.piratasdeandromeda.databinding.ActivityAuthBinding
import cat.hajoya.piratasdeandromeda.models.AuthState
import cat.hajoya.piratasdeandromeda.models.RolUsuari
import cat.hajoya.piratasdeandromeda.ui.admin.AdminActivity
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModel
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModelFactory

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    internal val sessionManager by lazy { SessionManager(applicationContext) }
    internal val authViewModelFactory by lazy { AuthViewModelFactory(sessionManager) }
    private val viewModel: AuthViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, LoginFragment())
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.authState.observe(this) { state ->
            if (state == AuthState.SUCCESS) {
                val user = viewModel.currentUser.value
                if (user != null) {
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


