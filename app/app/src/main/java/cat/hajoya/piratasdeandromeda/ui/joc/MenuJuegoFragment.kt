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
import cat.hajoya.piratasdeandromeda.models.RolJoc
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.launch

/**
 * Fragment que muestra el menú principal del juego.
 *
 * - CAMAROTES → RoomFragment (lista de planetas/salas para elegir)
 * - LABORES   → UserTasksFragment (tareas del jugador, sin sala preseleccionada)
 * - REUNIRSE  → (TODO)
 * - SOLTAR ARMAS → ExitGameDialogFragment (confirmación para abandonar)
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

        setupTitle()

        lifecycleScope.launch {
            gameViewModel.playerScores.collect { scores ->
                updatePlayerScores(scores)
            }
        }

        setupButtonListeners()
    }

    private fun setupTitle() {
        // Obtener el nombre del usuario actual del ViewModel
        val username = gameViewModel.usuarioActual.value ?: "Corsario"
        val jugador = gameViewModel.jugadorActual.value
        val rol = if (jugador?.rol == RolJoc.IMPOSTOR) "impostor" else "tripulante"
        
        val fullText = "$username, ¡eres $rol en esta nave!"

        val spannableString = SpannableString(fullText)

        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            0, username.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.blanco)),
            0, username.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.anaranjado)),
            username.length, fullText.length,
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

    private fun setupButtonListeners() {
        // CAMAROTES → lista de salas para elegir antes de ver tareas
        binding.btnCamarotes.setOnClickListener {
            val roomFragment = RoomFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, roomFragment)
                .addToBackStack(null)
                .commit()
        }

        // LABORES → tareas directamente (sin selección de sala)
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

        // SOLTAR ARMAS → dialog de confirmación de salida
        binding.btnSoltarArmas.setOnClickListener {
            ExitGameDialogFragment.newInstance()
                .show(parentFragmentManager, ExitGameDialogFragment.TAG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}