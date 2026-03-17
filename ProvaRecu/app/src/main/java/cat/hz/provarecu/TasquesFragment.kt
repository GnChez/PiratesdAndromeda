package cat.hz.provarecu

import cat.hz.provarecu.TasquesAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip

class TasquesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private lateinit var toolbar: Toolbar
    private val adapter = TasquesAdapter()
    private var categoriaSeleccionada: Categoria? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tasques, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        recyclerView = view.findViewById(R.id.recyclerView)
        chipGroup = view.findViewById(R.id.chipGroup)

        setupRecyclerView()
        setupChips()
        setupToolbarBehavior()

        // Carregar totes les tasques inicialment
        actualitzarTasques()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupChips() {
        val chipTotes: Chip = view?.findViewById(R.id.chipTotes) ?: return
        val chipFeina: Chip = view?.findViewById(R.id.chipFeina) ?: return
        val chipFamilia: Chip = view?.findViewById(R.id.chipFamilia) ?: return
        val chipPersonal: Chip = view?.findViewById(R.id.chipPersonal) ?: return

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            categoriaSeleccionada = when {
                checkedIds.contains(R.id.chipFeina) -> Categoria.Feina
                checkedIds.contains(R.id.chipFamilia) -> Categoria.Familia
                checkedIds.contains(R.id.chipPersonal) -> Categoria.Personal
                else -> null
            }
            actualitzarTasques()
        }
    }

    private fun setupToolbarBehavior() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) {
                    toolbar.animate()
                        .translationY(-toolbar.height.toFloat())
                        .setDuration(200)
                        .start()
                } else if (dy < 0) {
                    toolbar.animate()
                        .translationY(0f)
                        .setDuration(200)
                        .start()
                }
            }
        })
    }

    private fun actualitzarTasques() {
        val tasquesFiltrades = if (categoriaSeleccionada != null) {
            TasquesRepository.tasques.filter {
                it.categoria::class == categoriaSeleccionada!!::class
            }
        } else {
            TasquesRepository.tasques
        }
        adapter.setTasques(tasquesFiltrades)
    }
}
