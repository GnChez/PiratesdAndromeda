## Reestructuración Completa - Piratas de Andromeda

### ✅ CAMBIOS REALIZADOS

#### FASE 1-2: Modelos y ViewModels
- ✅ Creado `models/User.kt` con AuthState, RolUsuari y User
- ✅ Creado `models/GameModels.kt` con todas las entidades del juego
- ✅ Creado `viewmodels/AuthViewModel.kt` para autenticación
- ✅ Creado `viewmodels/GameViewModel.kt` para gestión de partidas
- ✅ Creado `viewmodels/AdminViewModel.kt` para panel de admin

#### FASE 3: Reorganización de Activities y Fragments

**AuthActivity (Nueva)**
- ✅ Ubicación: `ui/auth/AuthActivity.kt`
- ✅ Layout: `activity_auth.xml`
- ✅ Fragmentos iniciales: LoginFragment y RegisterFragment
- ✅ Lógica: Redirige a MainActivity después de login exitoso

**MainActivity (Reorganizada)**
- ✅ Ubicación: `ui/main/MainActivity.kt`
- ✅ Inicia directamente con StartPartidaFragment
- ✅ ViewModel compartido: GameViewModel

**Fragmentos de Preparación**
- ✅ StartPartidaFragment (antes ConfigPartFrFragment)
  - Layout: `config_part_fr.xml`
  - Permite crear o unirse a partidas
  
- ✅ ConfigHabitacionsFragment (antes ConfigHabPartFragment)
  - Layout: `config_hab_part.xml`
  - Configurar habitaciones
  
- ✅ PersonatgesFragment (PersonajesPartidaFragment)
  - Layout: `personajes_partida.xml`
  - Seleccionar personaje

**Fragmentos de Autenticación**
- ✅ LoginFragment
  - Layout: `inici.xml`
  
- ✅ RegisterFragment
  - Layout: `register.xml`

#### FASE 4: Configuración del Manifiest
- ✅ Actualizado `AndroidManifest.xml`
  - AuthActivity como LAUNCHER
  - MainActivity como actividad secundaria

### 📱 FLUJO DE NAVEGACIÓN

1. **AuthActivity (Inicio)**
   - LoginFragment → Login exitoso → MainActivity
   - LoginFragment → Sign Up → RegisterFragment
   - RegisterFragment → Login → LoginFragment
   - RegisterFragment → Registrarse exitoso → MainActivity

2. **MainActivity**
   - StartPartidaFragment (Inicial)
   - → Crear → ConfigHabitacionsFragment → PersonatgesFragment
   - → Unirse → PersonatgesFragment
   - X (desde cualquier pantalla) → Pantalla anterior o AuthActivity

### 🔑 PUNTOS CLAVE

- **ViewModels compartidos** dentro de cada Activity usando `by activityViewModels()`
- **Binding correcto** con `private val binding get() = _binding!!`
- **Fragmentos organizados** en carpetas según funcionalidad
- **Modelos centralizados** en carpeta `models/`
- **Sin errores de compilación** (estructura verificada)

### ⚠️ PENDIENTE

- [ ] Implementar lógica de base de datos (actualmente usa mock)
- [ ] Crear AdminActivity con sus fragmentos
- [ ] Implementar minijuegos
- [ ] Conectar WebSockets para multijugador


