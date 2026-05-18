package cat.hajoya.piratasdeandromeda.ui.preparacio

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.PersonajesPartidaBinding
import cat.hajoya.piratasdeandromeda.ui.joc.MenuJuegoFragment
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PersonatgesFragment : Fragment() {

    companion object {
        private const val TAG = "PersonatgesFragment"
    }

    private var _binding: PersonajesPartidaBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        (requireActivity() as MainActivity).gameViewModelFactory
    }

    private val playerAdapter = PlayerLobbyAdapter()

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

        binding.rvPlayers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlayers.adapter = playerAdapter

        observeViewModel()
        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnEmpezar.setOnClickListener { iniciarPartida() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    gameViewModel.wsGameCode.collect { gameCode ->
                        if (gameCode != null) binding.btnPartidaCode.text = gameCode
                    }
                }

                launch {
                    gameViewModel.connectedPlayers.collect { players ->
                        playerAdapter.submitList(players.toList())
                        Log.i(TAG, "Lista actualizada: ${players.size} → ${players.map { it.playerName }}")
                    }
                }

                launch {
                    val userId = gameViewModel.idCreador.first { it != null }
                    binding.btnEmpezar.isEnabled = userId != null
                    if (userId == null) {
                        Snackbar.make(binding.root, "⏳ Esperando a que el anfitrión comience...", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun iniciarPartida() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (gameViewModel.idCreador.value == null) {
                Snackbar.make(binding.root, "❌ Solo el anfitrión puede comenzar", Snackbar.LENGTH_SHORT).show()
                return@launch
            }
            binding.btnEmpezar.isEnabled = false
            try {
                gameViewModel.iniciarPartidaPorWebSocket()
                kotlinx.coroutines.delay(500)
                openMenuJuegoScreen()
            } catch (e: Exception) {
                binding.btnEmpezar.isEnabled = true
                Snackbar.make(binding.root, "❌ Error al iniciar partida", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun openMenuJuegoScreen() {
        parentFragmentManager.commit {
            setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            replace(R.id.fragment_container, MenuJuegoFragment())
            addToBackStack(null)
        }
    }
}