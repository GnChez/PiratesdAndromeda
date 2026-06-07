package cat.hajoya.piratasdeandromeda.data.network

import cat.hajoya.piratasdeandromeda.BuildConfig

/** Configuración base de red proveniente de BuildConfig. */
object NetworkConfig {
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    val baseHttpUrl: String = BuildConfig.BASE_HTTP_URL
    val baseWsUrl: String = BuildConfig.BASE_WS_URL
}

