# Guía de Testing - Cambios Implementados

## 1. Test de Persistencia de Naves y Habitaciones

### Procedimiento Manual

#### Paso 1: Crear una nave
1. Abre la app
2. Navega a "Login" → "Crear partida"
3. Introduce un nombre de nave (ej: "Nave Prueba")
4. Pulsa "Crear"
5. **Verifica**: La nave debe aparecer en el RecyclerView y estar seleccionada (borde dorado)
6. **Persiste**: Cierra y abre la app → la nave debe seguir ahí

#### Paso 2: Agregar habitaciones
1. Con la nave creada seleccionada, introduce un nombre de habitación
2. Pulsa "Añadir habitación"
3. **Verifica**: La habitación aparece en el RecyclerView de habitaciones
4. Agrega más habitaciones (mínimo 2)
5. **Persiste**: Cierra y abre la app → las habitaciones deben seguir ahí

#### Paso 3: Seleccionar otra nave
1. En la pantalla de naves, crea otra nave (ej: "Nave 2")
2. **Verifica**: La nueva nave se selecciona automáticamente
3. Pulsa sobre la primera nave
4. **Verifica**: Las habitaciones se actualizan para mostrar las de la primera nave

#### Paso 4: Eliminar nave
1. Pulsa el botón de eliminar (🗑️) en una nave
2. Confirma la eliminación
3. **Verifica**: La nave desaparece del RecyclerView
4. **Verifica**: Sus habitaciones se eliminan en cascada (revisar Room database)

---

## 2. Test de URLs por BuildType

### Debug (Local - Emulador)
```
BASE_HTTP_URL = http://10.0.2.2:8000/
BASE_WS_URL = ws://10.0.2.2:8000
```

**Para verificar:**
```kotlin
// En un fragment o activity de debug
Log.d("API_URL", BuildConfig.BASE_HTTP_URL)
Log.d("WS_URL", BuildConfig.BASE_WS_URL)
```

### Release (Producción)
```
BASE_HTTP_URL = https://api.piratasandromeda.me/
BASE_WS_URL = wss://api.piratasandromeda.me
```

**Para compilar en release:**
```bash
./gradlew assembleRelease
# o desde Android Studio: Build > Build Bundles/APKs > Build APK(s)
```

---

## 3. Test de Fragmentos Nuevos vs Legacy

### Flujo Esperado (Fragmentos Nuevos)
```
IniciFragment / RegisterFragment / StartFrFragment
        ↓
   StartPartidaFragment (NUEVO)
        ↓
   ConfigHabitacionsFragment (NUEVO)
        ↓
   PersonatgesFragment
```

**Verifica:**
- Al navegar entre pantallas, se usan los fragmentos nuevos
- No hay referencias a ConfigPartFrFragment ni ConfigHabPartFragment en el flujo
- Los fragmentos legacy están deprecados pero no causan errores

---

## 4. Test de Room Database

### Inspeccionar datos guardados (Debug)

#### Opción 1: Android Studio Database Inspector
1. Abre Android Studio
2. Vé a: `View > Tool Windows > App Inspection`
3. Selecciona la pestaña "Database Inspector"
4. Busca la base de datos `piratas_offline_db`
5. **Verifica** las tablas:
   - `ships` - Contendrá las naves creadas
   - `rooms` - Contendrá las habitaciones

#### Opción 2: Logcat
Agrega logs en el ViewModel para verificar inserciones:

```kotlin
fun addSavedShip(name: String) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch(Dispatchers.IO) {
        val newId = shipRepository.addShip(trimmed)
        Log.d("ShipRepository", "Nave creada con ID: $newId, nombre: $trimmed")
        _selectedShipId.value = newId
    }
}
```

---

## 5. Checklist de Validación

### ✅ Persistencia
- [ ] Las naves se guardan y persisten entre reinicios
- [ ] Las habitaciones se guardan en la nave correcta
- [ ] Al eliminar una nave, sus habitaciones se eliminan
- [ ] La última nave creada se selecciona automáticamente

### ✅ UI/RecyclerView
- [ ] El RecyclerView de naves se actualiza al crear/eliminar
- [ ] El RecyclerView de habitaciones se actualiza al crear/eliminar
- [ ] Los contadores de habitaciones son correctos
- [ ] La selección visual (borde dorado) funciona correctamente

### ✅ Fragmentos
- [ ] No hay errores al navegar entre fragmentos
- [ ] El flujo completo funciona (Inicio → Naves → Habitaciones → Personajes)
- [ ] No aparecen referencias a fragmentos legacy

### ✅ URLs/API
- [ ] En debug, usa `http://10.0.2.2:8000/`
- [ ] En release, usa `https://api.piratasandromeda.me/`
- [ ] Los logs muestran la URL correcta según el buildType

---

## 6. Debugging

### Si las naves no se guardan:
1. Verifica que `ShipRepository` se inyecta correctamente en `GameViewModel`
2. Revisa que `AppDatabase.getInstance()` no está fallando
3. Compila con `./gradlew clean build`

### Si los fragmentos no navegan:
1. Verifica que `StartPartidaFragment` existe en `ui/preparacio/`
2. Comprueba que los imports son correctos en los fragmentos que hacen la navegación
3. Revisa el Logcat para excepciones de Fragment

### Si el RecyclerView está vacío:
1. Verifica que el adapter está inicializado
2. Comprueba que el Flow del ViewModel emite datos
3. Revisa que el LiveData/StateFlow está siendo observado correctamente

---

## 7. Comandos Útiles

```bash
# Limpiar y compilar
./gradlew clean build

# Compilar solo debug
./gradlew assembleDebug

# Compilar solo release
./gradlew assembleRelease

# Ejecutar tests unitarios
./gradlew test

# Ver logs en tiempo real
adb logcat | grep -E "TAG|Error"

# Instalar en emulador
./gradlew installDebug
```

---

## 8. Datos Esperados en Room

### Tabla `ships`
```
id  | name              | createdAt
----|-------------------|----------
1   | "Nave Prueba"     | 1234567890000
2   | "Nave 2"          | 1234567891000
```

### Tabla `rooms`
```
id  | shipId | name
----|--------|------------------
1   | 1      | "Sala de Combate"
2   | 1      | "Sala de Motores"
3   | 2      | "Camarote"
```

---

## 9. Troubleshooting Rápido

| Problema | Causa | Solución |
|----------|-------|----------|
| App no compila | Fragmentos legacy sin deprecate | ✓ Ya resuelto |
| RecyclerView vacío | Flow no emite | Revisa `ShipRepository` |
| Nave no persiste | Room no guarda | Verifica `AppDatabase` getInstance |
| URL incorrecta | BuildType no aplicado | Usa `BuildConfig.BASE_HTTP_URL` |
| Habitaciones no se muestran | ShipId no coincide | Verifica FK en Room |


