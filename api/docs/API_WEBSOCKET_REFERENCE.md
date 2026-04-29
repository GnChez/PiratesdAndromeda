# Referencia API HTTP y WebSocket — Piratas de Andromeda

**Base URL (producción, backend en Docker en la VM):** `http://129.158.197.45:8000`  
**Base URL (desarrollo local):** `http://localhost:8000` — en el front se puede forzar con `?api=http://localhost:8000` o variables `VITE_BACKEND_URL` / `VITE_MONITOR_WS_URL`.  
**Prefijos de rutas FastAPI:**

| Prefijo     | Descripción        |
|------------|--------------------|
| `/users`   | Registro y login   |
| `/games`   | Ciclo de vida de la partida |
| `/`        | Raíz (mensaje de bienvenida) |

**Formato:** JSON en el cuerpo (`Content-Type: application/json`) salvo donde se indique.  
**CORS:** configurado para orígenes amplios (`*`) en desarrollo.

**Documentación interactiva:** al arrancar el servidor, `GET /docs` (Swagger UI) y `GET /redoc`.

---

## 1. Endpoints HTTP

### 1.1 `GET /`

| Campo | Valor |
|--------|--------|
| **Funcionalidad** | Comprobación de que el servicio está levantado. |
| **Query params** | Ninguno |
| **Body** | Ninguno |

**Response 200**

```json
{
  "mensaje": "Bienvenido al servidor del juego"
}
```

---

### 1.2 `POST /users/register`

| Campo | Valor |
|--------|--------|
| **Funcionalidad** | Crea un usuario: valida email y nombre únicos, hashea la contraseña y persiste en `usuarios`. |
| **Auth** | Ninguna |

**Request body (`UserCreate`)**

| Campo | Tipo | Obligatorio | Descripción |
|--------|------|-------------|-------------|
| `nombre_usuario` | `string` | Sí | Identificador único (máx. 50 caracteres en BD). |
| `email` | `string` (email) | Sí | Email único (máx. 100). |
| `password` | `string` | Sí | Contraseña en claro (se guarda hasheada con PBKDF2). |
| `avatar_url` | `string \| null` | No | URL opcional del avatar. |

**Ejemplo**

```json
{
  "nombre_usuario": "capitan_ana",
  "email": "ana@ejemplo.com",
  "password": "secreto123",
  "avatar_url": null
}
```

**Response 200 (`UserResponse`)**

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `id_usuario` | `integer` | PK del usuario. |
| `nombre_usuario` | `string` | |
| `email` | `string` | |
| `avatar_url` | `string \| null` | |
| `id_rol_sistema` | `integer` | FK a `rol_sistema` (por defecto 1). |
| `total_partidas_jugadas` | `integer` | |
| `total_puntos_acumulados` | `integer` | |
| `veces_impostor` | `integer` | |
| `veces_superviviente` | `integer` | |
| `veces_eliminado` | `integer` | |
| `fecha_ultima_conexion` | `string (ISO datetime) \| null` | Se rellena al registrarse. |

**Errores**

| Código | Cuándo |
|--------|--------|
| `400` | Email o nombre de usuario ya existentes (`detail` descriptivo). |

---

### 1.3 `POST /users/login`

| Campo | Valor |
|--------|--------|
| **Funcionalidad** | Autentica por **email o nombre de usuario** + contraseña; actualiza `fecha_ultima_conexion`. No devuelve JWT: el cliente debe usar `id_usuario` en las llamadas posteriores (ej. `/games/join`). |
| **Auth** | Ninguna |

**Request body**

Mismo esquema que **register** (`UserCreate`): el servicio busca filas donde `email == join_req.email` **o** `nombre_usuario == join_req.nombre_usuario` y verifica `password`.

**Response 200**

Igual que `UserResponse` en register.

**Errores**

| Código | Cuándo |
|--------|--------|
| `401` | Credenciales incorrectas (`detail`: `"Credenciales no válidas"`). |

---

### 1.4 `POST /games/create`

| Campo | Valor |
|--------|--------|
| **Funcionalidad** | Crea partida en BD, reparte misiones por habitaciones, genera `codigo_partida` y registra al creador como jugador. Registra en memoria el `ws_room_code` (UUID de sala) y asocia el código de partida al creador con `ws_code` para WebSocket. |
| **Auth** | Ninguna (se envía `id_creador` explícito). |

**Request body (`PartidaPedido`)**

| Campo | Tipo | Obligatorio | Descripción |
|--------|------|-------------|-------------|
| `id_creador` | `integer` | Sí | `id_usuario` del anfitrión. |
| `nombre_partida` | `string \| null` | No | Si se omite, el servidor asigna un nombre por defecto. |
| `presencial` | `boolean` | No | Default `false`. Si `true`, las habitaciones se toman de la BD; si `false`, de `habitaciones`. |
| `habitaciones` | `array` | Sí | Lista de `{ "nombre": "string" }` (`HabitacionBase`). En modo presencial puede usarse como respaldo según la lógica del servicio. |

**Ejemplo (modo no presencial)**

```json
{
  "id_creador": 1,
  "nombre_partida": "Noche en el galeón",
  "presencial": false,
  "habitaciones": [
    { "nombre": "Cocina" },
    { "nombre": "Puente" }
  ]
}
```

**Response 200 (`PartidaInitialResponse`)**

Incluye todos los campos de `PartidaPedido` más:

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `id_partida` | `integer` | ID numérico de la partida. |
| `codigo_partida` | `string` | Código corto (ej. 6 caracteres) para unirse y WebSocket. |
| `id_estado_partida` | `integer` | Estado inicial (p. ej. esperando). |
| `habitaciones_misiones` | `object` | Mapa `nombre_habitacion` → lista de `id_mision` (enteros). |
| `ws_code` | `string` | Token para **`/ws/join/{codigo_partida}/{ws_code}`** del creador. |
| `ws_room_code` | `string` | UUID interno de sala (no es el mismo que `codigo_partida`). |

**Errores**

| Código | Cuándo |
|--------|--------|
| `500` / error no HTTP | `ValueError` si faltan catálogos en BD, habitaciones o misiones. |

---

### 1.5 `POST /games/join`

| Campo | Valor |
|--------|--------|
| **Funcionalidad** | Añade un jugador a una partida en estado *esperando*. Genera un nuevo `ws_code` y lo registra en memoria. Incrementa `num_jugadores` en BD. |
| **Auth** | Ninguna (`id_jugador` explícito). |

**Request body (`PartidaJoinRequest`)**

| Campo | Tipo | Obligatorio |
|--------|------|-------------|
| `codigo_partida` | `string` | Sí |
| `id_jugador` | `integer` | Sí |

**Response 200 (`PartidaJoinResponse`)**

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `id_jugador` | `integer` | Mismo `id_usuario` enviado. |
| `nombre_partida` | `string \| null` | Nombre de la partida en BD. |
| `ws_code` | `string` | Token para conectar a **`/ws/join/{codigo_partida}/{ws_code}`**. |

**Errores**

| Código | Cuándo |
|--------|--------|
| `404` | Partida inexistente o no admite más jugadores (no está en estado esperando). |

---

### 1.6 `POST /games/leave`

| Campo | Valor |
|--------|--------|
| **Funcionalidad** | Elimina al jugador de `jugadores_partida`, invalida su `ws_code` en memoria, hace broadcast `PLAYER_LEFT` a la sala si existe, e intenta registrar evento `jugador_desconectado`. |
| **Auth** | Ninguna |

**Request body (`PartidaLeaveRequest`)**

| Campo | Tipo | Obligatorio |
|--------|------|-------------|
| `codigo_partida` | `string` | Sí |
| `id_jugador` | `integer` | Sí |

**Response 200 (`PartidaLeaveResponse`)**

```json
{
  "left": true
}
```

`left` es `false` si no existía la partida o el jugador no estaba en la partida.

---

### 1.7 `POST /games/start`

| Campo | Valor |
|--------|--------|
| **Funcionalidad** | Solo el **creador** (`creator` == `id_creador` de la partida): reparte misiones por jugador (`misiones_partida_jugador`), pasa la partida a *en curso*, elige `impostor_actual`, y emite por WebSocket `GAME_STARTED` a la sala. Registra evento `inicio_partida`. |
| **Auth** | Ninguna (query params explícitos). |

**Query parameters**

| Param | Tipo | Descripción |
|--------|------|-------------|
| `creator` | `integer` | Debe coincidir con `id_creador` de la partida `game_id`. |
| `game_id` | `integer` | `id_partida`. |

**Ejemplo:** `POST /games/start?creator=1&game_id=5`

**Response 200 (`PartidaStartResponse`)**

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `id_partida` | `integer` | |
| `id_estado_partida` | `integer` | Estado tras iniciar (en curso). |
| `impostor_actual` | `integer \| null` | `id_usuario` del impostor elegido. |
| `distribucion_misiones` | `object` | Claves `id_usuario` (número como string en JSON), valores: lista de `id_mision`. |

**Errores**

| Código | Cuándo |
|--------|--------|
| `400` | Partida inexistente, no es el creador, sin jugadores, catálogo de estado de misión faltante, etc. (`detail` con el mensaje). |

---

### 1.8 `POST /games/end`

| Campo | Valor |
|--------|--------|
| **Funcionalidad** | Marca la partida como **finalizada**, fija `fecha_fin` y `ganador_tripulacion` (explícito o inferido por % de reparación vs objetivo). Actualiza estadísticas agregadas en `usuarios`. Emite `GAME_ENDED` por WebSocket y registra `fin_partida`. |
| **Auth** | Ninguna |

**Request body (`PartidaEndRequest`)**

| Campo | Tipo | Obligatorio | Descripción |
|--------|------|-------------|-------------|
| `id_partida` | `integer` | Sí | |
| `ganador_tripulacion` | `boolean \| null` | No | Si es `null`, el servidor deduce: `porcentaje_reparacion_actual >= porcentaje_reparacion_victoria` → tripulación gana. |

**Response 200 (`PartidaEndResponse`)**

| Campo | Tipo | Descripción |
|--------|------|-------------|
| `id_partida` | `integer` | |
| `id_estado_partida` | `integer` | Estado finalizada. |
| `ganador_tripulacion` | `boolean \| null` | Quién ganó según reglas del modelo. |
| `fecha_fin` | `string (ISO datetime) \| null` | |
| `crew_won` | `boolean` | Indicador derivado usado internamente para stats (`true` = ganó tripulación). |

**Errores**

| Código | Cuándo |
|--------|--------|
| `400` | Partida inexistente u otra validación del servicio. |

---

## 2. Rutas WebSocket

Convención: usar la misma **base host** que HTTP, esquema `ws://` o `wss://`.  
Ejemplo: `ws://129.158.197.45:8000/ws/monitor/ABC123` (o `wss://` si el servidor expone HTTPS).

Todos los mensajes son **JSON** en texto sobre WebSocket salvo donde se indique ping en crudo.

---

### 2.1 `WS /ws/join/{game_code}/{ws_code}`

**Audiencia:** cliente móvil / jugador.

| Campo | Valor |
|--------|--------|
| **Funcionalidad** | Conexión en tiempo real a la sala de la partida. `game_code` = `codigo_partida`. `ws_code` = token devuelto por **`POST /games/create`** o **`POST /games/join`**. Tras conectar, el servidor envía a la sala un evento `PLAYER_JOINED`. |

**Parámetros de ruta**

| Param | Descripción |
|--------|-------------|
| `game_code` | Código de partida (mismo string que en REST). |
| `ws_code` | Token efímero por jugador y partida. |

**Cierre inmediato con error** (un único mensaje JSON y cierre de conexión):

| `code` (en JSON) | `message` (ejemplo) |
|-------------------|---------------------|
| `GAME_NOT_FOUND` | Juego inexistente en BD. |
| `GAME_ENDED` | Partida ya finalizada. |
| `GAME_NOT_LIVE` | Partida en BD pero sin sesión en este servidor (reinicio, etc.). |
| `WS_CODE_INVALID` | Token no válido o no coincide con la partida. |

**Formato del error**

```json
{
  "type": "ERROR",
  "code": "GAME_NOT_FOUND",
  "message": "Juego inexistente: no hay ninguna partida con este código."
}
```

**Mensajes entrantes (cliente → servidor)**

Cada mensaje debe ser un objeto JSON. El campo **`action`** determina el comportamiento.

| `action` | Campos útiles adicionales | Efecto resumido |
|----------|---------------------------|-----------------|
| `reunion_emergencia` | — | Inicia reunión; persiste evento padre; broadcast `REUNION`. Si ya había reunión: respuesta al emisor `{"type":"REUNION_ALREADY_ACTIVE"}`. |
| `jugador_eliminado` | `id_usuario_afectado` (opcional, int) | Marca muerto en BD; broadcast `PLAYER_DIED`. |
| `mision_saboteada` | — | Elige misión saboteable; modo presencial + dispositivo → `SABOTAGE_PENDING` + MQTT; si no, sabotaje directo + broadcast `SABOTAGE`. |
| `mision_desaboteada` | `mission_id` (int, **`id_mision_partida`**) | Broadcast `DESABOTAGE`; MQTT. **No** está en la lista de tipos persistidos automáticamente en `eventos_partida` con el flujo genérico (solo acciones en `ALLOWED_GAME_EVENT_TYPES`; este action puede no generar fila de evento). |
| `mision_iniciada` | `mission_id` | Broadcast `START_MISSION`; actualiza fila jugador-misión. |
| `mision_completada` | `mission_id` | Broadcast `COMPLETE_MISSION`; actualiza reparación acumulada (tope al % victoria). |
| `voto` | `id_usuario_afectado` (int o ausente = blanco) | Broadcast `VOTE_CAST`; persiste fila en `votos`; puede cerrar reunión y expulsar por mayoría. |
| `inicio_partida` | — | Solo creador: ejecuta lógica de inicio; broadcast `GAME_STARTED`. Errores: `GAME_START_DENIED`, `GAME_START_FAILED`. |
| `fin_partida` | `ganador_tripulacion` (bool o string opcional) | Solo creador: cierra partida y stats; broadcast `GAME_ENDED`. Errores: `GAME_END_DENIED`, `GAME_END_FAILED`. |
| `salir` | — | Sale de la sala, BD y broadcast `PLAYER_LEFT`. |
| *cualquier otra* | — | Reenvío genérico del objeto a todos los de la sala (incluye monitores). |

**Campos opcionales en persistencia de eventos** (cuando aplica):

- `id_usuario_afectado`
- `id_mision_relacionada`
- `descripcion`

El origen del evento en BD es el **`player_id`** resuelto del `ws_code`, no un `id_usuario` arbitrario en el JSON.

**Tipos de `action` que se persisten en `eventos_partida`** (vía `ALLOWED_GAME_EVENT_TYPES`):

`inicio_partida`, `fin_partida`, `mision_iniciada`, `mision_completada`, `mision_saboteada`, `jugador_eliminado`, `impostor_descubierto`, `cambio_impostor`, `jugador_desconectado`, `jugador_reconectado`, `votacion_iniciada`, `voto`, `pausa`, `reanudacion`, `otro`.

*(Nota: `reunion_emergencia` y `voto` tienen flujo propio; `reunion_emergencia` crea el evento padre al abrir la reunión.)*

**Mensajes salientes relevantes (servidor → cliente / sala)**

Algunos usan `event`, otros `type`:

| Clave principal | Ejemplo / descripción |
|-----------------|------------------------|
| `event`: `PLAYER_JOINED` | `{ "event", "player", "room" }` |
| `event`: `PLAYER_LEFT` | `{ "event", "player", "reason" }` |
| `type`: `REUNION` | Incluye `duration_seconds`, `player`. |
| `type`: `REUNION_FINISHED` | `reason`, `expelled_player`, `votes`. |
| `type`: `GAME_STARTED` | `id_partida`, `impostor_actual`, `distribucion_misiones`. |
| `type`: `GAME_ENDED` | `id_partida`, `ganador_tripulacion`, `fecha_fin`. |
| `type`: `START_MISSION`, `COMPLETE_MISSION`, `SABOTAGE`, `DESABOTAGE`, `SABOTAGE_PENDING`, `PLAYER_DIED`, `VOTE_CAST`, … | Ver implementación en `main.py`. |

---

### 2.2 `WS /ws/monitor/{game_code}`

**Audiencia:** monitor web (solo lectura en la sala en vivo).

| Campo | Valor |
|--------|--------|
| **Funcionalidad** | Si la partida **existe y no está finalizada** y hay sesión activa: se une como monitor y recibe los mismos broadcasts que los jugadores (sin procesar comandos de juego más allá de mantener el ping). Si la partida **está finalizada**: **no** entra al canal en vivo; recibe un snapshot **`MONITOR_READ_ONLY`** desde BD (evita confundir con otra partida que reutilice el código). |

**Parámetros de ruta**

| Param | Descripción |
|--------|-------------|
| `game_code` | Mismo `codigo_partida` que en REST. |

**Error y cierre (igual que join)**

```json
{
  "type": "ERROR",
  "code": "GAME_NOT_FOUND",
  "message": "..."
}
```

Códigos: `GAME_NOT_FOUND`, `GAME_NOT_LIVE` (partida en BD pero sin `room_uuid` en este proceso).

**Conexión con partida finalizada**

Primer mensaje:

```json
{
  "type": "MONITOR_READ_ONLY",
  "reason": "game_ended",
  "id_partida": 1,
  "codigo_partida": "ABC123",
  "nombre_partida": "...",
  "ganador_tripulacion": true,
  "fecha_fin": "2026-04-28T12:00:00",
  "porcentaje_reparacion_actual": "85.00",
  "porcentaje_reparacion_victoria": "100.00",
  "mensaje": "Esta partida ya terminó. ..."
}
```

La conexión permanece abierta; el cliente puede enviar **cualquier texto** periódicamente para mantener viva la conexión (el servidor hace `receive_text()` en bucle).

**Conexión en vivo**

1. El servidor envía:

```json
{
  "type": "MONITOR_CONNECTED",
  "game_code": "ABC123"
}
```

2. A partir de ahí, los mismos JSON de eventos que reciben los jugadores en esa sala.

---

## 3. Notas de integración

1. **Orden típico:** `register` / `login` → `create` (anfitrión) → otros `join` → `start` (REST o WS `inicio_partida`) → juego por WS → `end` (REST o WS `fin_partida`).
2. **Código de partida vs sala en memoria:** Tras un **reinicio del servidor**, la partida puede seguir en BD pero **`GAME_NOT_LIVE`** hasta que alguien vuelva a crear/asociar la sesión vía `POST /games/create` (nuevo código) o hasta que exista lógica que repueble `code_to_uuid` (actualmente no hay endpoint para “re-anclar” una partida antigua).
3. **Colisión de códigos:** Los códigos cortos pueden repetirse en el tiempo. El monitor de partidas **finalizadas** usa solo datos de BD y **no** el canal en vivo para ese código.

---

*Documento generado a partir del código en `piratas-server` (FastAPI). Para detalles exactos de validación Pydantic, consultar `schemas/` y `/docs`.*
