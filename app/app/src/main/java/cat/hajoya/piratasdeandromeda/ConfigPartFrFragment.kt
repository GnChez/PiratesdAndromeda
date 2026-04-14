package cat.hajoya.piratasdeandromeda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cat.hajoya.piratasdeandromeda.databinding.ConfigPartFrBinding

class ConfigPartFrFragment : Fragment() {

    private var _binding: ConfigPartFrBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels()

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

        viewModel.savedShips.value?.let(adapter::submitList)

        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnCloseSession.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCrear.setOnClickListener {
            val name = binding.edShipName.text?.toString().orEmpty()
            viewModel.addSavedShip(name)
            binding.edShipName.text?.clear()
        }

        binding.btnUnirme.setOnClickListener {
            val code = binding.edUnirseCode.text?.toString().orEmpty().trim()
            if (code.isNotEmpty()) {
                viewModel.addSavedShip("Código: $code")
                binding.edUnirseCode.text?.clear()
            }
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



