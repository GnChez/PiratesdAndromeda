package cat.hajoya.piratasdeandromeda.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.ActivityMainBinding
import cat.hajoya.piratasdeandromeda.viewmodels.AdminViewModel

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adminViewModel: AdminViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adminViewModel = ViewModelProvider(this)[AdminViewModel::class.java]

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(
                    R.id.fragment_container,
                    createFragment("cat.hajoya.piratasdeandromeda.ui.admin.InicioAdminFragment")
                )
            }
        }
    }

    private fun createFragment(className: String): Fragment =
        Class.forName(className).getDeclaredConstructor().newInstance() as Fragment
}


