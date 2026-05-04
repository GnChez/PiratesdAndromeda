package cat.hajoya.piratasdeandromeda.ui.preparacio

import android.os.Bundle
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
import cat.hajoya.piratasdeandromeda.databinding.ConfigPartFrBinding
import cat.hajoya.piratasdeandromeda.SavedShip
import cat.hajoya.piratasdeandromeda.SavedShipAdapter
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class StartPartidaFragment : Fragment() {

    private var _binding: ConfigPartFrBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by activityViewModels {
        (requireActivity() as MainActivity).gameViewModelFactory
    }
    private val adapter = SavedShipAdapter({ ship -> viewModel.selectShip(ship.id) }, ::confirmDeleteShip)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ConfigPartFrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView
        binding.rvSavedShips.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSavedShips.adapter = adapter
        observeViewModel()

        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnCloseSession.setOnClickListener {
            openSettingsScreen()
        }

        binding.btnCancel.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnCrear.setOnClickListener {
            val shipName = binding.edShipName.text?.toString().orEmpty().trim()
            if (shipName.isEmpty()) {
                binding.edShipName.error = getString(R.string.error_empty_ship_name)
                binding.edShipName.requestFocus()
                return@setOnClickListener
            }
            if (shipName.length > 50) {
                binding.edShipName.error = getString(R.string.error_ship_name_too_long)
                return@setOnClickListener
            }
            // Deshabilitar botón mientras se crea la nave
            binding.btnCrear.isEnabled = false
            
            // Crear nave y esperar a que se complete
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    viewModel.addSavedShip(shipName)
                    // Esperar el evento de creación
                    val shipId = viewModel.shipCreatedEvent.first()
                    // Nave creada exitosamente, ahora navegar
                    binding.edShipName.text?.clear()
                    openConfigHabitacionsScreen()
                } catch (e: Exception) {
                    // Manejar error
                    binding.btnCrear.isEnabled = true
                    Snackbar.make(binding.root, "Error al crear nave: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnUnirme.setOnClickListener {
            openPersonatgesScreen()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.savedShips.collectLatest { ships ->
                        adapter.submitList(ships)
                        binding.root.findViewById<View>(R.id.tvEmptyShips).isVisible = ships.isEmpty()
                    }
                }
                launch {
                    viewModel.selectedShipId.collectLatest { selectedId ->
                        adapter.setSelectedId(selectedId)
                        // Navegar a habitaciones si se selecciona una nave
                        if (selectedId != null) {
                            openConfigHabitacionsScreen()
                        }
                    }
                }
                launch {
                    viewModel.allRooms.collectLatest { rooms ->
                        val roomCounts = rooms.groupingBy { it.shipId }.eachCount().mapValues { it.value }
                        adapter.setRoomCounts(roomCounts)
                    }
                }
            }
        }
    }

    private fun openSettingsScreen() {
        parentFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
            replace(R.id.fragment_container, SettingsFragment())
            addToBackStack(null)
        }
    }

    private fun openConfigHabitacionsScreen() {
        parentFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
            replace(R.id.fragment_container, ConfigHabitacionsFragment())
            addToBackStack(null)
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

    private fun confirmDeleteShip(ship: SavedShip) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_ship, null, false)
        dialogView.findViewById<android.widget.TextView>(R.id.tvDeleteDialogMessage).text =
            getString(R.string.delete_ship_dialog_message, ship.name)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())

        dialogView.findViewById<android.widget.Button>(R.id.btnDialogCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.btnDialogConfirm).setOnClickListener {
            viewModel.deleteSavedShip(ship.id)
            Snackbar.make(binding.root, getString(R.string.ship_deleted_ok, ship.name), Snackbar.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}



