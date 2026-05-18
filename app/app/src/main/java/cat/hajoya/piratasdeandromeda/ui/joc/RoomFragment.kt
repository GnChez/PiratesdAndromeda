package cat.hajoya.piratasdeandromeda.ui.joc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.RoomsFrBinding
import cat.hajoya.piratasdeandromeda.models.Habitacio
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel

/**
 * Fragment de selección de sala (planetas/camarotes).
 * - Se abre desde MenuJuegoFragment al pulsar "Camarotes"
 * - Al seleccionar una sala, navega a UserTasksFragment con esa sala activa
 * - El botón X vuelve a este fragment (no al menu)
 */
class RoomFragment : Fragment() {

    private var _binding: RoomsFrBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        (requireActivity() as MainActivity).gameViewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = RoomsFrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCloseButton()
    }

    private fun setupRecyclerView() {
        val rooms = gameViewModel.habitacionsPartida.value ?: emptyList()

        val adapter = RoomSelectAdapter(rooms) { room ->
            navigateToUserTasks(room)
        }

        binding.rvRooms.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRooms.adapter = adapter
    }

    private fun navigateToUserTasks(room: Habitacio) {
        val fragment = UserTasksFragment.newInstance(roomId = room.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            // addToBackStack con tag de RoomFragment para que X vuelva aquí
            .addToBackStack(TAG)
            .commit()
    }

    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RoomFragment"
        fun newInstance() = RoomFragment()
    }
}