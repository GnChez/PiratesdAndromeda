package cat.hajoya.piratasdeandromeda.ui.joc

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cat.hajoya.piratasdeandromeda.databinding.ItemRoomBinding
import cat.hajoya.piratasdeandromeda.models.Habitacio

class RoomSelectAdapter(
    private val rooms: List<Habitacio>,
    private val onRoomClick: (Habitacio) -> Unit,
) : RecyclerView.Adapter<RoomSelectAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(private val binding: ItemRoomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(room: Habitacio) {
            binding.tvRoomName.text = room.nom
            binding.root.setOnClickListener { onRoomClick(room) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder =
        RoomViewHolder(ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) = holder.bind(rooms[position])

    override fun getItemCount() = rooms.size
}