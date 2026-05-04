package cat.hajoya.piratasdeandromeda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ItemSavedShipBinding

class SavedShipAdapter(
    private val onSelectClick: (SavedShip) -> Unit,
    private val onDeleteClick: (SavedShip) -> Unit,
) : ListAdapter<SavedShip, SavedShipAdapter.SavedShipViewHolder>(DiffCallback) {

    private var selectedId: Long? = null
    private var roomCounts: Map<Long, Int> = emptyMap()

    fun setSelectedId(id: Long?) {
        selectedId = id
        notifyDataSetChanged()
    }

    fun setRoomCounts(counts: Map<Long, Int>) {
        roomCounts = counts
        notifyDataSetChanged()
    }

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
            val count = roomCounts[item.id] ?: 0
            binding.tvRoomCount.text = binding.root.context.getString(R.string.ship_room_count_format, count)
            val isSelected = selectedId == item.id
            binding.cardSavedShip.strokeWidth = binding.root.resources.displayMetrics.density.let {
                if (isSelected) (2 * it).toInt() else (1 * it).toInt()
            }
            binding.cardSavedShip.strokeColor = binding.root.context.getColor(
                if (isSelected) R.color.dorado_claro else R.color.borde_boton,
            )

            binding.cardSavedShip.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSelectClick(getItem(position))
                }
            }
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

