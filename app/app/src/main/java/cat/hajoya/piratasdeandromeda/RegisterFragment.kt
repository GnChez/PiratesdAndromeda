package cat.hajoya.piratasdeandromeda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cat.hajoya.piratasdeandromeda.databinding.IniciBinding
import cat.hajoya.piratasdeandromeda.databinding.RegisterBinding

class RegisterFragment: Fragment() {

    private var _binding: RegisterBinding? = null

    private val binding get()  = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RegisterBinding.inflate(inflater, container, false)
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
        binding.btnEntraReg.setOnClickListener {

        }

    }


}