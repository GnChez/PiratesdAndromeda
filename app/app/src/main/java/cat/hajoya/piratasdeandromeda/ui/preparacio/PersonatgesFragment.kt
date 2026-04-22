package cat.hajoya.piratasdeandromeda.ui.preparacio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cat.hajoya.piratasdeandromeda.databinding.PersonajesPartidaBinding
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel

class PersonatgesFragment : Fragment() {

    private var _binding: PersonajesPartidaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by activityViewModels()

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

        binding.btnEmpezar.setOnClickListener {
            viewModel.iniciarPartida()
        }
    }
}


