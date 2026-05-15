package cat.hajoya.piratasdeandromeda.ui.utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * Helper para mostrar notificaciones toast temporales con estilo de la app.
 */
object ToastHelper {
    
    fun showMessage(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
    
    fun Fragment.showMessage(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), message, duration).show()
    }
    
    fun showLong(context: Context, message: String) {
        showMessage(context, message, Toast.LENGTH_LONG)
    }
    
    fun Fragment.showMessageLong(message: String) {
        showMessage(message, Toast.LENGTH_LONG)
    }
}

