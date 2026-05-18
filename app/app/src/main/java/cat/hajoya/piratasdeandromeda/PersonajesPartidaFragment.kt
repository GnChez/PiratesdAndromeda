package cat.hajoya.piratasdeandromeda

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import cat.hajoya.piratasdeandromeda.databinding.PersonajesPartidaBinding
import cat.hajoya.piratasdeandromeda.ui.joc.MenuJuegoFragment
import cat.hajoya.piratasdeandromeda.ui.utils.ToastHelper.showMessage
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.launch

class PersonajesPartidaFragment : Fragment() {

    companion object {
        private const val TAG = "PersonajesPartidaFragment"
    }

    private var _binding: PersonajesPartidaBinding? = null
    private val binding get() = _binding!!
    
    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = PersonajesPartidaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Verificar que WebSocket ya está conectado
        verificarConexionWebSocket()

        // Al presionar "Comenzar", enviar inicio de partida por WebSocket y ir al menú
        binding.btnEmpezar.setOnClickListener {
            navegarAlJuego()
        }
    }

    /**
     * Verifica que el WebSocket está conectado
     */
    private fun verificarConexionWebSocket() {
        lifecycleScope.launch {
            val wsGameCode = gameViewModel.wsGameCode.value
            val wsCode = gameViewModel.wsCode.value
            
            if (wsGameCode != null && wsCode != null) {
                Log.i(TAG, "✅ WebSocket ya conectado: Partida=$wsGameCode")
                showMessage(requireContext(), "✅ Conexión lista\n🎮 Código: $wsGameCode")
            } else {
                Log.w(TAG, "⚠️ WebSocket sin códigos disponibles")
                showMessage(requireContext(), "⚠️ Datos de conexión pendientes...")
            }
        }
    }

    /**
     * Navega al menú del juego
     */
    private fun navegarAlJuego() {
        Log.i(TAG, "Iniciando partida...")
        
        // Enviar evento de inicio de partida por WebSocket
        gameViewModel.iniciarPartidaPorWebSocket()
        
        // Navegar al menú del juego
        val menuJuegoFragment = MenuJuegoFragment()
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, menuJuegoFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

