package cat.hz.recunat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyViewHolder(
    itemView: View
    ): RecyclerView.ViewHolder(itemView){
        private var tvNom: String = itemView.findViewById(R.id.tvNom)
        private var tvDist: String = itemView.findViewById(R.id.tvDistancia)
        private var tvMeta: String = itemView.findViewById(R.id.tvMeta)
        private var tvFecha: String = itemView.findViewById(R.id.tvFecha)
        private var tvHora: String = itemView.findViewById(R.id.tvHora)
        private val btnDelete : Button = itemView.findViewById(R.id.btnDelete)


    fun bind ( item: Natacion){
        tvNom = item.Nom
        tvDist = item.Distancia
        tvMeta = item.Meta
        tvFecha = item.Fecha
        tvHora = item.Hora

 }
    }





class MyAdapter(
    private val items : List<Natacion>
) : RecyclerView.Adapter<MyViewHolder>(){

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: cat.hz.recunat.MyViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): cat.hz.recunat.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.rv_lsta, parent, false)
        return MyViewHolder(view)
    }



}