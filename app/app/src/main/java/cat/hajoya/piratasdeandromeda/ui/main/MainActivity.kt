package cat.hajoya.piratasdeandromeda.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ActivityMainBinding
import cat.hajoya.piratasdeandromeda.ui.preparacio.StartPartidaFragment
import cat.hajoya.piratasdeandromeda.viewmodels.GameViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gameViewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gameViewModel = ViewModelProvider(this).get(GameViewModel::class.java)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, StartPartidaFragment())
            }
        }
    }
}

