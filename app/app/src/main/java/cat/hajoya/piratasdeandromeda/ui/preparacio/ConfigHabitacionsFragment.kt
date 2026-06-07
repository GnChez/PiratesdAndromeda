package cat.hajoya.piratasdeandromeda.ui.preparacio

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ConfigHabPartBinding
import cat.hajoya.piratasdeandromeda.RoomAdapter
import cat.hajoya.piratasdeandromeda.RoomItem
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class ConfigHabitacionsFragment : Fragment() {

    companion object {
        private const val TAG = "ConfigHabitacionsFragment"
    }

    private var _binding: ConfigHabPartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by activityViewModels {
        (requireActivity() as MainActivity).gameViewModelFactory
    }
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

        // Configurar RecyclerView
        binding.rvRooms.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRooms.adapter = adapter
        observeViewModel()

        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            // Desseleccionar la nave antes de volver
            viewModel.selectShip(null)
            parentFragmentManager.popBackStack()
        }

        binding.btnSiguiente.setOnClickListener {
            binding.btnSiguiente.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val result = viewModel.createGameFromSelectedShip()
                binding.btnSiguiente.isEnabled = true

                result
                    .onSuccess { gameCode ->
                        Log.i(TAG, "✅ Partida creada exitosamente: $gameCode")
                        // Esperar un poco para asegurar que WebSocket está conectado
                        kotlinx.coroutines.delay(500)
                        
                        val wsCode = viewModel.wsCode.value
                        if (wsCode != null) {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.game_ws_connection_success, gameCode),
                                Snackbar.LENGTH_LONG
                            ).show()
                            openPersonatgesScreen()
                        } else {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.game_ws_connection_warning),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "❌ Error al crear partida: ${error.message}", error)
                        Snackbar.make(
                            binding.root,
                            getString(R.string.game_error_game_creation, error.message ?: "Desconocido"),
                            Snackbar.LENGTH_LONG,
                        ).show()
                    }
            }
        }

        binding.btnAddRoom.setOnClickListener {
            val name = binding.edRoomName.text?.toString().orEmpty().trim()
            if (name.isEmpty()) {
                binding.edRoomName.error = getString(R.string.error_empty_room_name)
                binding.edRoomName.requestFocus()
                return@setOnClickListener
            }
            // Deshabilitar botón mientras se crea la habitación
            binding.btnAddRoom.isEnabled = false
            
            // Crear habitación y esperar a que se complete
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    viewModel.addRoom(name)
                    // Esperar el evento de creación
                    val roomId = viewModel.roomCreatedEvent.first()
                    // Habitación creada exitosamente
                    binding.edRoomName.text?.clear()
                    binding.btnAddRoom.isEnabled = true
                } catch (e: Exception) {
                    // Manejar error
                    binding.btnAddRoom.isEnabled = true
                    Snackbar.make(binding.root, getString(R.string.game_error_room_creation, e.message.orEmpty()), Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.rooms.collectLatest { rooms ->
                        adapter.submitList(rooms)
                        binding.tvEmptyRooms.isVisible = rooms.isEmpty()
                    }
                }
            }
        }
    }

    private fun openPersonatgesScreen() {
        parentFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
            replace(R.id.fragment_container, PersonatgesFragment())
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
            Snackbar.make(binding.root, getString(R.string.room_deleted_ok, room.name), Snackbar.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}



