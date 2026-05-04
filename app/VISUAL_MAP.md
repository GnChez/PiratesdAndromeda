# 🗺️ Mapa Visual de Cambios

## 1️⃣ Arquitectura de Persistencia de Datos

```
┌─────────────────────────────────────────────────┐
│              INTERFAZ DE USUARIO                │
│  StartPartidaFragment  ConfigHabitacionsFragment│
└────────────┬──────────────────────────┬─────────┘
             │                          │
             │ Observa StateFlow        │
             │                          │
┌────────────▼──────────────────────────▼─────────┐
│           GameViewModel                         │
│  (OrquestaLógica de Aplicación)                 │
│  - addSavedShip()                               │
│  - deleteSavedShip()                            │
│  - addRoom()                                    │
│  - deleteRoom()                                 │
└────────────┬──────────────────────────┬─────────┘
             │ Usa                      │
             │                          │
┌────────────▼──────────────────────────▼─────────┐
│        ShipRepository                           │
│  (Centraliza Acceso a Datos)                    │
│  - allShips: Flow<List<SavedShip>>              │
│  - getRoomsForShip(shipId): Flow<List<RoomItem>>│
│  - allRooms: Flow<List<RoomItem>>               │
└────────────┬──────────────────────────┬─────────┘
             │ Lee/Escribe             │
             │                          │
        ┌────▼──────────────────────────▼────┐
        │      AppDatabase (Room)             │
        │                                     │
        │  ┌─────────────────────────────┐   │
        │  │ ShipEntity (Tabla: ships)   │   │
        │  │ - id (PrimaryKey)           │   │
        │  │ - name                      │   │
        │  │ - createdAt                 │   │
        │  └─────────────────────────────┘   │
        │                                     │
        │  ┌─────────────────────────────┐   │
        │  │ RoomEntity (Tabla: rooms)   │   │
        │  │ - id (PrimaryKey)           │   │
        │  │ - shipId (ForeignKey)       │   │
        │  │ - name                      │   │
        │  └─────────────────────────────┘   │
        │                                     │
        └─────────────────────────────────────┘
                        │
                        │ Almacena
                        ▼
            📱 Almacenamiento Local
             piratas_offline_db.db
```

---

## 2️⃣ Flujo de Crear una Nave

```
┌─────────────────┐
│ Usuario escribe │
│  nombre nave    │
└────────┬────────┘
         │ Pulsa "Crear"
         ▼
┌─────────────────────────────────┐
│ StartPartidaFragment             │
│ - viewModel.addSavedShip(name)   │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ GameViewModel.addSavedShip()     │
│ - Valida nombre                 │
│ - Llama shipRepository.addShip() │
│ - Establece selectedShipId       │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ ShipRepository.addShip()         │
│ - INSERT ShipEntity              │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ Room - ShipDao.insert()          │
│ - Crea registro en tabla ships   │
│ - Retorna nuevo ID              │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ StateFlow<List<SavedShip>>       │
│ - Emite evento de cambio         │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ Fragment observa cambio          │
│ - Recibe nueva lista de naves    │
│ - Llama adapter.submitList()     │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ RecyclerView se actualiza        │
│ - Muestra nave nueva             │
│ - Nave está seleccionada ✓       │
└─────────────────────────────────┘
```

---

## 3️⃣ Flujo de URLs por BuildType

```
┌────────────────────────────────────────┐
│         build.gradle.kts               │
├────────────────────────────────────────┤
│  buildTypes {                          │
│    debug {                             │
│      BASE_HTTP_URL = localhost:8000    │
│      BASE_WS_URL = ws://localhost      │
│    }                                   │
│    release {                           │
│      BASE_HTTP_URL = api.prod.com      │
│      BASE_WS_URL = wss://api.prod.com  │
│    }                                   │
│  }                                     │
└────────────┬───────────────────────────┘
             │ Genera automáticamente
             ▼
    ┌─────────────────────────┐
    │  BuildConfig (Clase)    │
    │  (Autogenerada)         │
    │                         │
    │  BASE_HTTP_URL = ???    │◄─┐
    │  BASE_WS_URL = ???      │  │
    └─────────────────────────┘  │
                                 │
         ┌───────────────────────┤
         │                       │
    ┌────▼─────┐          ┌─────▼──────┐
    │  DEBUG   │          │  RELEASE   │
    │ (Local)  │          │ (Prod)     │
    └────┬─────┘          └─────┬──────┘
         │                      │
    10.0.2.2:8000           https://api.
    (Emulador)              piratasandromeda.me
```

---

## 4️⃣ Cambio de Fragmentos (Antes vs Después)

### ❌ ANTES (Legacy)
```
StartFrFragment
    ↓
ConfigPartFrFragment (Legacy) ← Duplicado
    ↓
ConfigHabPartFragment (Legacy) ← Duplicado
    ↓
PersonajesPartidaFragment
```

### ✅ DESPUÉS (Nuevo)
```
StartFrFragment
    ↓
StartPartidaFragment (Nuevo) ← Unificado
    ↓
ConfigHabitacionsFragment (Nuevo) ← Unificado
    ↓
PersonatgesFragment
```

---

## 5️⃣ Estructura de Carpetas (Relevante)

```
app/src/main/
├── java/cat/hajoya/piratasdeandromeda/
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt       ✅ Room DB
│   │   │   ├── ShipEntity.kt        ✅ Tabla naves
│   │   │   ├── RoomEntity.kt        ✅ Tabla habitaciones
│   │   │   ├── ShipDao.kt           ✅ DAO naves
│   │   │   ├── RoomDao.kt           ✅ DAO habitaciones
│   │   │   └── SessionManager.kt
│   │   └── repository/
│   │       └── ShipRepository.kt    ✅ Acceso centralizado
│   ├── ui/
│   │   └── preparacio/
│   │       ├── StartPartidaFragment.kt      ✅ NUEVO
│   │       └── ConfigHabitacionsFragment.kt ✅ NUEVO
│   ├── viewmodels/
│   │   └── GameViewModel.kt         ✅ Orquestador
│   ├── StartFrFragment.kt           ✅ MODIFICADO
│   ├── RegisterFragment.kt          ✅ MODIFICADO
│   ├── IniciFragment.kt             ✅ MODIFICADO
│   ├── ConfigPartFrFragment.kt      ⚠️ DEPRECATED
│   ├── ConfigHabPartFragment.kt     ⚠️ DEPRECATED
│   └── ... otros archivos
└── res/
    └── ... recursos
```

---

## 6️⃣ Estados de Archivos Modificados

```
┌───────────────────────┬──────────────┬──────────────────────┐
│      Archivo          │    Estado    │      Acción          │
├───────────────────────┼──────────────┼──────────────────────┤
│ build.gradle.kts      │ ✅ Modificado │ URLs por buildType   │
│ RoomItem.kt           │ ✅ Modificado │ shipId obligatorio   │
│ StartFrFragment.kt    │ ✅ Modificado │ Usa StartPartida     │
│ RegisterFragment.kt   │ ✅ Modificado │ Usa StartPartida     │
│ IniciFragment.kt      │ ✅ Modificado │ Usa StartPartida     │
│ ConfigPartFrFragment  │ ⚠️ Deprecated │ Mantener compatible  │
│ ConfigHabPartFragment │ ⚠️ Deprecated │ Mantener compatible  │
└───────────────────────┴──────────────┴──────────────────────┘
```

---

## 7️⃣ Ciclo de Vida de Persistencia

```
┌─────────────────────────────────────────────────────┐
│          Ciclo Completo de Datos                    │
└─────────────────────────────────────────────────────┘

1. CREAR NAVE
   Usuario → Fragment → ViewModel → Repository → Room → DB

2. LEER NAVES
   DB → Room → Repository → StateFlow → Fragment → RecyclerView

3. SELECCIONAR NAVE
   Usuario → Fragment → ViewModel → StateFlow actualizado

4. CREAR HABITACION (EN NAVE SELECCIONADA)
   Usuario → Fragment → ViewModel → Repository → Room → DB

5. LEER HABITACIONES (DE NAVE SELECCIONADA)
   DB → Room → Repository → StateFlow filtrado → Fragment → RecyclerView

6. ELIMINAR NAVE
   Usuario → Fragment → ViewModel → Repository → Room
   ↓
   Cascada: Elimina nave + sus habitaciones
   ↓
   DB actualizado → StateFlow emite cambio → UI actualizada

7. CIERRE Y REAPERTURA APP
   ✅ Los datos persisten en Room
   ✅ Room emite datos al iniciar
   ✅ UI se actualiza automáticamente
```

---

## 8️⃣ Validación Visual

```
┌──────────────────────────────────────────────────┐
│  ANTES DE IMPLEMENTAR                            │
├──────────────────────────────────────────────────┤
│ ❌ URLs hardcodeadas (solo localhost)            │
│ ❌ Datos en memoria (no persisten)               │
│ ❌ Fragmentos duplicados (legacy)                │
│ ❌ RecyclerView puede estar vacío                │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│  DESPUÉS DE IMPLEMENTAR                          │
├──────────────────────────────────────────────────┤
│ ✅ URLs por buildType (Debug + Release)          │
│ ✅ Datos en Room (persisten)                     │
│ ✅ Fragmentos nuevos (unificados)                │
│ ✅ RecyclerView siempre actualizado              │
│ ✅ Cascada de eliminación funciona               │
│ ✅ Última nave seleccionada automáticamente      │
└──────────────────────────────────────────────────┘
```

---

## 9️⃣ Diagrama de Dependencias

```
        ┌─────────────────┐
        │  StartFrFragment│
        │  RegisterFragment
        │  IniciFragment  │
        └────────┬────────┘
                 │ Navega a
                 ▼
        ┌─────────────────────────────┐
        │ StartPartidaFragment (NUEVO)│
        │ ConfigHab..Fragment (NUEVO) │
        └────────┬────────────────────┘
                 │ Usa
                 ▼
        ┌─────────────────────────────┐
        │   GameViewModel             │
        │   (activityViewModels)      │
        └────────┬────────────────────┘
                 │ Inyecta
                 ▼
        ┌─────────────────────────────┐
        │    ShipRepository           │
        │    (Singleton)              │
        └────────┬────────────────────┘
                 │ Accede
                 ▼
        ┌─────────────────────────────┐
        │   AppDatabase (Room)        │
        │   ShipDao, RoomDao          │
        └────────┬────────────────────┘
                 │ Almacena
                 ▼
        📱 piratas_offline_db.db
```

---

## 🔟 Tabla de Decisiones Implementadas

| Decisión | Opción Elegida | Razón |
|----------|----------------|-------|
| Persistencia | Room | Estándar Android, eficiente |
| URLs | BuildConfig | Tipo-seguro, compilación |
| Fragmentos | Deprecación | Evita regresiones |
| Relaciones | FK + Cascada | Integridad datos |
| StateFlow | Reactivo | Actualizaciones automáticas |

---

## 📍 Ubicaciones Clave

```
URLs:              app/build.gradle.kts (líneas 31-45)
Persistencia:      data/local/*.kt
Repositorio:       data/repository/ShipRepository.kt
ViewModel:         viewmodels/GameViewModel.kt
UI:                ui/preparacio/*.kt
Modelos:           *.kt en raíz (SavedShip.kt, RoomItem.kt)
```


