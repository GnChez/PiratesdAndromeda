package cat.hajoya.piratasdeandromeda.ui.preparacio

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cat.hajoya.piratasdeandromeda.data.network.ConnectedPlayer
import cat.hajoya.piratasdeandromeda.databinding.ItemPlayerStatusBinding

class PlayerLobbyAdapter : ListAdapter<ConnectedPlayer, PlayerLobbyAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemPlayerStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(player: ConnectedPlayer) {
            binding.tvPlayerName.text = player.playerName.ifBlank { player.nombreUsuario }
            binding.btnInfo.isEnabled = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlayerStatusBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ConnectedPlayer>() {
            override fun areItemsTheSame(a: ConnectedPlayer, b: ConnectedPlayer) =
                a.playerId == b.playerId
            override fun areContentsTheSame(a: ConnectedPlayer, b: ConnectedPlayer) =
                a == b
        }
    }
}