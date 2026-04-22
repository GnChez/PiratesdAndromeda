package cat.hajoya.piratasdeandromeda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cat.hajoya.piratasdeandromeda.databinding.ItemRoomBinding

class RoomAdapter(
    private val onDeleteClick: (RoomItem) -> Unit,
) : ListAdapter<RoomItem, RoomAdapter.RoomViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemRoomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RoomViewHolder(
        private val binding: ItemRoomBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RoomItem) {
            binding.tvShipName.text = item.name
            binding.btnDeleteRoom.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RoomItem>() {
        override fun areItemsTheSame(oldItem: RoomItem, newItem: RoomItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RoomItem, newItem: RoomItem): Boolean =
            oldItem == newItem
    }
}

