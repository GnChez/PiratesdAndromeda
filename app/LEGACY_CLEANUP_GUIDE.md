# Plan de Eliminación de Código Legacy

## 📋 Estado Actual

Los fragmentos legacy han sido **deprecados pero mantienen compatibilidad**:
- `ConfigPartFrFragment.kt` - @Deprecated
- `ConfigHabPartFragment.kt` - @Deprecated

**¿Por qué no eliminados completamente?**
- Minimiza riesgo de regresiones
- Permite validación incremental
- Facilita rollback si es necesario

---

## ✅ Validación Previa a la Eliminación

Antes de eliminar los archivos legacy, asegúrate de:

### 1. Verificar que NO se usan
```bash
# Buscar referencias a los fragmentos legacy
grep -r "ConfigPartFrFragment" src/ --exclude-dir=.git
grep -r "ConfigHabPartFragment" src/ --exclude-dir=.git
```

**Resultado esperado**: NO encontrar ninguna referencia (excepto en los mismos archivos deprecated)

### 2. Compilar y validar
```bash
./gradlew clean build
```

**Resultado esperado**: Compilación sin errores

### 3. Ejecutar en dispositivo
- Crear una nave
- Crear habitaciones
- Eliminar nave
- Cerrar y abrir app
- Verificar que los datos persisten

---

## 🗑️ Pasos para Eliminar

### Si NO hay referencias a los fragmentos legacy:

#### Opción 1: Eliminación manual (segura)
```bash
# Desde PowerShell en Windows
rm "C:\path\to\ConfigPartFrFragment.kt"
rm "C:\path\to\ConfigHabPartFragment.kt"
```

#### Opción 2: Usar Git (recomendado)
```bash
git rm app/src/main/java/cat/hajoya/piratasdeandromeda/ConfigPartFrFragment.kt
git rm app/src/main/java/cat/hajoya/piratasdeandromeda/ConfigHabPartFragment.kt
git commit -m "Remove legacy fragments ConfigPartFrFragment and ConfigHabPartFragment"
```

#### Opción 3: Usando Android Studio
1. Click derecho en el archivo
2. "Delete"
3. Seleccionar "Delete file" (no "Remove from VCS")

---

## 🔄 Si hay referencias al código legacy

### Caso 1: Referencias en imports
```kotlin
// Eliminar imports no utilizados
import cat.hajoya.piratasdeandromeda.ConfigPartFrFragment  // ❌ Eliminar
import cat.hajoya.piratasdeandromeda.ConfigHabPartFragment  // ❌ Eliminar
```

### Caso 2: Referencias en métodos
```kotlin
// ❌ Antes
replace(R.id.fragment_container, ConfigPartFrFragment())

// ✅ Después
replace(R.id.fragment_container, StartPartidaFragment())
```

Si encuentras estas referencias:
1. Actualiza al fragmento nuevo
2. Compila y valida
3. Luego sí, elimina el legacy

---

## 📊 Checklist Pre-Eliminación

```
☐ Compilación limpia (./gradlew clean build)
☐ No hay referencias con grep
☐ App funciona en emulador/dispositivo
☐ Persistencia de datos funciona
☐ RecyclerView muestra datos correctamente
☐ Crear/eliminar naves y habitaciones funciona
☐ Fragmentos nuevos se navegan correctamente
☐ Git status muestra solo el cambio esperado
```

---

## 🚀 Ejecución Limpia

Una vez validado, ejecutar:

```bash
# 1. Buscar una última vez
grep -r "ConfigPartFrFragment\|ConfigHabPartFragment" src/

# 2. Compilar
./gradlew clean build

# 3. Instalar en dispositivo
./gradlew installDebug

# 4. Validar en dispositivo
# (crear nave, habitación, etc.)

# 5. Eliminar archivos
git rm app/src/main/java/.../ConfigPartFrFragment.kt
git rm app/src/main/java/.../ConfigHabPartFragment.kt

# 6. Compilar después de eliminar
./gradlew clean build

# 7. Commit
git commit -m "Remove deprecated legacy fragments"

# 8. Push
git push origin main
```

---

## ⚠️ Posibles Problemas y Soluciones

### Problema: Todavía hay referencias después de buscar
**Solución**: 
- Buscar también en archivos XML (layouts, navigation)
- Buscar en resources strings
- Compilar para ver errores específicos

### Problema: Compilación falla después de eliminar
**Solución**:
- Ejecutar `./gradlew clean build` de nuevo
- Si persiste, revisa que no hay imports colgantes

### Problema: La app no funciona después de eliminar
**Solución**:
- Revert: `git revert HEAD`
- Revisar si había referencias ocultas
- Buscar en todos los archivos del proyecto

---

## 📝 Notas Importantes

1. **Los fragmentos deprecados** están marcados con `@Deprecated` para que Android Studio muestre advertencias
2. **No rompen la compilación** - son solo clases vacías/comentadas
3. **Se pueden eliminar cuando sea seguro** - pero no es urgente
4. **Mantener en VCS (git)** - al menos una vez deprecados, para histórico

---

## 🎯 Recomendación Final

**Recomendado**: Mantener los fragmentos deprecated por 1-2 releases/semanas, luego eliminar.

**Garantiza**:
- Tiempo suficiente de validación
- Rollback fácil si algo falla
- Documentación histórica

---

## Dudas o Problemas

Si al eliminar surge algún problema:
1. Revert del cambio
2. Compilar nuevamente
3. Revisar referencias paso a paso


