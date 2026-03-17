package cat.hz.recunat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import cat.hz.recunat.databinding.NatacioRvBinding
import cat.hz.recunat.databinding.RvLstaBinding

class RVFragment: Fragment() {

    private var _binding: RvLstaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MyAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RvLstaBinding.inflate(inflater, container, false)
        return binding.root
    }


    private fun setupRecyclerView() {
        adapter = MyAdapter(
            items = emptyList()
        )
        binding.rvNataciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNataciones.adapter = adapter
    }





}