$fontDir = "C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app\app\src\main\res\font"

$filesToDelete = @(
    "pirata_font.xml",
    "pirata_font_backup.xml",
    "pirata_family_backup.xml",
    "pirata_regular.ttf"
)

foreach ($file in $filesToDelete) {
    $fullPath = Join-Path $fontDir $file
    if (Test-Path $fullPath) {
        Remove-Item $fullPath -Force
        Write-Host "Eliminado: $file"
    } else {
        Write-Host "No existe: $file"
    }
}

Write-Host ""
Write-Host "Archivos restantes en $fontDir :"
Get-ChildItem $fontDir | select Name

