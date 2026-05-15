package cat.hajoya.piratasdeandromeda.ui.joc

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ActivityMenuGameBinding
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.launch

/**
 * Fragment que muestra el menú principal del juego.
 * Permite acceder a diferentes pantallas del juego:
 * - Camarotes
 * - Labores
 * - Reunirse
 * - Soltar armas (salir)
 */
class MenuJuegoFragment : Fragment() {

    private var _binding: ActivityMenuGameBinding? = null
    private val binding get() = _binding!!
    
    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ActivityMenuGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup title con colores mixtos (accesibilidad)
        setupTitle()
        
        // Escuchar cambios en puntuaciones
        lifecycleScope.launch {
            gameViewModel.playerScores.collect { scores ->
                updatePlayerScores(scores)
            }
        }
        
        // Setup button listeners
        setupButtonListeners()
    }

    /**
     * Configura el título con estilos mixtos (nombre del usuario en blanco bold, resto en color más claro)
     */
    private fun setupTitle() {
        val username = "Corsario"  // Aquí podrías obtener del usuario actual
        val fullText = "$username, eres parte de esta tripulación!"
        
        val spannableString = SpannableString(fullText)
        
        // Aplicar estilo bold y color blanco al nombre del usuario
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            username.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.blanco)),
            0,
            username.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        // Color más claro para el resto del texto
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.anaranjado)),
            username.length,
            fullText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        binding.tvHeroTitle.text = spannableString
        binding.tvHeroTitle.isFocusable = false
        binding.tvHeroTitle.isClickable = false
    }

    private fun updatePlayerScores(scores: Map<String, Int>) {
        if (scores.isNotEmpty()) {
            val userPoints = scores.values.firstOrNull() ?: 0
            binding.tvPuntosJugador.text = userPoints.toString()
        }
    }

    /**
     * Configura los listeners de los botones
     */
    private fun setupButtonListeners() {
        binding.btnCamarotes.setOnClickListener {
            // TODO: Navegar a camarotes
        }
        
        binding.btnLabores.setOnClickListener {
            val userTasksFragment = UserTasksFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, userTasksFragment)
                .addToBackStack(null)
                .commit()
        }
        
        binding.btnReunirse.setOnClickListener {
            // TODO: Navegar a reunirse
        }
        
        binding.btnSoltarArmas.setOnClickListener {
            // Soltar armas = volver atrás / salir del juego
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

