# Script para generar el icono de la aplicación desde una imagen PNG
# Requisitos: ImageMagick debe estar instalado

# Colores basados en el proyecto
$backgroundColor = "#D2AC5E"

# Obtener el archivo de imagen
$imageFile = Get-Item -Path "relieve.png" -ErrorAction SilentlyContinue

if ($null -eq $imageFile) {
    Write-Host "❌ Error: No se encontró 'relieve.png' en la carpeta actual" -ForegroundColor Red
    Write-Host "Por favor, coloca tu imagen 'relieve.png' en: $PSScriptRoot" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Carpeta actual: $PSScriptRoot" -ForegroundColor Cyan
    exit 1
}

Write-Host "✅ Imagen encontrada: $($imageFile.FullName)" -ForegroundColor Green
Write-Host ""

# Verificar si ImageMagick está instalado
$magickPath = Get-Command "magick" -ErrorAction SilentlyContinue
if ($null -eq $magickPath) {
    $magickPath = Get-Command "convert" -ErrorAction SilentlyContinue
}

if ($null -eq $magickPath) {
    Write-Host "⚠️  ImageMagick no está instalado" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Tienes dos opciones:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1️⃣  Instala ImageMagick desde: https://imagemagick.org/script/download.php" -ForegroundColor White
    Write-Host "   Luego ejecuta este script nuevamente" -ForegroundColor White
    Write-Host ""
    Write-Host "2️⃣  Usa Android Asset Studio online:" -ForegroundColor White
    Write-Host "   https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html" -ForegroundColor White
    Write-Host "   - Sube tu relieve.png" -ForegroundColor White
    Write-Host "   - Descarga los assets generados" -ForegroundColor White
    Write-Host "   - Copia las carpetas mipmap-* en:" -ForegroundColor White
    Write-Host "     app\app\src\main\res\" -ForegroundColor White
    exit 1
}

Write-Host "✅ ImageMagick encontrado en: $($magickPath.Source)" -ForegroundColor Green
Write-Host ""
Write-Host "Generando iconos en múltiples tamaños..." -ForegroundColor Cyan
Write-Host ""

# Definir tamaños para cada densidad
$densities = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

$appResPath = ".\app\src\main\res"
$success = $true
$generatedFiles = @()

foreach ($density in $densities.GetEnumerator()) {
    $folderName = $density.Key
    $size = $density.Value

    $mipmapPath = Join-Path $appResPath "mipmap-$folderName"

    if (-not (Test-Path $mipmapPath)) {
        Write-Host "📁 Creando carpeta: $mipmapPath" -ForegroundColor Yellow
        New-Item -ItemType Directory -Path $mipmapPath -Force | Out-Null
    }

    $outputPath = Join-Path $mipmapPath "ic_launcher.webp"
    $outputPathRound = Join-Path $mipmapPath "ic_launcher_round.webp"

    try {
        # Generar icono cuadrado
        Write-Host "⏳ Generando ic_launcher ($folderName): ${size}x${size}px..." -NoNewline

        # Usar magick o convert dependiendo de lo que esté disponible
        if ($null -ne (Get-Command "magick" -ErrorAction SilentlyContinue)) {
            magick convert $imageFile.FullName -resize "${size}x${size}" -background "$backgroundColor" -gravity center -extent "${size}x${size}" "$outputPath"
        } else {
            convert $imageFile.FullName -resize "${size}x${size}" -background "$backgroundColor" -gravity center -extent "${size}x${size}" "$outputPath"
        }

        Write-Host " ✅" -ForegroundColor Green
        $generatedFiles += $outputPath

        # Generar icono redondeado
        Write-Host "⏳ Generando ic_launcher_round ($folderName): ${size}x${size}px..." -NoNewline

        if ($null -ne (Get-Command "magick" -ErrorAction SilentlyContinue)) {
            # Crear icono con esquinas redondeadas
            magick convert $imageFile.FullName -resize "${size}x${size}" -background "$backgroundColor" -gravity center -extent "${size}x${size}" `
                -bordercolor "$backgroundColor" -border 0 `
                -background none -alpha off -alpha on `
                -fill white -stroke none -distort barrel "0.0 0.0 0.0" `
                -trim +repage `
                "$outputPathRound"
        } else {
            convert $imageFile.FullName -resize "${size}x${size}" -background "$backgroundColor" -gravity center -extent "${size}x${size}" `
                -bordercolor "$backgroundColor" -border 0 `
                -background none -alpha off -alpha on `
                -fill white -stroke none `
                "$outputPathRound"
        }

        Write-Host " ✅" -ForegroundColor Green
        $generatedFiles += $outputPathRound

    } catch {
        Write-Host " ❌" -ForegroundColor Red
        Write-Host "Error al generar $outputPath`: $_" -ForegroundColor Red
        $success = $false
    }
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

if ($success) {
    Write-Host "✅ ¡Iconos generados exitosamente!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📝 Archivos generados:" -ForegroundColor Cyan
    foreach ($file in $generatedFiles) {
        Write-Host "   - $file" -ForegroundColor Green
    }
    Write-Host ""
    Write-Host "🔄 Próximos pasos:" -ForegroundColor Yellow
    Write-Host "   1. Abre Android Studio" -ForegroundColor White
    Write-Host "   2. Haz clic en 'Sync Now'" -ForegroundColor White
    Write-Host "   3. Ejecuta la app (Shift + F10)" -ForegroundColor White
    Write-Host ""
    Write-Host "✨ Tu aplicación usará estos nuevos iconos" -ForegroundColor Cyan
} else {
    Write-Host "❌ Hubo errores generando los iconos" -ForegroundColor Red
    Write-Host ""
    Write-Host "Intenta con el método online:" -ForegroundColor Yellow
    Write-Host "https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html" -ForegroundColor Cyan
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

