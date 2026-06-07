# Script para limpiar y sincronizar el proyecto
Write-Host "Limpiando cache de Gradle..." -ForegroundColor Green

# Eliminar carpetas de build
Remove-Item -Path ".\app\build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".\.gradle" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".\build" -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "Cache limpiado exitosamente" -ForegroundColor Green
Write-Host ""
Write-Host "Ahora necesitas hacer lo siguiente en Android Studio:" -ForegroundColor Yellow
Write-Host "1. File > Invalidate Caches / Restart" -ForegroundColor Yellow
Write-Host "2. O simplemente File > Sync Project with Gradle Files" -ForegroundColor Yellow
Write-Host ""
Write-Host "Esto debería resolver el problema y mostrar correctamente la vista Android." -ForegroundColor Yellow

