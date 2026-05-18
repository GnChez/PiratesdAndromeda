package cat.hajoya.piratasdeandromeda.ui.joc

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cat.hajoya.piratasdeandromeda.databinding.ItemUserTaskBinding
import cat.hajoya.piratasdeandromeda.models.UserTaskUi

class UserTaskAdapter(
    private val onStartTask: (UserTaskUi) -> Unit,
    private val onTaskStatusChanged: (UserTaskUi, Boolean) -> Unit,
) : ListAdapter<UserTaskUi, UserTaskAdapter.UserTaskViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserTaskViewHolder {
        val binding = ItemUserTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return UserTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserTaskViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class UserTaskViewHolder(
        private val binding: ItemUserTaskBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserTaskUi, position: Int) {
            binding.tvTareaNombre.text = item.nombre
            binding.cbTareaCompletada.isChecked = item.completada
            binding.cbTareaCompletada.contentDescription = "Marcar ${item.nombre} como completada"

            binding.cbTareaCompletada.setOnCheckedChangeListener { _, isChecked ->
                if (position != RecyclerView.NO_POSITION) {
                    onTaskStatusChanged(getItem(position), isChecked)
                }
            }

            binding.btnComenzarTarea.setOnClickListener {
                if (position != RecyclerView.NO_POSITION) {
                    onStartTask(getItem(position))
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<UserTaskUi>() {
        override fun areItemsTheSame(oldItem: UserTaskUi, newItem: UserTaskUi): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: UserTaskUi, newItem: UserTaskUi): Boolean =
            oldItem == newItem
    }
}

