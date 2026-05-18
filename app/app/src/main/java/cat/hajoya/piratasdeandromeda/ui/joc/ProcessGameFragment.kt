package cat.hajoya.piratasdeandromeda.ui.joc

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ProcesGameFrBinding
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProcessGameFragment : Fragment() {

    private var _binding: ProcesGameFrBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels()

    private var taskId: Long = 0L
    private var taskName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId   = arguments?.getLong(ARG_TASK_ID, 0L) ?: 0L
        taskName = arguments?.getString(ARG_TASK_NAME, "") ?: ""
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

        setupCloseButton()
        setupWebView()

        gameViewModel.sendMissionStarted(taskId.toInt())

        lifecycleScope.launch {
            gameViewModel.playerScores.collect { scores ->
                updatePlayerScores(scores)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webViewGame.apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            addJavascriptInterface(GameBridge(), "AndroidBridge")
            loadUrl("file:///android_asset/game.html")
        }
    }

    inner class GameBridge {
        @JavascriptInterface
        fun onGameCompleted() {
            requireActivity().runOnUiThread {
                navigateToCompletedGame()
            }
        }
    }

    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun navigateToCompletedGame() {
        gameViewModel.sendMissionCompleted(taskId.toInt())

        lifecycleScope.launch {
            delay(500)
            val scores     = gameViewModel.playerScores.value
            val userPoints = scores.values.firstOrNull() ?: 0

            val completedFragment = CompletedGameFragment.newInstance(taskId, taskName, 0, userPoints)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, completedFragment)
                .commit()
        }
    }

    private fun updatePlayerScores(scores: Map<String, Int>) {
        binding.tvPuntosJugador.text = gameViewModel.getMyPoints(scores).toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TASK_ID   = "task_id"
        private const val ARG_TASK_NAME = "task_name"

        fun newInstance(taskId: Long, taskName: String) =
            ProcessGameFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_TASK_ID, taskId)
                    putString(ARG_TASK_NAME, taskName)
                }
            }
    }
}