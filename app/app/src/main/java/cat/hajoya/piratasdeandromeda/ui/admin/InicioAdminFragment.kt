package cat.hajoya.piratasdeandromeda.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.StartFrBinding
import cat.hajoya.piratasdeandromeda.ui.auth.AuthActivity
import cat.hajoya.piratasdeandromeda.ui.joc.EstadoPartPersonalFragment
import cat.hajoya.piratasdeandromeda.ui.preparacio.SettingsFragment
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModel
import cat.hajoya.piratasdeandromeda.viewmodels.AuthViewModelFactory
import cat.hajoya.piratasdeandromeda.data.local.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class InicioAdminFragment : Fragment() {

    private var _binding: StartFrBinding? = null
    private val binding get() = _binding!!
    
    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val authViewModelFactory by lazy { AuthViewModelFactory(sessionManager) }
    private val authViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = StartFrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Cambiar textos para admin
        binding.tvWelcomeTitle.text = "Monitor de Partidas"
        binding.tvCreateLabel.text = "Opciones"
        binding.btnCrear.text = "Cerrar Sesión"
        binding.btnUnirse.text = "Buscar Partida"
        
        // Habilitar botones
        binding.btnCrear.isEnabled = true
        binding.btnUnirse.isEnabled = true
        
        // Mostrar botón de perfil
        binding.btnCloseSession.visibility = View.VISIBLE
        
        setupListeners()
    }

    private fun setupListeners() {
        // Botón de perfil - abrir configuración de usuario
        binding.btnCloseSession.setOnClickListener {
            val settingsFragment = SettingsFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, settingsFragment)
                .addToBackStack(null)
                .commit()
        }
        
        // Botón de logout
        binding.btnCrear.setOnClickListener {
            logout()
        }
        
        // Buscar partida por código
        binding.btnUnirse.setOnClickListener {
            val gameCode = binding.edPartidaCode.text?.toString()?.trim() ?: ""
            if (gameCode.isEmpty()) {
                Snackbar.make(binding.root, "Ingresa un código de partida", Snackbar.LENGTH_SHORT).show()
            } else {
                buscarPartida(gameCode)
            }
        }
    }
    
    private fun buscarPartida(gameCode: String) {
        lifecycleScope.launch {
            // Navegar a EstadoPartPersonalFragment con el código
            val fragment = EstadoPartPersonalFragment.newInstance(gameCode)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
    
    private fun logout() {
        // Limpiar sesión y volver a login
        lifecycleScope.launch {
            authViewModel.logout()
            
            // Volver a la pantalla de login
            val activity = requireActivity()
            activity.startActivity(
                Intent(activity, AuthActivity::class.java)
            )
            activity.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

