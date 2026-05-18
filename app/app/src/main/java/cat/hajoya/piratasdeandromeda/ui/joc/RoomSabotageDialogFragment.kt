package cat.hajoya.piratasdeandromeda.ui.joc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cat.hajoya.piratasdeandromeda.databinding.DialogRoomSabotageBinding
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel

class RoomSabotageDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogRoomSabotageBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        (requireActivity() as MainActivity).gameViewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogRoomSabotageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rooms = gameViewModel.habitacionsPartida.value ?: emptyList()

        binding.rvSabotageRooms.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSabotageRooms.adapter = RoomSelectAdapter(rooms) { room ->
            gameViewModel.sabotearHabitacio(room.id)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RoomSabotageDialog"
        fun newInstance() = RoomSabotageDialogFragment()
    }
}