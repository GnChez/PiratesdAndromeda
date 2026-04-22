package cat.hajoya.piratasdeandromeda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cat.hajoya.piratasdeandromeda.databinding.ItemSavedShipBinding

class SavedShipAdapter(
    private val onDeleteClick: (SavedShip) -> Unit,
) : ListAdapter<SavedShip, SavedShipAdapter.SavedShipViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedShipViewHolder {
        val binding = ItemSavedShipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SavedShipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedShipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavedShipViewHolder(
        private val binding: ItemSavedShipBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SavedShip) {
            binding.tvShipName.text = item.name
            binding.btnDeleteShip.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SavedShip>() {
        override fun areItemsTheSame(oldItem: SavedShip, newItem: SavedShip): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SavedShip, newItem: SavedShip): Boolean =
            oldItem == newItem
    }
}

