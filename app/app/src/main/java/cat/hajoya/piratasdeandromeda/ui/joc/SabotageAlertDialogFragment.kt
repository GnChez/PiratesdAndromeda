package cat.hajoya.piratasdeandromeda.ui.joc

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import cat.hajoya.piratasdeandromeda.databinding.DialogSabotageAlertBinding

/**
 * Dialog que se muestra a los jugadores que están haciendo un minijuego
 * cuando el impostor sabotea su sala. Dura 15 segundos y cierra solo.
 *
 * Uso:
 *   SabotageAlertDialogFragment.newInstance("Sala de control")
 *       .show(parentFragmentManager, SabotageAlertDialogFragment.TAG)
 */
class SabotageAlertDialogFragment : DialogFragment() {

    private var _binding: DialogSabotageAlertBinding? = null
    private val binding get() = _binding!!

    private var timer: CountDownTimer? = null
    private val SABOTAGE_DURATION_MS = 15_000L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogSabotageAlertBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // No cancelable por el usuario durante los 15 segundos
        isCancelable = false

        val roomName = arguments?.getString(ARG_ROOM_NAME)
        if (!roomName.isNullOrEmpty()) {
            binding.tvSabotageRoomName.text = roomName
            binding.tvSabotageRoomName.visibility = View.VISIBLE
        } else {
            binding.tvSabotageRoomName.visibility = View.GONE
        }

        startCountdown()
    }

    private fun startCountdown() {
        timer = object : CountDownTimer(SABOTAGE_DURATION_MS, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt() + 1
                binding.tvSabotageCountdown.text = seconds.toString()
            }

            override fun onFinish() {
                binding.tvSabotageCountdown.text = "0"
                dismiss()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        timer = null
        _binding = null
    }

    companion object {
        const val TAG = "SabotageAlertDialog"
        private const val ARG_ROOM_NAME = "room_name"

        fun newInstance(roomName: String? = null) = SabotageAlertDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ROOM_NAME, roomName)
            }
        }
    }
}