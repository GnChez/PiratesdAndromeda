package cat.hz.recunat

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import cat.hz.recunat.databinding.ActivityMainBinding


lateinit var rvNat: Fragment
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()
    }

    private fun initComponents(){
        rvNat = findViewById(R.id.rvNataciones)
        supportFragmentManager.beginTransaction()
            .replace(RVFragment())
            .commit()

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}