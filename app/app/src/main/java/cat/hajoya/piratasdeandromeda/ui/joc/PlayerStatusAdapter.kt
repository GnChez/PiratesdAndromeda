package cat.hajoya.piratasdeandromeda.ui.joc

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cat.hajoya.piratasdeandromeda.databinding.ItemPlayerStatusBinding

class PlayerStatusAdapter(
    private val onInfoClick: (PlayerStatusUi) -> Unit,
) : ListAdapter<PlayerStatusUi, PlayerStatusAdapter.PlayerStatusViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerStatusViewHolder {
        val binding = ItemPlayerStatusBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return PlayerStatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerStatusViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlayerStatusViewHolder(
        private val binding: ItemPlayerStatusBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlayerStatusUi) {
            binding.tvPlayerName.text = item.username
            binding.tvPercent.text = "${item.percent}%"
            binding.btnInfo.contentDescription = "Ver informacion de ${item.username}"

            binding.btnInfo.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onInfoClick(getItem(position))
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PlayerStatusUi>() {
        override fun areItemsTheSame(oldItem: PlayerStatusUi, newItem: PlayerStatusUi): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PlayerStatusUi, newItem: PlayerStatusUi): Boolean =
            oldItem == newItem
    }
}


