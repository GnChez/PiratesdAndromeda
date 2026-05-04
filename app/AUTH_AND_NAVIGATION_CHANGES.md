# ✅ Cambios Implementados - Autenticación API REST y Navegación de Naves

## 📋 Resumen de Cambios

Se han implementado 2 funcionalidades principales:

### 1️⃣ **Autenticación Real con API REST**
- ✅ Login con verificación en la API REST
- ✅ Registro de nuevos usuarios en la API REST
- ✅ Validación de credenciales contra la nube
- ✅ Manejo de errores con mensajes informativos
- ✅ UI feedback durante el login/registro (botones deshabilitados, mensajes)

### 2️⃣ **Navegación Automática al Clickear Nave**
- ✅ Al clickear una nave ya creada, se navega automáticamente a habitaciones
- ✅ Permite editar habitaciones de la nave seleccionada
- ✅ La nave queda seleccionada visualmente

---

## 📦 Archivos Modificados

```
✏️ viewmodels/AuthViewModel.kt
   └─ Reemplazar login/register mock con llamadas API REST reales
   └─ Usar UserCreate y UserResponse correctamente
   └─ Manejo de errores con try-catch

✏️ ui/auth/LoginFragment.kt
   └─ Observar AuthState.LOADING para deshabilitar botón
   └─ Mostrar mensajes de error con Snackbar
   └─ UI feedback visual durante login

✏️ ui/auth/RegisterFragment.kt
   └─ Observar AuthState.LOADING para deshabilitar botón
   └─ Mostrar mensajes de registro exitoso
   └─ UI feedback visual durante registro

✏️ ui/preparacio/StartPartidaFragment.kt
   └─ Navegar a ConfigHabitacionsFragment al seleccionar una nave
   └─ Evitar navegación duplicada
```

---

## 🔄 Flujo de Autenticación

### Antes (❌ Mock)
```
Usuario ingresa credenciales
        ↓
Mock acepta cualquier cosa
        ↓
Usuario accede a la app
```

### Ahora (✅ API REST)
```
Usuario ingresa credenciales
        ↓
Llamada HTTP POST /users/login → API REST en la nube
        ↓
API verifica si el usuario existe
        ↓
API verifica si la contraseña es correcta
        ↓
Si OK: Devuelve datos del usuario (idUsuario, nombreUsuario, email, etc.)
       Usuario accede a la app
       
Si Error: Devuelve error (usuario no existe, contraseña incorrecta, etc.)
         Se muestra mensaje de error al usuario
```

### Flujo de Registro
```
Usuario completa formulario (username, email, password)
        ↓
Llamada HTTP POST /users/register → API REST
        ↓
API crea usuario nuevo en la BD
        ↓
Si OK: Devuelve datos del usuario nuevo
       Usuario accede a la app automáticamente
       
Si Error: Devuelve error (usuario existe, email inválido, etc.)
         Se muestra mensaje de error
```

---

## 🚀 Pasos para Compilar y Validar

### Paso 1: Compilar Proyecto
```bash
cd "C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app"
./gradlew clean build
```

**Esperado**: ✅ BUILD SUCCESSFUL

### Paso 2: Instalar
```bash
./gradlew installDebug
```

**Esperado**: ✅ App se instala en emulador

### Paso 3: Validar Autenticación

#### Test 1: Registro de Nuevo Usuario
1. Abre la app
2. Pantalla de login
3. Click en "SIGN UP"
4. Rellena:
   - Username: `pirata_test_1`
   - Email: `pirata1@example.com`
   - Password: `Test123!`
   - Repetir: `Test123!`
5. Click "REGISTRA'T"
6. **Esperado**:
   - Botón se deshabilita (gris) mientras se procesa
   - ✅ Esperar la respuesta de la API (~2-5 segundos)
   - Si✅ Éxito: Mensaje "Registro exitoso" → Acceso a app
   - Si❌ Error: Mensaje de error específico

#### Test 2: Login con Usuario Registrado
1. Estar en login
2. Rellena:
   - Email: `pirata1@example.com`
   - Password: `Test123!`
3. Click "ENTRA"
4. **Esperado**:
   - Botón se deshabilita (gris)
   - ✅ Esperar la respuesta de la API
   - Si✅ Éxito: Mensaje "Sesión iniciada" → Acceso a app
   - Si❌ Error: Mensaje "Error en login: ..."

#### Test 3: Login con Contraseña Incorrecta
1. Email: `pirata1@example.com`
2. Password: `WrongPassword`
3. Click "ENTRA"
4. **Esperado**:
   - ✅ Mensaje de error de la API (autenticación fallida)

#### Test 4: Registro Duplicado
1. Intentar registra el mismo usuario `pirata_test_1`
2. **Esperado**:
   - ✅ Mensaje de error (usuario ya existe en BD)

---

## 🎯 Validar Navegación de Naves

### Test 5: Crear Nave y Editar Habitaciones
1. Inicia sesión ✅
2. Pantalla de naves (StartPartidaFragment)
3. Crea nueva nave: "Mi Nave Test"
4. Wait until navega a habitaciones ✅
5. Agrega habitación: "Sala Principal"
6. Vuelve atrás
7. **Esperado**: "Mi Nave Test" aparece en lista

### Test 6: Clickear Nave Existente
1. Ya en pantalla de naves
2. Click en "Mi Nave Test"
3. **Esperado**:
   - ✅ Navega automáticamente a ConfigHabitacionsFragment
   - Muestra "Sala Principal" que ya está creada
   - Puede agregar más habitaciones
   - El visual muestra la nave seleccionada (highlight)

### Test 7: Diversas Naves
1. Crea "Nave 1" con "Sala A", "Sala B"
2. Crea "Nave 2" con "Sala X", "Sala Y", "Sala Z"
3. Click en "Nave 1"
4. **Esperado**: Muestra solo "Sala A" y "Sala B"
5. Vuelve atrás
6. Click en "Nave 2"
7. **Esperado**: Muestra solo "Sala X", "Sala Y", "Sala Z"

---

## 💾 Persistencia Final

### Test 8: Datos Persisten Después del Reinicio
1. Crea 2 naves con varias habitaciones
2. Login correctamente
3. Cierra completamente la app
4. Reabre la app
5. Login con el mismo usuario
6. **Esperado**:
   - ✅ Las naves siguen ahí
   - ✅ Las habitaciones están asociadas correctamente
   - ✅ Puedes editar las habitaciones existentes

---

## 📝 Detalles Técnicos

### AuthViewModel.kt

**Cambios principales:**
1. Se inyecta `ApiService` desde `RetrofitClient`
2. `login()` ahora hace llamada real a `/users/login`
3. `register()` ahora hace llamada real a `/users/register`
4. Se mapea `UserResponse` a modelo local `User`
5. Se capturan excepciones y se muestran errores

**Flujo:**
```kotlin
// 1. Usuario llama login(email, password)
// 2. Se crea UserCreate(nombreUsuario, email, password)
// 3. apiService.login(UserCreate) → Retrofit hace POST
// 4. Se recibe UserResponse de la API
// 5. Se mapea a User local
// 6. Se persiste en SessionManager (DataStore)
// 7. Se emite AuthState.SUCCESS
```

### LoginFragment.kt y RegisterFragment.kt

**Cambios principales:**
1. Se observa `authState` para detectar LOADING
2. Cuando LOADING: botón se deshabilita (isEnabled=false, alpha=0.5f)
3. Se observa `errorMessage` para mostrar Snackbar
4. Feedback visual al usuario: Snackbar de éxito/error

**Estados:**
- `AuthState.IDLE` → Normal
- `AuthState.LOADING` → Botón deshabilitado
- `AuthState.SUCCESS` → Snackbar "Sesión iniciada"
- `AuthState.ERROR` → Snackbar con error

### StartPartidaFragment.kt

**Cambios principales:**
1. Se observa `selectedShipId`
2. Cuando `selectedShipId != null`, se navega automáticamente
3. Esto permite:
   - Crear nave → Navega auto
   - Clickear nave existente → Navega auto

**Flujo:**
```kotlin
// En observeViewModel()
viewModel.selectedShipId.collectLatest { selectedId ->
    adapter.setSelectedId(selectedId)  // Visual feedback
    if (selectedId != null) {           // Navegar si hay selección
        openConfigHabitacionsScreen()
    }
}
```

---

## 🧪 Logs Importantes para Debuggear

En Logcat, puedes ver:

```
// Login exitoso
D/Retrofit: POST /users/login
I/AuthViewModel: Login successful: idUsuario=123, nombreUsuario=pirata_test_1

// Error en login
E/AuthViewModel: Error en login: 401 Unauthorized

// Registro exitoso
D/Retrofit: POST /users/register
I/AuthViewModel: Register successful: idUsuario=124, nombreUsuario=nuevo_pirata

// Error al procesar
E/AuthViewModel: Error en registro: 400 Bad Request: email already exists
```

---

## ❌ Si Hay Problemas

### "Error: Cannot serialize 'nombreUsuario' to JSON"
**Causa**: El nombre del campo en UserCreate no coincide con la API  
**Solución**: Verifica que `@SerializedName("nombre_usuario")` es correcto

### "Error 401 Unauthorized"
**Causa**: Las credenciales son incorrectas o el usuario no existe  
**Solución**: Verifica que las credenciales sean correctas en la API

### "Error: Connection refused"
**Causa**: La API no está disponible o la URL es incorrecta  
**Solución**: Verifica que `NetworkConfig.baseHttpUrl` es correcto

### "Botón se queda deshabilitado después de error"
**Causa**: AuthState no se resetea después de error  
**Solución**: Ya está manejado, el botón debe habilitarse automáticamente

### "No navega a habitaciones al crear nave"
**Causa**: El selectedShipId no se establece correctamente  
**Solución**: Verifica que GameViewModel.selectShip() se llama después de la inserción

---

## 🎓 Conceptos Clave

1. **UserCreate**: DTO para enviar credenciales a la API
2. **UserResponse**: DTO que devuelve la API con datos del usuario autenticado
3. **ApiService**: Interfaz Retrofit que define los endpoints
4. **AuthState**: Enum que representa el estado actual (IDLE, LOADING, SUCCESS, ERROR)
5. **SessionManager**: Persiste datos en DataStore (encriptado)
6. **Room Database**: Persiste Naves y Habitaciones

---

## ✅ Resumen Final

| Funcionalidad | Antes ❌ | Después ✅ |
|---|---|---|
| Login | Mock (acepta todo) | Real (verifica en API) |
| Registro | Mock | Real (guarda en BD nube) |
| Feedback | No hay | Snackbar + Botón disabled |
| Clickear nave | No hace nada | Navega a habitaciones |
| Persistencia | Solo memoria | Room DB + API |
| Error handling | No hay | Mensajes específicos |

---

Ahora compila y prueba los tests. ¡Debe funcionar! 🚀

