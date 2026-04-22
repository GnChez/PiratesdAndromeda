package cat.hajoya.piratasdeandromeda.models

enum class Personaje {
    CORSARIO, LOBO_MAR, BUCANERO
}

enum class RolJoc {
    TRIPULANT, IMPOSTOR, FANTASMA
}

enum class EstatMissio {
    PENDENT, EN_PROGRES, COMPLETADA, SABOTEJADA
}

enum class TipusHabitacio {
    SALA_CONTROL, ARMERIA, COMUNICACIONES, MEDICA,
    ALMACEN, SALA_ESPIRITUS, OTRA
}

enum class EstatPartida {
    ESPERANT_JUGADORS, EN_CURS, FINALITZADA
}

enum class Dificultad {
    FACIL, MITJA, DIFICIL
}

data class JugadorPartida(
    val id: Int,
    val userId: Int,
    val apodoPartida: String,
    val rol: RolJoc,
    val viu: Boolean,
    val missionsCompletades: Int,
    val punts: Int,
    val personatge: Personaje
)

data class Missio(
    val id: Int,
    val nom: String,
    val descripcio: String,
    val habitacioId: Int,
    val tempsEstimatSegons: Int,
    val percentatgeReparacio: Float,
    val estat: EstatMissio
)

data class Habitacio(
    val id: Int,
    val nom: String,
    val tipus: TipusHabitacio
)

data class Partida(
    val id: Int,
    val codiPartida: String,
    val nomPartida: String?,
    val presencial: Boolean,
    val estatPartida: EstatPartida,
    val numJugadors: Int,
    val numImpostors: Int,
    val tempsLimitMinuts: Int,
    val percentatgeReparacio: Float,
    val dificultad: Dificultad
)

data class ConfigPartida(
    val nomPartida: String,
    val habitacions: List<Int>,
    val numImpostors: Int,
    val tempsLimitMinuts: Int,
    val dificultad: Dificultad
)

