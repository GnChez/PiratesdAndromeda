package cat.hajoya.piratasdeandromeda.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import cat.hajoya.piratasdeandromeda.databinding.RegisterBinding
import cat.hajoya.piratasdeandromeda.models.AuthState
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: RegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RegisterBinding.inflate(inflater, container, false)
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
                                binding.btnRegistrate.isEnabled = false
                                binding.btnRegistrate.alpha = 0.5f
                            }
                            AuthState.SUCCESS -> {
                                binding.btnRegistrate.isEnabled = true
                                binding.btnRegistrate.alpha = 1f
                                Snackbar.make(binding.root, "Registro exitoso", Snackbar.LENGTH_SHORT).show()
                                // El MainActivity se encargará de navegar
                            }
                            AuthState.ERROR -> {
                                binding.btnRegistrate.isEnabled = true
                                binding.btnRegistrate.alpha = 1f
                            }
                            else -> {
                                binding.btnRegistrate.isEnabled = true
                                binding.btnRegistrate.alpha = 1f
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
        binding.tvLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnRegistrate.setOnClickListener {
            val email = binding.etUsuari.text?.toString() ?: ""
            val username = binding.etUserName.text?.toString() ?: ""
            val password = binding.etContrasenya.text?.toString() ?: ""
            val passwordRep = binding.etContrasenyaRep.text?.toString() ?: ""
            
            if (password == passwordRep) {
                viewModel.register(username, email, password)
            }
        }
    }
}


