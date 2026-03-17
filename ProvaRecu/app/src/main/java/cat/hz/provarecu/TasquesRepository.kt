package cat.hz.provarecu

object TasquesRepository {
    val tasques = listOf(
        Tasca(1, "Preparar presentació", Categoria.Feina, "25/01/2026", Estat.EnCurs),
        Tasca(2, "Revisar emails", Categoria.Feina, "25/01/2026", Estat.NoComencada),
        Tasca(3, "Sopar familiar", Categoria.Familia, "26/01/2026", Estat.NoComencada),
        Tasca(4, "Comprar regal aniversari", Categoria.Familia, "28/01/2026", Estat.EnCurs),
        Tasca(5, "Anar al gimnàs", Categoria.Personal, "25/01/2026", Estat.Finalitzada),
        Tasca(6, "Llegir llibre", Categoria.Personal, "27/01/2026", Estat.EnCurs),
        Tasca(7, "Reunió equip", Categoria.Feina, "26/01/2026", Estat.NoComencada),
        Tasca(8, "Trucar mare", Categoria.Familia, "25/01/2026", Estat.Finalitzada)
    )
}
