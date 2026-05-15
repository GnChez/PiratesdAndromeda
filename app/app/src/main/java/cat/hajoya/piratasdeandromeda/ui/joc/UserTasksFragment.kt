package cat.hajoya.piratasdeandromeda.ui.joc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.UserTasksFragmentBinding
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.launch

/**
 * Fragment que muestra la lista de tareas del usuario.
 * Permite filtrar por estado (todos, pendientes, completados) y comenzar tareas.
 */
class UserTasksFragment : Fragment() {

    private var _binding: UserTasksFragmentBinding? = null
    private val binding get() = _binding!!
    
    private val gameViewModel: GameViewModel by activityViewModels()

    private val adapter = UserTaskAdapter(
        onStartTask = ::onStartTask,
        onTaskStatusChanged = ::onTaskStatusChanged,
    )

    // Lista de tareas (en un proyecto real, vendría del ViewModel/Repository)
    private val allTasks = listOf(
        UserTaskUi(1L, "Arregla luces", "Reparar el sistema de iluminación", false, 30),
        UserTaskUi(2L, "Limpia cubierta", "Barrer y limpiar la cubierta principal", false, 45),
        UserTaskUi(3L, "Repara velas", "Costura y reparación de velas dañadas", true, 60),
        UserTaskUi(4L, "Revisa barriles", "Inspecciona el estado de los barriles de agua", false, 20),
        UserTaskUi(5L, "Organiza bodega", "Ordena la carga en la bodega", true, 40),
        UserTaskUi(6L, "Pinta casco", "Retocar la pintura del casco", false, 50),
    )

    private var currentFilter = FilterType.TODOS
    private var modifiedTasks = mutableMapOf<Long, UserTaskUi>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = UserTasksFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabListener()
        setupCloseButton()
        updateFilteredTasks()
        
        // Escuchar cambios en puntuaciones
        lifecycleScope.launch {
            gameViewModel.playerScores.collect { scores ->
                updatePlayerScores(scores)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvTareas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTareas.adapter = adapter
    }

    private fun setupTabListener() {
        binding.tabsFiltro.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> FilterType.TODOS
                    1 -> FilterType.PENDIENTES
                    2 -> FilterType.TERMINADOS
                    else -> FilterType.TODOS
                }
                updateFilteredTasks()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

    }

    private fun updateFilteredTasks() {
        val getTasks = {
            val tasksToUse = allTasks.map { original ->
                modifiedTasks[original.id] ?: original
            }

            when (currentFilter) {
                FilterType.TODOS -> tasksToUse
                FilterType.PENDIENTES -> tasksToUse.filter { !it.completada }
                FilterType.TERMINADOS -> tasksToUse.filter { it.completada }
            }
        }

        val filteredTasks = getTasks()
        adapter.submitList(filteredTasks)
        binding.tvEmpty.visibility = if (filteredTasks.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onStartTask(task: UserTaskUi) {
        // Navegar a la pantalla de juego en proceso
        val gameFragment = ProcessGameFragment.newInstance(task.id, task.nombre, task.duracionEstimada)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, gameFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun onTaskStatusChanged(task: UserTaskUi, isCompleted: Boolean) {
        val updatedTask = task.copy(completada = isCompleted)
        modifiedTasks[task.id] = updatedTask
        updateFilteredTasks()
    }

    private fun updatePlayerScores(scores: Map<String, Int>) {
        if (scores.isNotEmpty()) {
            val userPoints = scores.values.firstOrNull() ?: 0
            binding.tvPuntosJugador.text = userPoints.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class FilterType {
        TODOS, PENDIENTES, TERMINADOS
    }

    companion object {
        fun newInstance() = UserTasksFragment()
    }
}




