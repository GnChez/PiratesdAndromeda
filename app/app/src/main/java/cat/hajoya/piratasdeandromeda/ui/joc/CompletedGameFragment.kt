package cat.hajoya.piratasdeandromeda.ui.joc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.CompletedGameFrBinding
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.launch

/**
 * Fragment que muestra la pantalla de resultado cuando se completa una tarea.
 * Muestra el nombre de la tarea, tiempo empleado y puntuación obtenida del servidor.
 */
class CompletedGameFragment : Fragment() {

    private var _binding: CompletedGameFrBinding? = null
    private val binding get() = _binding!!
    
    private val gameViewModel: GameViewModel by activityViewModels()

    private var taskId: Long = 0L
    private var taskName: String = ""
    private var timeElapsed: Int = 0
    private var points: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getLong(ARG_TASK_ID, 0L) ?: 0L
        taskName = arguments?.getString(ARG_TASK_NAME, "") ?: ""
        timeElapsed = arguments?.getInt(ARG_TIME_ELAPSED, 0) ?: 0
        points = arguments?.getInt(ARG_POINTS, 0) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = CompletedGameFrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupCloseButton()
        
        // Escuchar cambios en puntuaciones
        lifecycleScope.launch {
            gameViewModel.playerScores.collect { scores ->
                updatePlayerScores(scores)
            }
        }
    }

    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            navigateToUserTasks()
        }

    }

    private fun setupUI() {
        binding.tvTareaNombre.text = taskName
        
        // Formato: MM:SS (minutos:segundos)
        val minutes = timeElapsed / 60
        val seconds = timeElapsed % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        binding.tvTiempo.text = "Temps: $timeString"
        
        binding.tvPuntos.text = "Punts: $points"
    }


    private fun navigateToUserTasks() {
        val userTasksFragment = UserTasksFragment.newInstance()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, userTasksFragment)
            .commit()
    }

    private fun updatePlayerScores(scores: Map<String, Int>) {
        if (scores.isNotEmpty()) {
            val userPoints = scores.values.firstOrNull() ?: 0
            binding.tvPuntosJugador.text = userPoints.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TASK_ID = "task_id"
        private const val ARG_TASK_NAME = "task_name"
        private const val ARG_TIME_ELAPSED = "time_elapsed"
        private const val ARG_POINTS = "points"

        fun newInstance(taskId: Long, taskName: String, timeElapsed: Int, points: Int) =
            CompletedGameFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_TASK_ID, taskId)
                    putString(ARG_TASK_NAME, taskName)
                    putInt(ARG_TIME_ELAPSED, timeElapsed)
                    putInt(ARG_POINTS, points)
                }
            }
    }
}



