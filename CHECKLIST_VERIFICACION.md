# ✅ Checklist de Verificación

Usa este checklist para verificar que todo está bien configurado mientras realizas el proceso.

---

## 📝 FASE 1: Preparación de la Imagen

- [ ] Tengo mi imagen (relieve.png o similar)
- [ ] La imagen está en formato PNG
- [ ] La imagen tiene al menos 512x512 px
- [ ] La imagen tiene forma cuadrada (1:1)
- [ ] (Opcional) La imagen tiene fondo transparente para mejor resultado

---

## 🎬 FASE 2: Configurar Splash Screen

- [ ] Tengo acceso a la carpeta: `PiratesdAndromeda\app\app\src\main\res\drawable\`
- [ ] He copiado mi imagen a esa carpeta con el nombre: `relieve.png`
- [ ] Verifiqué que el archivo se llama exactamente: `relieve.png` (minúsculas)
- [ ] Abrí Android Studio
- [ ] Sincronicé el proyecto: `Build → Sync Now`
- [ ] (Opcional) Limpié: `Build → Clean Project`
- [ ] (Opcional) Reconstruí: `Build → Rebuild Project`

---

## 🎨 FASE 3: Configurar Icono de App

### Si elegio OPCIÓN A (Automática - Script):
- [ ] Tengo instalado ImageMagick (https://imagemagick.org)
- [ ] Copié `relieve.png` en la carpeta: `PiratesdAndromeda\app\`
- [ ] Ejecuté el script: `setup_app_icon_windows.ps1` (o .bat)
- [ ] El script completó sin errores
- [ ] Verifiqué que se generaron archivos en las carpetas `mipmap-*/`

### Si eligio OPCIÓN B (Android Studio):
- [ ] Abrí Android Studio
- [ ] Fui a: `File → New → Image Asset`
- [ ] Seleccioné el tipo: "Image"
- [ ] Cargué mi imagen: `relieve.png`
- [ ] Completé el asistente
- [ ] Android Studio generó automáticamente todos los tamaños
- [ ] Vi los nuevos archivos en `mipmap-*/`

### Si eligio OPCIÓN C (Online):
- [ ] Visité: https://romannunik.github.io/AsyncAssetStudio/icons-launcher.html
- [ ] Cargué mi imagen: `relieve.png`
- [ ] Descargué los assets generados
- [ ] Extraje el ZIP
- [ ] Copié el contenido de cada carpeta `mipmap-*/` en:
  - [ ] `app\app\src\main\res\mipmap-mdpi\`
  - [ ] `app\app\src\main\res\mipmap-hdpi\`
  - [ ] `app\app\src\main\res\mipmap-xhdpi\`
  - [ ] `app\app\src\main\res\mipmap-xxhdpi\`
  - [ ] `app\app\src\main\res\mipmap-xxxhdpi\`

---

## 🔄 FASE 4: Sincronizar y Probar

- [ ] Abrí Android Studio
- [ ] Hice clic en: `Build → Sync Now` (si aparece)
- [ ] Limpié: `Build → Clean Project`
- [ ] Reconstruí: `Build → Rebuild Project`
- [ ] Esperé a que termine la compilación
- [ ] No hay errores en la ventana de "Build"

---

## 🚀 FASE 5: Ejecutar la App

### En Emulador:
- [ ] Tengo un emulador abierto
- [ ] Presioné: `Shift + F10`
- [ ] La app se abrió
- [ ] Vi mi imagen en la SPLASH SCREEN (al iniciar)
- [ ] Pasé la splash screen
- [ ] Verifiqué que el ICONO es el mío en el menú de aplicaciones

### En Dispositivo Físico:
- [ ] Conecté mi teléfono/tablet por USB
- [ ] Android Studio lo detectó
- [ ] Presioné: `Shift + F10`
- [ ] La app se instaló
- [ ] Vi mi imagen en la SPLASH SCREEN (al iniciar)
- [ ] Pasé la splash screen
- [ ] Verifiqué que el ICONO es el mío en el menú de aplicaciones

---

## ✨ FASE 6: Verificación Final

- [ ] La SPLASH SCREEN muestra mi imagen 🎬
- [ ] El ICONO en el menú muestra mi imagen 🎨
- [ ] La app funciona normalmente después de la splash
- [ ] El icono aparece correctamente en diferentes tamaños

---

## 🆘 Si Algo No Funciona

### Splash Screen no cambia:
- [ ] Verifiqué que el archivo es `relieve.png` (minúsculas)
- [ ] Verifiqué que está en: `app\src\main\res\drawable\`
- [ ] Sincronicé: `Build → Sync Now`
- [ ] Limpié: `Build → Clean Project`
- [ ] Reconstruí: `Build → Rebuild Project`
- [ ] Reinstalé: `Shift + F10`

### Icono no cambia:
- [ ] Verifiqué que hay archivos en todas las carpetas `mipmap-*/`
- [ ] Verifiqué que se llaman: `ic_launcher.webp` (en todas)
- [ ] Sincronicé: `Build → Sync Now`
- [ ] Limpié: `Build → Clean Project`
- [ ] Reconstruí: `Build → Rebuild Project`
- [ ] Desinstalé la app manual
- [ ] Reinstalé: `Shift + F10`

### El script falló:
- [ ] Instalé ImageMagick
- [ ] Verifiqué que `relieve.png` está en `app\`
- [ ] Volví a ejecutar el script

### Total desastre:
- [ ] Leí: `INSTRUCCIONES_IMAGEN_PERSONALIZADA.md` (guía completa)
- [ ] Probé con OPCIÓN B (Android Studio)
- [ ] Probé con OPCIÓN C (Online)

---

## 📊 Resumen de Archivos Esperados

Después de hacer todo, deberías tener:

```
app\src\main\res\
├── drawable\
│   └── relieve.png ✅
├── mipmap-mdpi\
│   ├── ic_launcher.webp ✅
│   └── ic_launcher_round.webp ✅
├── mipmap-hdpi\
│   ├── ic_launcher.webp ✅
│   └── ic_launcher_round.webp ✅
├── mipmap-xhdpi\
│   ├── ic_launcher.webp ✅
│   └── ic_launcher_round.webp ✅
├── mipmap-xxhdpi\
│   ├── ic_launcher.webp ✅
│   └── ic_launcher_round.webp ✅
└── mipmap-xxxhdpi\
    ├── ic_launcher.webp ✅
    └── ic_launcher_round.webp ✅
```

---

## 🎉 ¡Enhorabuena!

Si marcaste todos los ☑️, ¡tu app está lista con tu imagen personalizada!

- ✨ Splash Screen: FUNCIONANDO
- 🎨 Icono de App: FUNCIONANDO
- 🚀 App: LISTA PARA USAR


