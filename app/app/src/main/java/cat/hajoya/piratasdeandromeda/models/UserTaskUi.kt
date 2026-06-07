package cat.hajoya.piratasdeandromeda.models

data class UserTaskUi(
    val id: Long,
    val nombre: String,
    val descripcion: String = "",
    val completada: Boolean = false,
    val duracionEstimada: Int = 0, // en segundos
)