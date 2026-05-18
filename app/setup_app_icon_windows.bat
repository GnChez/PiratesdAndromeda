@echo off
REM Script para generar iconos de aplicación desde relieve.png
REM Requiere ImageMagick instalado

setlocal enabledelayedexpansion

echo.
echo ================================
echo GENERADOR DE ICONOS - PIRATA ANDROMEDA
echo ================================
echo.

REM Verificar si relieve.png existe
if not exist "relieve.png" (
    echo [ERROR] No se encontro relieve.png en la carpeta actual
    echo.
    echo Por favor, coloca tu imagen relieve.png en:
    echo %cd%
    echo.
    pause
    exit /b 1
)

echo [OK] Archivo encontrado: relieve.png
echo.

REM Buscar ImageMagick
where magick >nul 2>&1
if %errorlevel% neq 0 (
    where convert >nul 2>&1
    if %errorlevel% neq 0 (
        echo [ERROR] ImageMagick no esta instalado
        echo.
        echo Opciones:
        echo 1. Instala ImageMagick desde: https://imagemagick.org/script/download.php
        echo 2. O usa Android Studio: File ^> New ^> Image Asset
        echo 3. O usa: https://romannunik.github.io/AndroidAssetStudio/icons-launcher.html
        echo.
        pause
        exit /b 1
    )
)

echo [OK] ImageMagick encontrado
echo.
echo Generando iconos...
echo.

REM Crear carpetas si no existen
if not exist "app\src\main\res\mipmap-mdpi" mkdir app\src\main\res\mipmap-mdpi
if not exist "app\src\main\res\mipmap-hdpi" mkdir app\src\main\res\mipmap-hdpi
if not exist "app\src\main\res\mipmap-xhdpi" mkdir app\src\main\res\mipmap-xhdpi
if not exist "app\src\main\res\mipmap-xxhdpi" mkdir app\src\main\res\mipmap-xxhdpi
if not exist "app\src\main\res\mipmap-xxxhdpi" mkdir app\src\main\res\mipmap-xxxhdpi

REM Generar iconos para cada densidad
setlocal enabledelayedexpansion

set "colors=#D2AC5E"

for %%d in (mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi) do (
    if "%%d"=="mdpi" set size=48
    if "%%d"=="hdpi" set size=72
    if "%%d"=="xhdpi" set size=96
    if "%%d"=="xxhdpi" set size=144
    if "%%d"=="xxxhdpi" set size=192

    echo Generando ic_launcher para %%d ^(!size!x!size!px^)...
    magick convert relieve.png -resize !size!x!size! -background !colors! -gravity center -extent !size!x!size! "app\src\main\res\mipmap-%%d\ic_launcher.webp" 2>nul || convert relieve.png -resize !size!x!size! -background !colors! -gravity center -extent !size!x!size! "app\src\main\res\mipmap-%%d\ic_launcher.webp"

    echo Generando ic_launcher_round para %%d ^(!size!x!size!px^)...
    magick convert relieve.png -resize !size!x!size! -background !colors! -gravity center -extent !size!x!size! "app\src\main\res\mipmap-%%d\ic_launcher_round.webp" 2>nul || convert relieve.png -resize !size!x!size! -background !colors! -gravity center -extent !size!x!size! "app\src\main\res\mipmap-%%d\ic_launcher_round.webp"
)

echo.
echo ================================
echo [OK] Iconos generados exitosamente
echo ================================
echo.
echo Proximos pasos:
echo 1. Abre Android Studio
echo 2. Haz clic en "Sync Now"
echo 3. Ejecuta la app (Shift + F10)
echo.
pause

