package cat.hajoya.piratasdeandromeda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cat.hajoya.piratasdeandromeda.databinding.ProvaShowInfoBinding

/**
 * Fragment 2: Muestra el nombre y descripción guardados
 */
class DetalleFragment : Fragment() {

    private var _binding: ProvaShowInfoBinding? = null
    private val binding get() = _binding!!

    // Mismo ViewModel que InfoFragment (gracias a activityViewModels)
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ProvaShowInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.nombre.observe(viewLifecycleOwner) { nombre ->
            binding.showNom.text = "Nombre: $nombre"
        }

        sharedViewModel.descripcion.observe(viewLifecycleOwner) { descripcion ->
            binding.showDesc.text = "Descripción: $descripcion"
        }


        binding.btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}