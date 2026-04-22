#!/bin/bash
# Script de verificación de la estructura del proyecto

echo "=== VERIFICACIÓN DE ESTRUCTURA ==="

# Verificar que existen los directorios principales
echo "[✓] Verificando directorios..."
test -d "app/src/main/java/cat/hajoya/piratasdeandromeda/models" && echo "✓ models/" || echo "✗ models/ - FALTA"
test -d "app/src/main/java/cat/hajoya/piratasdeandromeda/viewmodels" && echo "✓ viewmodels/" || echo "✗ viewmodels/ - FALTA"
test -d "app/src/main/java/cat/hajoya/piratasdeandromeda/ui/auth" && echo "✓ ui/auth/" || echo "✗ ui/auth/ - FALTA"
test -d "app/src/main/java/cat/hajoya/piratasdeandromeda/ui/main" && echo "✓ ui/main/" || echo "✗ ui/main/ - FALTA"
test -d "app/src/main/java/cat/hajoya/piratasdeandromeda/ui/preparacio" && echo "✓ ui/preparacio/" || echo "✗ ui/preparacio/ - FALTA"

# Verificar que existen los archivos Kotlin
echo ""
echo "[✓] Verificando archivos Kotlin..."
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/models/User.kt" && echo "✓ models/User.kt" || echo "✗ models/User.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/models/GameModels.kt" && echo "✓ models/GameModels.kt" || echo "✗ models/GameModels.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/viewmodels/AuthViewModel.kt" && echo "✓ viewmodels/AuthViewModel.kt" || echo "✗ viewmodels/AuthViewModel.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/viewmodels/GameViewModel.kt" && echo "✓ viewmodels/GameViewModel.kt" || echo "✗ viewmodels/GameViewModel.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/viewmodels/AdminViewModel.kt" && echo "✓ viewmodels/AdminViewModel.kt" || echo "✗ viewmodels/AdminViewModel.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/ui/auth/AuthActivity.kt" && echo "✓ ui/auth/AuthActivity.kt" || echo "✗ ui/auth/AuthActivity.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/ui/auth/LoginFragment.kt" && echo "✓ ui/auth/LoginFragment.kt" || echo "✗ ui/auth/LoginFragment.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/ui/auth/RegisterFragment.kt" && echo "✓ ui/auth/RegisterFragment.kt" || echo "✗ ui/auth/RegisterFragment.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/ui/main/MainActivity.kt" && echo "✓ ui/main/MainActivity.kt" || echo "✗ ui/main/MainActivity.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/ui/preparacio/StartPartidaFragment.kt" && echo "✓ ui/preparacio/StartPartidaFragment.kt" || echo "✗ ui/preparacio/StartPartidaFragment.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/ui/preparacio/ConfigHabitacionsFragment.kt" && echo "✓ ui/preparacio/ConfigHabitacionsFragment.kt" || echo "✗ ui/preparacio/ConfigHabitacionsFragment.kt - FALTA"
test -f "app/src/main/java/cat/hajoya/piratasdeandromeda/ui/preparacio/PersonatgesFragment.kt" && echo "✓ ui/preparacio/PersonatgesFragment.kt" || echo "✗ ui/preparacio/PersonatgesFragment.kt - FALTA"

# Verificar que existen los layouts
echo ""
echo "[✓] Verificando layouts..."
test -f "app/src/main/res/layout/activity_auth.xml" && echo "✓ activity_auth.xml" || echo "✗ activity_auth.xml - FALTA"
test -f "app/src/main/res/layout/activity_main.xml" && echo "✓ activity_main.xml" || echo "✗ activity_main.xml - FALTA"
test -f "app/src/main/res/layout/inici.xml" && echo "✓ inici.xml" || echo "✗ inici.xml - FALTA"
test -f "app/src/main/res/layout/register.xml" && echo "✓ register.xml" || echo "✗ register.xml - FALTA"
test -f "app/src/main/res/layout/config_part_fr.xml" && echo "✓ config_part_fr.xml" || echo "✗ config_part_fr.xml - FALTA"
test -f "app/src/main/res/layout/config_hab_part.xml" && echo "✓ config_hab_part.xml" || echo "✗ config_hab_part.xml - FALTA"
test -f "app/src/main/res/layout/personajes_partida.xml" && echo "✓ personajes_partida.xml" || echo "✗ personajes_partida.xml - FALTA"

echo ""
echo "=== VERIFICACIÓN COMPLETADA ==="

