@echo off
cd /d "C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app"

echo Ejecutando clean assembleDebug...
call gradlew clean assembleDebug

echo.
echo Build completado
pause

