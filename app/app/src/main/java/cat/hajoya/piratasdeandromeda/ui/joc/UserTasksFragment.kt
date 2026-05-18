package cat.hajoya.piratasdeandromeda.ui.joc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.UserTasksFragmentBinding
import cat.hajoya.piratasdeandromeda.models.RolJoc
import cat.hajoya.piratasdeandromeda.models.UserTaskUi
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel
import kotlinx.coroutines.launch

/**
 * Fragment que muestra la lista de tareas del usuario.
 *
 * Novedades respecto al original:
 * - Si el jugador es IMPOSTOR, se muestra un botón "Sabotear" que abre RoomSabotageDialogFragment
 * - Si viene de RoomFragment (roomId != null), el botón X vuelve a RoomFragment en lugar del menu
 * - Observa wsNotifications para mostrar SabotageAlertDialogFragment cuando llega un evento de sabotaje
 */
class UserTasksFragment : Fragment() {

    private var _binding: UserTasksFragmentBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by activityViewModels {
        (requireActivity() as MainActivity).gameViewModelFactory
    }

    private val adapter = UserTaskAdapter(
        onStartTask = ::onStartTask,
        onTaskStatusChanged = ::onTaskStatusChanged,
    )

    private val modifiedTasks = mutableMapOf<Long, UserTaskUi>()
    private var currentFilter = FilterType.TODOS
    private var roomId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = arguments?.getInt(ARG_ROOM_ID, -1).takeIf { it != -1 }
    }

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
        setupImpostorButton()
        observeViewModel()
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
                updateFilteredTasks(gameViewModel.myMissions.value)
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

    /**
     * Muestra el botón de sabotaje SOLO si el jugador es IMPOSTOR.
     * El rol viene de jugadorActual en el ViewModel.
     */
    private fun setupImpostorButton() {
        val jugador = gameViewModel.jugadorActual.value
        val esImpostor = jugador?.rol == RolJoc.IMPOSTOR

        // El layout user_tasks_fragment debe tener btnSabotear con visibility="gone" por defecto
        binding.btnSabotear?.let { btn ->
            btn.visibility = if (esImpostor) View.VISIBLE else View.GONE
            btn.setOnClickListener {
                RoomSabotageDialogFragment.newInstance()
                    .show(parentFragmentManager, RoomSabotageDialogFragment.TAG)
            }
        }

        // También observar cambios de rol en tiempo real (por si llega por WS)
        gameViewModel.jugadorActual.observe(viewLifecycleOwner) { jugadorActualizado ->
            val impostor = jugadorActualizado?.rol == RolJoc.IMPOSTOR
            binding.btnSabotear?.visibility = if (impostor) View.VISIBLE else View.GONE
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    gameViewModel.myMissions.collect { missions ->
                        updateFilteredTasks(missions)
                    }
                }

                launch {
                    gameViewModel.playerScores.collect { scores ->
                        updatePlayerScores(scores)
                    }
                }

                // Escuchar notificaciones de sabotaje desde WebSocket
                launch {
                    gameViewModel.wsNotifications.collect { notification ->
                        if (notification.contains("sabotage", ignoreCase = true) ||
                            notification.contains("sabotej", ignoreCase = true)) {
                            showSabotageAlert(notification)
                        }
                    }
                }
            }
        }
    }

    private fun showSabotageAlert(notification: String) {
        // Extraer nombre de sala si viene en la notificación, si no null
        val roomName = extractRoomName(notification)
        if (!isAdded || parentFragmentManager.isStateSaved) return

        SabotageAlertDialogFragment.newInstance(roomName)
            .show(parentFragmentManager, SabotageAlertDialogFragment.TAG)
    }

    private fun extractRoomName(notification: String): String? {
        // El formato esperado del WS podría ser "sabotage:NombreSala" o similar
        return if (notification.contains(":")) {
            notification.substringAfter(":").trim().ifEmpty { null }
        } else null
    }

    private fun updateFilteredTasks(missions: List<UserTaskUi>) {
        val tasksToUse = missions.map { modifiedTasks[it.id] ?: it }
        val filtered = when (currentFilter) {
            FilterType.TODOS -> tasksToUse
            FilterType.PENDIENTES -> tasksToUse.filter { !it.completada }
            FilterType.TERMINADOS -> tasksToUse.filter { it.completada }
        }
        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onStartTask(task: UserTaskUi) {
        val gameFragment = ProcessGameFragment.newInstance(task.id, task.nombre)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, gameFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun onTaskStatusChanged(task: UserTaskUi, isCompleted: Boolean) {
        modifiedTasks[task.id] = task.copy(completada = isCompleted)
        updateFilteredTasks(gameViewModel.myMissions.value)
    }

    private fun updatePlayerScores(scores: Map<String, Int>) {
        binding.tvPuntosJugador.text = gameViewModel.getMyPoints(scores).toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class FilterType {
        TODOS, PENDIENTES, TERMINADOS
    }

    companion object {
        private const val ARG_ROOM_ID = "room_id"

        /** Desde btnLabores del menu (sin sala seleccionada) */
        fun newInstance() = UserTasksFragment()

        /** Desde RoomFragment con sala seleccionada */
        fun newInstance(roomId: Int) = UserTasksFragment().apply {
            arguments = Bundle().apply { putInt(ARG_ROOM_ID, roomId) }
        }
    }
}