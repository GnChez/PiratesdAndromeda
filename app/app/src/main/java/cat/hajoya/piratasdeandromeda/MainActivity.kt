package cat.hajoya.piratasdeandromeda

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cat.hajoya.piratasdeandromeda.databinding.ActivityMainBinding
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, IniciFragment())
                .commit()
        }
    }
}