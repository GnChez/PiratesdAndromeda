package cat.hz.provarecu

sealed class Categoria(val nom: String) {
    object Feina : Categoria("Feina")
    object Familia : Categoria("Família")
    object Personal : Categoria("Personal")
}
