package cat.hajoya.piratasdeandromeda.ui.joc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ExitGameFrBinding
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel

/**
 * Dialog de confirmación para abandonar la partida.
 * - "Seguir luchando" → dismiss
 * - "Abandonar" → leaveGame + navegar a MenuJuegoFragment
 */
class ExitGameDialogFragment : DialogFragment() {

    private var _binding: ExitGameFrBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ExitGameFrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStay.setOnClickListener {
            dismiss()
        }

        binding.btnExit.setOnClickListener {
            confirmExit()
        }
    }

    private fun confirmExit() {
        gameViewModel.leaveGame()

        // Volver al menú
        parentFragmentManager.popBackStack()

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ExitGameDialog"
        fun newInstance() = ExitGameDialogFragment()
    }
}