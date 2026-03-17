package cat.hz.provarecu

data class Tasca(
    val id: Int,
    val nom: String,
    val categoria: Categoria,
    val data: String,
    val estat: Estat
)
