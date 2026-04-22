package cat.hajoya.piratasdeandromeda.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ActivityAuthBinding
import cat.hajoya.piratasdeandromeda.models.AuthState
import cat.hajoya.piratasdeandromeda.models.RolUsuari
import cat.hajoya.piratasdeandromeda.ui.admin.AdminActivity
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModel

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
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


