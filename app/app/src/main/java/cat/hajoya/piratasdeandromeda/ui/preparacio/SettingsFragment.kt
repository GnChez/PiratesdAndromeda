package cat.hajoya.piratasdeandromeda.ui.preparacio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import cat.hajoya.piratasdeandromeda.R
import cat.hajoya.piratasdeandromeda.databinding.SettingsFrBinding
import cat.hajoya.piratasdeandromeda.ui.auth.AuthActivity
import cat.hajoya.piratasdeandromeda.ui.main.MainActivity
import cat.hajoya.piratasdeandromeda.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.appcompat.app.AppCompatDelegate

class SettingsFragment : Fragment() {

    private var _binding: SettingsFrBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by activityViewModels {
        (requireActivity() as MainActivity).settingsViewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SettingsFrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSaveUsername.setOnClickListener {
            viewModel.saveUsername(binding.edUsername.text.toString())
            Snackbar.make(binding.root, getString(R.string.settings_saved_ok), Snackbar.LENGTH_SHORT).show()
        }

        binding.btnSaveEmail.setOnClickListener {
            viewModel.saveEmail(binding.edEmail.text.toString())
            Snackbar.make(binding.root, getString(R.string.settings_saved_ok), Snackbar.LENGTH_SHORT).show()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveDarkMode(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO,
            )
        }

        binding.btnDeleteAccount.setOnClickListener {
            viewModel.logout {
                val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.username.collectLatest { binding.edUsername.setTextIfChanged(it) }
                }
                launch {
                    viewModel.email.collectLatest { binding.edEmail.setTextIfChanged(it) }
                }
                launch {
                    viewModel.darkModeEnabled.collectLatest { isChecked ->
                        if (binding.switchDarkMode.isChecked != isChecked) {
                            binding.switchDarkMode.isChecked = isChecked
                        }
                    }
                }
            }
        }
    }

    private fun android.widget.EditText.setTextIfChanged(newValue: String) {
        if (text?.toString() != newValue) {
            setText(newValue)
        }
    }
}

