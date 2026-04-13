package cat.hajoya.piratasdeandromeda

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import cat.hajoya.piratasdeandromeda.databinding.IniciBinding
import kotlin.getValue


class IniciFragment: Fragment() {
    private var _binding: IniciBinding? = null

    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = IniciBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeViewModel() {

    }

    private fun setupListeners(){
        binding.tvSignUp.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnEntra.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StartFrFragment())
                .addToBackStack(null)
                .commit()
        }

    }
}