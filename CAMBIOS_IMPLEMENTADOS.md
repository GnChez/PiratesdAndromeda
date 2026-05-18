# Cambios Implementados — Conformidad con GAME_CONNECTION_GUIDE

> Documento que lista todos los cambios realizados para que la app Android funcione según el `GAME_CONNECTION_GUIDE.md`

---

## Resumen de Cambios

Se han implementado los siguientes cambios para cumplir con la guía de conexión:

### 1. **WebSocketManager.kt** — Dual WebSocket Support

✅ **Cambios realizados:**
- Agregada clase `ConnectedPlayer` para representar jugadores conectados
- **Respaldo de conexión jugador** (`connect()` / `listener`)
  - Sigue igual pero mejorado con mejor manejo de eventos
- **Nueva conexión Monitor** (`connectMonitor()` / `monitorListener`)
  - Conecta a `/ws/monitor/{gameCode}` (sin ws_code requerido)
  - Recibe `MONITOR_CONNECTED` con `connected_players`
  - Actualiza `_connectedPlayers` StateFlow con lista de jugadores
  - Escucha `PLAYER_JOINED` y `PLAYER_LEFT` en tiempo real

✅ **Eventos procesados:**
- `PLAYER_JOINED` → Agrega jugador a lista
- `PLAYER_LEFT` → Elimina jugador de lista
- `MONITOR_CONNECTED` → Parsea lista inicial de jugadores

---

### 2. **NetworkModels.kt** — Campos extendidos

✅ **Cambios en WsMessage:**
- Agregados campos para `MONITOR_CONNECTED`:
  - `connectedPlayers: Any?`
  - `gameCode: String?`
  - `gameInProgress: Boolean?`
  - `progress: Int?`
  - `timeLimitSeconds: Long?`
  - `timeRemaining: Long?`
  - `playerNames: Any?`

✅ **Métodos helper mejorados:**
- `getDistribucionMisiones()` → Retorna `Map<String, Any>?`
- `getScores()` → Retorna `Map<String, Int>?`

---

### 3. **GameViewModel.kt** — Lógica centralizada

✅ **Nuevos StateFlows:**
```kotlin
_idCreador: MutableStateFlow<Int?>          // ID del usuario que crea la partida
_connectedPlayers: StateFlow<List<ConnectedPlayer>>  // Lista de jugadores conectados
```

✅ **Métodos añadidos:**
- `connectWebSocket(gameCode, wsCode)` → Conecta el WS del jugador con logging
- `connectMonitor(gameCode)` → Conecta el WS del monitor
- `setupWebSocketListeners()` → Mejorado:
  - Escucha notificaciones del WS jugador
  - **Nuevo:** Escucha cambios en `connectedPlayers`

✅ **Flujo mejorado:**
- `createGameFromSelectedShip()`:
  1. Crea partida en backend
  2. Guarda `codigo_partida`, `ws_code`, `id_partida`, **`id_creador`**
  3. **Conecta WS jugador**
  4. **Conecta WS monitor** (para lista de jugadores)

- `joinGame()`:
  1. Se une a partida en backend
  2. Guarda `codigo_partida`, `ws_code`
  3. **Conecta WS jugador**
  4. **Conecta WS monitor** (para lista de jugadores)

---

### 4. **PersonatgesFragment.kt** — Pantalla de espera de jugadores

✅ **Cambios realizados:**

**Observadores mejorados:**
```kotlin
// Mostrar código de partida
viewModel.wsGameCode.collect { gameCode ->
    binding.btnPartidaCode.text = gameCode
}

// Actualizar lista de jugadores en tiempo real
viewModel.connectedPlayers.collect { players ->
    actualizarListaJugadores(players.map { it.playerName })
}

// Verificar si es creador
viewModel.idCreador.collect { isCreator ->
    binding.btnEmpezar.isEnabled = (isCreator != null)
}
```

**Lógica de inicio de partida mejorada:**
- ✅ Verificar que el usuario es **creador** (solo el creador puede iniciar)
- ✅ Si no es creador → Mostrar mensaje "Esperando a que el anfitrión comience..."
- ✅ Si es creador → Enviar `action: "inicio_partida"` por WebSocket
- ✅ Logging detallado en cada paso

---

## Flujo Completo — JUEGO_CONNECTION_GUIDE

```
┌─ CREAR PARTIDA (Anfitrión/Creador)
│  ├─ POST /games/create
│  ├─ Guardar: codigo_partida, ws_code, id_partida, id_creador
│  ├─ WS /ws/join/{codigo_partida}/{ws_code}  ✅ CONECTADO
│  └─ WS /ws/monitor/{codigo_partida}         ✅ CONECTADO
│
├─ O UNIRSE A PARTIDA (Otros jugadores)
│  ├─ usuario introduce: codigo_partida
│  ├─ POST /games/join { codigo_partida, id_jugador }
│  ├─ WS /ws/join/{codigo_partida}/{ws_code}  ✅ CONECTADO
│  └─ WS /ws/monitor/{codigo_partida}         ✅ CONECTADO
│
└─ PANTALLA DE ESPERA (PersonatgesFragment)
   ├─ Monitor WS emite: MONITOR_CONNECTED → Mostrar jugadores conectados
   ├─ Escuchar: PLAYER_JOINED → Agregar a lista
   ├─ Escuchar: PLAYER_LEFT → Eliminar de lista
   ├─ Solo creador puede presionar "Comenzar"
   ├─ Creador presiona → Envía: { "action": "inicio_partida" }
   ├─ Todos reciben: GAME_STARTED event
   └─ Navegar a MenuJuegoFragment
```

---

## Especificaciones según GAME_CONNECTION_GUIDE

### Tabla de Acciones WebSocket

Según la guía, el cliente puede enviar:

| `action` | Solo Creador | Campos | Broadcast |
|----------|---|---|---|
| `inicio_partida` | ✅ **SÍ** | — | `GAME_STARTED` |
| `fin_partida` | ✅ **SÍ** | `ganador_tripulacion` (bool, opt) | `GAME_ENDED` |
| `mision_iniciada` | ❌ No | `mission_id` | `START_MISSION` |
| `mision_completada` | ❌ No | `mission_id` | `COMPLETE_MISSION` |
| `reunion_emergencia` | ❌ No | — | `REUNION` |
| `voto` | ❌ No | `id_usuario_afectado` (opt) | `VOTE_CAST` |

**Implementación:**
- ✅ `iniciarPartidaPorWebSocket()` → WebSocket con `action: "inicio_partida"` (solo creador)
- ✅ `finalizarPartidaPorWebSocket()` → WebSocket con `action: "fin_partida"` (solo creador)
- ✅ `sendMissionStarted()` → WebSocket con `action: "mision_iniciada"`
- ✅ `sendMissionCompleted()` → WebSocket con `action: "mision_completada"`

---

## URLs Base

```
API REST:     ${BuildConfig.BASE_HTTP_URL}
WebSocket:    ${BuildConfig.BASE_WS_URL}
```

Configuradas en `BuildConfig` (vía Gradle).

---

## Flujo de Puntos y Estadísticas

Según GAME_CONNECTION_GUIDE:
- Al completar misión: `action: "mision_completada"` → Servidor retorna `COMPLETE_MISSION` con `scores`
- `scores` se actualiza automáticamente en `_playerScores` en el ViewModel
- La UI observa `playerScores` y muestra puntos en tiempo real

---

## Reconexión

✅ **Reconexión automática implementada:**
- Máximo 3 reintentos (MAX_RETRIES = 3)
- Delay entre reintentos: 3 segundos (RETRY_DELAY_MS)
- Solo si la desconexión no fue manual (`isManualDisconnect`/`isManualMonitorDisconnect`)

✅ **Desconexión manual:**
- `disconnect()` → Cierra WS jugador
- `disconnectMonitor()` → Cierra WS monitor
- Ambas marcan `isManualDisconnect = true` para evitar reconexión automática

---

## Logging

Todos los eventos importantes están registrados con logs:

```
I/GameViewModel: ✅ WebSocket conectado exitosamente para partida: ABC123
I/PersonatgesFragment: Iniciando partida por WebSocket...
I/PersonatgesFragment: ✅ Partida iniciada, navegando al menú...
E/GameViewModel: ❌ Error al conectar WebSocket: [error message]
```

Use `adb logcat | grep "GameViewModel\|PersonatgesFragment"` para ver los logs.

---

## Verificación de Funcionalidad

Checklist para verificar que todo funciona:

- [ ] Al crear partida → WebSocket jugador conectado
- [ ] Al unirse → WebSocket jugador conectado  
- [ ] Monitor WS muestra lista inicial de jugadores
- [ ] Nuevo jugador se une → Aparece en lista (PLAYER_JOINED)
- [ ] Jugador se va → Desaparece de lista (PLAYER_LEFT)
- [ ] Botón "Comenzar" deshabilitado si NO eres creador
- [ ] Botón "Comenzar" habilitado si ERES creador
- [ ] Al presionar "Comenzar" → Se envía `action: "inicio_partida"`
- [ ] Todos reciben `GAME_STARTED` event
- [ ] Navega a MenuJuegoFragment

---

## Próximos Pasos

1. **Mejorar UI de lista de jugadores** en PersonajesPartidaBinding
2. **Agregar soporte para monitores web** en la app (WebView con `/ws/monitor`)
3. **Implementar pantalla de resultados** después de `GAME_ENDED`
4. **Agregar estadísticas de usuario** tras final de partida

---

*Documento generado el 17 de Mayo de 2026*  
*Basado en: API_WEBSOCKET_REFERENCE.md + GAME_CONNECTION_GUIDE.md*

