package cat.hajoya.piratasdeandromeda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cat.hajoya.piratasdeandromeda.databinding.ProvaIntroInfoBinding

/**
 * Fragment 1: Introduce nombre y descripción
 */
class InfoFragment : Fragment() {

    // ViewBinding - se infla automáticamente
    private var _binding: ProvaIntroInfoBinding? = null
    private val binding get() = _binding!!

    // ViewModel compartido con toda la Activity
    // activityViewModels() asegura que sea la misma instancia que en otros Fragments
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ProvaIntroInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar el botón "Siguiente"
        binding.btnSiguiente.setOnClickListener {
            val nombre = binding.etNom.text.toString().trim()
            val descripcion = binding.etDesc.text.toString().trim()

            // Validación simple
            if (nombre.isEmpty() || descripcion.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Por favor, completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Guardar en el ViewModel
            sharedViewModel.guardarDatos(nombre, descripcion)

            // Navegar al siguiente Fragment
            navegarADetalleFragment()
        }
    }

    /**
     * Navegación manual entre Fragments
     */
    private fun navegarADetalleFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DetalleFragment())
            .addToBackStack(null) // Permite volver atrás con el botón back
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evita memory leaks
    }
}