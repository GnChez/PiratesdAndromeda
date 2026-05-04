# Resumen de Cambios Realizados

## 1. URLs de FastAPI - BuildTypes (Debug vs Release)

### Problema
La URL de la API estaba hardcodeada como localhost, lo que no funcionaba en release.

### Solución
Se ha configurado el `build.gradle.kts` para tener URLs diferentes según el buildType:

**Debug (desarrollo local):**
- `BASE_HTTP_URL`: `http://10.0.2.2:8000/` (emulador local)
- `BASE_WS_URL`: `ws://10.0.2.2:8000`

**Release (producción):**
- `BASE_HTTP_URL`: `https://api.piratasandromeda.me/`
- `BASE_WS_URL`: `wss://api.piratasandromeda.me`

### Ubicación del cambio
- `app/build.gradle.kts` - Configuración de `buildTypes`

---

## 2. Persistencia de Datos - Naves y Habitaciones

### Estado Actual
✅ Room database está correctamente configurado:
- `ShipEntity` - Entidad de naves
- `RoomEntity` - Entidad de habitaciones con FK a ShipEntity
- `ShipDao` y `RoomDao` - DAOs con queries
- `ShipRepository` - Repositorio que expone Flows
- `GameViewModel` - ViewModel que usa el repositorio

### Características Implementadas
✅ **Última nave creada seleccionada automáticamente**: El `GameViewModel.addSavedShip()` establece `_selectedShipId` al nuevo ID
✅ **Persistencia en Room**: Los datos se guardan automáticamente en la base de datos local
✅ **RecyclerView reactivo**: Los fragmentos observan `StateFlow` que se actualizan al cambiar datos en Room
✅ **Eliminación en cascada**: Al eliminar una nave, se eliminan sus habitaciones automáticamente

### Flujo de Datos
1. Usuario crea nave → `GameViewModel.addSavedShip()` → `ShipRepository.addShip()` → **Room**
2. Room emite evento → `StateFlow<List<SavedShip>>` → **RecyclerView actualizado**
3. Usuario selecciona nave → `GameViewModel.selectShip()` → `_selectedShipId` actualizado
4. `RoomsForShip` emite habitaciones filtradas → **RecyclerView de habitaciones actualizado**

### Ubicaciones Clave
- `data/local/ShipEntity.kt` - Entidad de naves con timestamp de creación
- `data/local/RoomEntity.kt` - Entidad de habitaciones con FK
- `data/repository/ShipRepository.kt` - Lógica centralizada
- `viewmodels/GameViewModel.kt` - Orquestador de datos
- `ui/preparacio/StartPartidaFragment.kt` - Pantalla de naves
- `ui/preparacio/ConfigHabitacionsFragment.kt` - Pantalla de habitaciones

---

## 3. Eliminación de Fragmentos Legacy

### Problema
Código duplicado en dos flujos de configuración:
- `ConfigPartFrFragment.kt` (legacy)
- `ConfigHabPartFragment.kt` (legacy)

Vs. la versión mejorada:
- `StartPartidaFragment.kt` (nuevo)
- `ConfigHabitacionsFragment.kt` (nuevo)

### Solución
✅ Se han actualizado todas las referencias para usar los fragmentos nuevos:
- `StartFrFragment.kt` → Ahora usa `StartPartidaFragment`
- `RegisterFragment.kt` → Ahora usa `StartPartidaFragment`
- `IniciFragment.kt` → Ahora usa `StartPartidaFragment`

✅ Los fragmentos legacy han sido **deprecados** (se mantienen en el proyecto pero no se usan)
- `ConfigPartFrFragment.kt` - Marcado como @Deprecated
- `ConfigHabPartFragment.kt` - Marcado como @Deprecated

### Beneficios
- **Una única fuente de verdad**: El flujo nuevo maneja todo
- **Mejor mantenimiento**: No hay código duplicado
- **Mejor persistencia**: Los fragmentos nuevos usan `GameViewModel` con Room correctamente

---

## 4. Modelo RoomItem - Corrección

### Cambio
Se cambió `RoomItem` para que `shipId` sea obligatorio (no tiene default value):

```kotlin
// Antes
data class RoomItem(
    val id: Long,
    val name: String,
    val shipId: Long = 0L,  // ❌ Default value problemático
)

// Después
data class RoomItem(
    val id: Long,
    val name: String,
    val shipId: Long,  // ✅ Obligatorio
)
```

### Razón
Facilita la relación correcta entre habitaciones y naves, evitando bugs donde una habitación podría no tener nave asignada.

---

## 5. Verificación de Compilación

Para verificar que todo compila correctamente:
```bash
./gradlew clean build
```

En caso de errores específicos, revisar:
1. Que las dependencias de Room estén actualizadas
2. Que el build.gradle.kts tenga la sintaxis correcta
3. Que los imports de los fragmentos nuevos sean correctos

---

## Próximos Pasos Recomendados

1. **Pruebas**: Crear/eliminar naves y habitaciones para verificar persistencia
2. **API**: Implementar sincronización con FastAPI una vez que se confirme la persistencia local
3. **Migración completa**: Eliminar completamente los archivos legacy después de validar

---

## Archivos Modificados
- ✅ `app/build.gradle.kts` - URLs por buildType
- ✅ `RoomItem.kt` - Modelo actualizado
- ✅ `StartFrFragment.kt` - Usa fragmento nuevo
- ✅ `RegisterFragment.kt` - Usa fragmento nuevo
- ✅ `IniciFragment.kt` - Usa fragmento nuevo
- ⚠️ `ConfigPartFrFragment.kt` - Deprecado (mantener por compatibilidad)
- ⚠️ `ConfigHabPartFragment.kt` - Deprecado (mantener por compatibilidad)

