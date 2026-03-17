package cat.hz.recunat

data class Natacion(
    val Nom: String,
    val Distancia: String,
    val Meta: String,
    val Fecha: String,
    val Hora: String,
    val id: Long = nextId()
){
    companion object{
        private var currentId = 0L

        fun nextId(): Long = ++currentId

        fun resetId(){
            currentId = 0L
        }

    }
}
