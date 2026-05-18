package cat.hajoya.piratasdeandromeda.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.IniciBinding
import cat.hajoya.piratasdeandromeda.models.AuthState
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var _binding: IniciBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = IniciBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observar estado de autenticación
                launch {
                    viewModel.authState.observe(viewLifecycleOwner) { state ->
                        when (state) {
                            AuthState.LOADING -> {
                                binding.btnEntra.isEnabled = false
                                binding.btnEntra.alpha = 0.5f
                            }
                            AuthState.SUCCESS -> {
                                binding.btnEntra.isEnabled = true
                                binding.btnEntra.alpha = 1f
                                Snackbar.make(binding.root, getString(R.string.auth_session_started), Snackbar.LENGTH_SHORT).show()
                                // El MainActivity se encargará de navegar
                            }
                            AuthState.ERROR -> {
                                binding.btnEntra.isEnabled = true
                                binding.btnEntra.alpha = 1f
                            }
                            else -> {
                                binding.btnEntra.isEnabled = true
                                binding.btnEntra.alpha = 1f
                            }
                        }
                    }
                }
                // Observar mensajes de error
                launch {
                    viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
                        if (!message.isNullOrEmpty()) {
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.tvSignUp.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnEntra.setOnClickListener {
            val usuari = binding.etUsuari.text?.toString() ?: ""
            val contrasenya = binding.etContrasenya.text?.toString() ?: ""
            
            when {
                usuari.isEmpty() -> {
                    Snackbar.make(binding.root, getString(R.string.auth_missing_credentials), Snackbar.LENGTH_SHORT).show()
                }
                contrasenya.isEmpty() -> {
                    Snackbar.make(binding.root, getString(R.string.auth_missing_password), Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    viewModel.login(usuari, contrasenya)
                }
            }
        }
    }
}



