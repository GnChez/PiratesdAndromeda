package cat.hajoya.piratasdeandromeda.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cat.hajoya.piratasdeandromeda.databinding.RegisterBinding
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModel

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
        // Observar cambios si es necesario
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


