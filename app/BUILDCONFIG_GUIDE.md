# 🔗 Guía de Uso - BuildConfig y URLs de API

## ¿Qué es BuildConfig?

`BuildConfig` es una clase generada automáticamente por Android Gradle que contiene valores compilados en tiempo de build.

En nuestro caso, contiene las URLs de la API diferenciadas por buildType.

---

## 📋 Valores Disponibles

Después de compilar, tienes acceso a:

```kotlin
// En Debug (desarrollo)
BuildConfig.BASE_HTTP_URL  // → "http://10.0.2.2:8000/"
BuildConfig.BASE_WS_URL    // → "ws://10.0.2.2:8000"

// En Release (producción)
BuildConfig.BASE_HTTP_URL  // → "https://api.piratasandromeda.me/"
BuildConfig.BASE_WS_URL    // → "wss://api.piratasandromeda.me"
```

---

## 💻 Cómo Usarlo en el Código

### Opción 1: En Retrofit (Recomendado)

```kotlin
// data/network/RetrofitClient.kt
import cat.hajoya.piratasdeandromeda.BuildConfig

object RetrofitClient {
    fun create(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_HTTP_URL)  // ← Usa BuildConfig
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
```

### Opción 2: En OkHttp

```kotlin
// data/network/HttpClient.kt
import cat.hajoya.piratasdeandromeda.BuildConfig

val httpClient = OkHttpClient.Builder()
    .addInterceptor(LoggingInterceptor())
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.BASE_HTTP_URL)  // ← Usa BuildConfig
    .client(httpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

### Opción 3: En WebSocket

```kotlin
// data/network/WebSocketClient.kt
import cat.hajoya.piratasdeandromeda.BuildConfig

fun connectToWebSocket() {
    val url = BuildConfig.BASE_WS_URL  // ← Usa BuildConfig
    val request = Request.Builder().url(url).build()
    
    val webSocket = okHttpClient.newWebSocket(request, webSocketListener)
}
```

### Opción 4: En ViewModel (Para logging)

```kotlin
// viewmodels/GameViewModel.kt
import cat.hajoya.piratasdeandromeda.BuildConfig
import android.util.Log

class GameViewModel(...) : ViewModel() {
    
    init {
        Log.d("API", "Base URL: ${BuildConfig.BASE_HTTP_URL}")
        Log.d("WS", "WebSocket URL: ${BuildConfig.BASE_WS_URL}")
    }
    
    fun fetchDataFromApi() {
        val apiUrl = BuildConfig.BASE_HTTP_URL + "ships"
        // ... código de Retrofit
    }
}
```

---

## 🔄 Flujo Completo de Ejemplo

### Paso 1: Crear servicio Retrofit

```kotlin
// data/network/ApiService.kt
import retrofit2.http.GET
import cat.hajoya.piratasdeandromeda.models.Ship

interface ApiService {
    @GET("ships")
    suspend fun getShips(): List<Ship>
}
```

### Paso 2: Crear RetrofitClient con BuildConfig

```kotlin
// data/network/RetrofitClient.kt
import cat.hajoya.piratasdeandromeda.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_HTTP_URL)  // ← IMPORTANTE
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    fun getApiService(): ApiService = retrofit.create(ApiService::class.java)
}
```

### Paso 3: Usar en Repository

```kotlin
// data/repository/GameRepository.kt
import cat.hajoya.piratasdeandromeda.data.network.RetrofitClient

class GameRepository {
    private val apiService = RetrofitClient.getApiService()
    
    suspend fun fetchShipsFromServer(): List<Ship> {
        return try {
            apiService.getShips()  // Usa URL de BuildConfig
        } catch (e: Exception) {
            emptyList()
        }
    }
}
```

### Paso 4: Usar en ViewModel

```kotlin
// viewmodels/GameViewModel.kt
class GameViewModel(private val repository: GameRepository) : ViewModel() {
    
    fun loadShips() {
        viewModelScope.launch(Dispatchers.IO) {
            val ships = repository.fetchShipsFromServer()
            // Actualizar UI con los datos
        }
    }
}
```

---

## 🧪 Verificar Valores en Tiempo de Ejecución

### Opción 1: Mediante Logcat

```kotlin
// En cualquier lugar del código
Log.d("BuildConfig", "Base URL: ${BuildConfig.BASE_HTTP_URL}")
Log.d("BuildConfig", "WS URL: ${BuildConfig.BASE_WS_URL}")
```

**Resultado en Logcat**:
```
Base URL: http://10.0.2.2:8000/        (DEBUG)
Base URL: https://api.piratasandromeda.me/  (RELEASE)
```

### Opción 2: Mediante AlertDialog (Solo para testing)

```kotlin
// En una Activity o Fragment
AlertDialog.Builder(this)
    .setTitle("BuildConfig Debug")
    .setMessage("Base URL: ${BuildConfig.BASE_HTTP_URL}\nWS URL: ${BuildConfig.BASE_WS_URL}")
    .setPositiveButton("OK", null)
    .show()
```

### Opción 3: Mediante Toast

```kotlin
Toast.makeText(
    this,
    "API: ${BuildConfig.BASE_HTTP_URL}",
    Toast.LENGTH_LONG
).show()
```

---

## ⚠️ Puntos Importantes

### 1. BuildConfig se genera automáticamente
```bash
# No necesitas hacer nada manual, pero asegúrate de:
./gradlew clean build  # Regenera BuildConfig
```

### 2. Distintos valores por buildType

```kotlin
// Si compilas en Debug:
./gradlew installDebug
// BuildConfig.BASE_HTTP_URL = "http://10.0.2.2:8000/"

// Si compilas en Release:
./gradlew assembleRelease
// BuildConfig.BASE_HTTP_URL = "https://api.piratasandromeda.me/"
```

### 3. Los valores son constantes en tiempo de compilación

```kotlin
// ✅ Correcto - En tiempo de compilación
val url = BuildConfig.BASE_HTTP_URL

// ❌ Incorrecto - Cambiar en runtime (NO funcionará)
BuildConfig.BASE_HTTP_URL = "https://otro.url.com"
```

### 4. Disponible desde Any lugar

```kotlin
// En Activity
BuildConfig.BASE_HTTP_URL

// En Fragment
BuildConfig.BASE_HTTP_URL

// En Repository
BuildConfig.BASE_HTTP_URL

// En ViewModel
BuildConfig.BASE_HTTP_URL

// En cualquier otra clase
BuildConfig.BASE_HTTP_URL
```

---

## 🎯 Caso de Uso Completo: Sincronización Local ↔ Servidor

```kotlin
// data/repository/SyncRepository.kt
import cat.hajoya.piratasdeandromeda.BuildConfig
import cat.hajoya.piratasdeandromeda.data.local.ShipRepository
import cat.hajoya.piratasdeandromeda.data.network.RetrofitClient
import android.util.Log

class SyncRepository(
    private val localRepository: ShipRepository,
    private val apiService: ApiService = RetrofitClient.getApiService(),
) {
    
    suspend fun syncShipsWithServer() {
        Log.d("Sync", "Sincronizando con ${BuildConfig.BASE_HTTP_URL}")
        
        try {
            // Obtener datos del servidor
            val serverShips = apiService.getShips()
            
            // Guardar en local
            serverShips.forEach { ship ->
                localRepository.addShip(ship.name)
            }
            
            Log.d("Sync", "✅ Sincronización completada")
        } catch (e: Exception) {
            Log.e("Sync", "❌ Error en sincronización: ${e.message}")
        }
    }
}
```

---

## 📊 Comparación: Debug vs Release

| Aspecto | Debug | Release |
|---------|-------|---------|
| Base URL | `http://10.0.2.2:8000/` | `https://api.piratasandromeda.me/` |
| WebSocket | `ws://10.0.2.2:8000` | `wss://api.piratasandromeda.me` |
| Protocolo | HTTP/WS | HTTPS/WSS (seguro) |
| Uso | Desarrollo local | Producción |
| BuildCommand | `./gradlew installDebug` | `./gradlew assembleRelease` |

---

## 🔒 Consideraciones de Seguridad

### En Release (Producción)
- ✅ Usa HTTPS (seguro)
- ✅ Usa WSS (seguro)
- ✅ No expone localhost
- ✅ Certificados SSL validados

### En Debug (Desarrollo)
- ⚠️ Usa HTTP (inseguro, solo local)
- ⚠️ Usa WS (inseguro, solo local)
- ⚠️ No debe usarse en producción
- ⚠️ Solo funciona en emulador/localhost

---

## 🚀 Próximos Pasos

1. **Implementa Retrofit** con `BuildConfig.BASE_HTTP_URL`
2. **Implementa WebSocket** con `BuildConfig.BASE_WS_URL`
3. **Prueba en Debug** (http://10.0.2.2:8000/)
4. **Prueba en Release** (https://api.piratasandromeda.me/)
5. **Implementa sincronización** local ↔ servidor

---

## 💾 Ejemplo Listo para Usar

Si necesitas un ejemplo completo, consulta:

```
Retrofit + BuildConfig:
  → Implementar en data/network/RetrofitClient.kt

WebSocket + BuildConfig:
  → Implementar en data/network/WebSocketClient.kt

Repository + BuildConfig:
  → Implementar en data/repository/GameRepository.kt
```

---

## 🆘 Troubleshooting

### P: BuildConfig no aparece en las sugerencias
**R**: Ejecuta `./gradlew clean build` para regenerar

### P: Dice que BuildConfig no existe
**R**: Verifica que `buildConfig = true` esté en build.gradle.kts

### P: La URL es incógnita en runtime
**R**: Usa `BuildConfig.BASE_HTTP_URL` directamente (es constante)

### P: Quiero cambiar la URL en runtime
**R**: No es posible con BuildConfig. Usa DataStore o preferences en su lugar.


