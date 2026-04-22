package cat.hajoya.piratasdeandromeda.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.IniciBinding
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModel

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
        // Observar cambios de autenticación si es necesario
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
            viewModel.login(usuari, contrasenya)
        }
    }
}



