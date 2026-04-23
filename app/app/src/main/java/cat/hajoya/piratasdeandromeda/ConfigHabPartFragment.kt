package cat.hajoya.piratasdeandromeda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import cat.hajoya.piratasdeandromeda.databinding.ConfigHabPartBinding

class ConfigHabPartFragment : Fragment() {

    private var _binding: ConfigHabPartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels()
    private val adapter = RoomAdapter(::confirmDeleteRoom)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ConfigHabPartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvRooms.adapter = adapter

        viewModel.rooms.observe(viewLifecycleOwner) { rooms ->
            adapter.submitList(rooms)
        }
        viewModel.rooms.value?.let(adapter::submitList)

        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSiguiente.setOnClickListener {
            openCharactersScreen()
        }

        binding.btnAddRoom.setOnClickListener {
            val name = binding.edRoomName.text?.toString().orEmpty()
            viewModel.addRoom(name)
            binding.edRoomName.text?.clear()
        }
    }

    private fun openCharactersScreen() {
        parentFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
            replace(R.id.fragment_container, PersonajesPartidaFragment())
            addToBackStack(null)
        }
    }


    private fun confirmDeleteRoom(room: RoomItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_ship, null, false)
        dialogView.findViewById<android.widget.TextView>(R.id.tvDeleteDialogMessage).text =
            getString(R.string.delete_ship_dialog_message, room.name)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())

        dialogView.findViewById<android.widget.Button>(R.id.btnDialogCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.btnDialogConfirm).setOnClickListener {
            viewModel.deleteRoom(room.id)
            dialog.dismiss()
        }

        dialog.show()
    }
}


