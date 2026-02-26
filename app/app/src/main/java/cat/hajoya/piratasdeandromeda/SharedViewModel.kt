package cat.hajoya.piratasdeandromeda

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel compartido entre los Fragments de la Activity.
 * Al usar activityViewModels() en los Fragments, todos comparten esta misma instancia.
 */
class SharedViewModel : ViewModel() {

    // MutableLiveData - privada, solo modificable desde dentro del ViewModel
    private val _nombre = MutableLiveData<String>()
    private val _descripcion = MutableLiveData<String>()

    // LiveData pública - solo lectura para los Fragments
    val nombre: LiveData<String> = _nombre
    val descripcion: LiveData<String> = _descripcion

    /**
     * Guarda los datos introducidos por el usuario
     */
    fun guardarDatos(nuevoNombre: String, nuevaDescripcion: String) {
        _nombre.value = nuevoNombre
        _descripcion.value = nuevaDescripcion
    }

    /**
     * Limpia los datos (útil si quieres resetear)
     */
    fun limpiarDatos() {
        _nombre.value = ""
        _descripcion.value = ""
    }
}