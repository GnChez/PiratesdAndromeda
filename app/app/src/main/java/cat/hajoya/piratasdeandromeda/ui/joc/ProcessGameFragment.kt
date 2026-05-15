package cat.hajoya.piratasdeandromeda.ui.joc

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ProcesGameFrBinding
import cat.hajoya.piratasdeandromeda.ui.utils.ToastHelper.showMessage
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.launch

/**
 * Fragment que muestra el juego en progreso con un countdown timer.
 * Una vez termina el timer, navega automáticamente al fragment de finalización.
 * Integrado con WebSocket para enviar eventos de inicio y finalización de misión.
 */
class ProcessGameFragment : Fragment() {

    private var _binding: ProcesGameFrBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels()

    private var taskId: Long = 0L
    private var taskName: String = ""
    private var taskDuration: Int = 30 // segundos
    private var timeElapsed: Int = 0

    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getLong(ARG_TASK_ID, 0L) ?: 0L
        taskName = arguments?.getString(ARG_TASK_NAME, "") ?: ""
        taskDuration = arguments?.getInt(ARG_TASK_DURATION, 30) ?: 30
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ProcesGameFrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTareaNombre.text = taskName
        
         // Mostrar toast con código de juego y WebSocket al conectar
        lifecycleScope.launch {
            gameViewModel.wsGameCode.collect { gameCode ->
                if (gameCode != null && gameViewModel.wsCode.value != null) {
                    val wsCode = gameViewModel.wsCode.value!!
                    showNotification("Conexión: $gameCode | $wsCode")
                }
            }
        }
        
        // Escuchar notificaciones del WebSocket
        lifecycleScope.launch {
            gameViewModel.wsNotifications.collect { notification ->
                showNotification(notification)
            }
        }
        
        // Escuchar cambios en puntuaciones
        lifecycleScope.launch {
            gameViewModel.playerScores.collect { scores ->
                updatePlayerScores(scores)
            }
        }
        
        setupCloseButton()
        startTimer()
        
        // Enviar evento de inicio de misión al WebSocket
        gameViewModel.sendMissionStarted(taskId.toInt())
    }

    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            stopTimer()
            parentFragmentManager.popBackStack()
        }

    }

    private fun startTimer() {
        timer = object : CountDownTimer((taskDuration * 1000L), 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeElapsed++
            }

            override fun onFinish() {
                // Ir a la pantalla de completado
                navigateToCompletedGame()
            }
        }.start()
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun navigateToCompletedGame() {
        // Enviar evento de finalización de misión al WebSocket
        gameViewModel.sendMissionCompleted(taskId.toInt())
        
        // Obtener puntos del usuario actual desde el mapa de puntos del servidor
        val scores = gameViewModel.playerScores.value
        var userPoints = (taskDuration - timeElapsed) * 10  // Valor por defecto si no viene del servidor
        
        // Buscar los puntos en el mapa de scores
        scores.forEach { (_, score) ->
            userPoints = score  // Usar el score del servidor si está disponible
        }
        
        val completedFragment = CompletedGameFragment.newInstance(taskId, taskName, timeElapsed, userPoints)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, completedFragment)
            .commit()
    }

    private fun showNotification(message: String) {
        showMessage(requireContext(), message)
    }

    private fun updatePlayerScores(scores: Map<String, Int>) {
        if (scores.isNotEmpty()) {
            val userPoints = scores.values.firstOrNull() ?: 0
            binding.tvPuntosJugador.text = userPoints.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
        _binding = null
    }

    companion object {
        private const val ARG_TASK_ID = "task_id"
        private const val ARG_TASK_NAME = "task_name"
        private const val ARG_TASK_DURATION = "task_duration"

        fun newInstance(taskId: Long, taskName: String, duration: Int = 30) =
            ProcessGameFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_TASK_ID, taskId)
                    putString(ARG_TASK_NAME, taskName)
                    putInt(ARG_TASK_DURATION, duration)
                }
            }
    }
}



