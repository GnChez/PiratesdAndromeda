package cat.hajoya.piratasdeandromeda.ui.preparacio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ConfigPartFrBinding
import cat.hajoya.piratasdeandromeda.SavedShip
import cat.hajoya.piratasdeandromeda.SavedShipAdapter
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel

class StartPartidaFragment : Fragment() {

    private var _binding: ConfigPartFrBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by activityViewModels()
    private val adapter = SavedShipAdapter(::confirmDeleteShip)

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

        binding.rvSavedShips.adapter = adapter
        viewModel.savedShips.observe(viewLifecycleOwner) { ships ->
            adapter.submitList(ships)
        }

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
            val shipName = binding.edShipName.text?.toString().orEmpty()
            viewModel.addSavedShip(shipName)
            binding.edShipName.text?.clear()
            openConfigHabitacionsScreen()
        }

        binding.btnUnirme.setOnClickListener {
            openPersonatgesScreen()
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
            dialog.dismiss()
        }

        dialog.show()
    }
}



