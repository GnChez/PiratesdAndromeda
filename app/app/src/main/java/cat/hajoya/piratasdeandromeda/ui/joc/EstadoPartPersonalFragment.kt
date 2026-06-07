package cat.hajoya.piratasdeandromeda.ui.joc

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.EstadoPartPersonalBinding
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.launch

class EstadoPartPersonalFragment : Fragment() {

    private var _binding: EstadoPartPersonalBinding? = null
    private val binding get() = _binding!!

    private val adapter = PlayerStatusAdapter(::showPlayerInfoDialog)
    private val viewModel: GameViewModel by activityViewModels {
        val activity = requireActivity()
        // Soportar MainActivity y AdminActivity
        (activity as? MainActivity)?.gameViewModelFactory 
            ?: (activity as? cat.hajoya.piratasdeandromeda.ui.admin.AdminActivity)?.gameViewModelFactory
            ?: throw IllegalStateException("Activity debe ser MainActivity o AdminActivity")
    }

    private var gameCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameCode = arguments?.getString(ARG_GAME_CODE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = EstadoPartPersonalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Conectar al monitor si tenemos código
        gameCode?.let { code ->
            viewModel.connectMonitor(code)
        }

        setupRecyclerView()
        observeViewModel()
        setupStaticUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.rvPlayersProgress.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlayersProgress.adapter = adapter
    }

    private fun observeViewModel() {
        // Observar jugadores conectados desde el monitor usando StateFlow
        lifecycleScope.launch {
            viewModel.connectedPlayers.collect { players ->
                val playerStatusList = players.map { player ->
                    PlayerStatusUi(
                        id = player.playerId.toLongOrNull() ?: 0L,
                        username = player.nombreUsuario,
                        email = player.playerName,
                        percent = (player.puntos / 100).coerceIn(0, 100)
                    )
                }
                adapter.submitList(playerStatusList)
            }
        }

        viewModel.partidaActual.observe(viewLifecycleOwner) { partida ->
            val code = partida?.codiPartida ?: gameCode ?: "-"
            binding.tvGameCode.text = getString(R.string.game_code_value_format, code)
        }
    }

    private fun setupStaticUi() {
        binding.tvTimeRemaining.text = "Tiempo restante: 09:42"

        binding.btnClose.setOnClickListener { 
            parentFragmentManager.popBackStack() 
        }
        binding.btnMenu.setOnClickListener { }
    }

    private fun showPlayerInfoDialog(player: PlayerStatusUi) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_player_info, null, false)

        val ivProfile = dialogView.findViewById<ImageView>(R.id.ivPlayerProfile)
        val tvName = dialogView.findViewById<TextView>(R.id.tvPlayerInfoName)
        val tvEmail = dialogView.findViewById<TextView>(R.id.tvPlayerInfoEmail)
        val btnCloseDialog = dialogView.findViewById<Button>(R.id.btnClosePlayerInfo)

        ivProfile.contentDescription = "Imagen de perfil de ${player.username}"
        tvName.text = "Nombre: ${player.username}"
        tvEmail.text = "Correo: ${player.email}"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        // Cerrar al clickear fuera
        dialog.setCanceledOnTouchOutside(true)

        btnCloseDialog.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    companion object {
        private const val ARG_GAME_CODE = "game_code"

        fun newInstance(gameCode: String): EstadoPartPersonalFragment {
            return EstadoPartPersonalFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_GAME_CODE, gameCode)
                }
            }
        }
    }
}


