# 📁 Estructura de Carpetas y Ubicación de Archivos

## La Ruta Exacta Donde Va Tu Imagen

```
C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\
│
├─📦 app\ (carpeta raíz del módulo)
│  │
│  ├─ setup_app_icon_windows.ps1         ← EJECUTA ESTE (si quieres)
│  ├─ setup_app_icon_windows.bat         ← O ESTE (doble clic)
│  ├─ SETUP_RAPIDO.md                    ← LEE ESTO PRIMERO
│  │
│  └─ 📦 app\ (subcarpeta)
│     │
│     └─ 📦 src\main\res\
│        │
│        ├─ 📂 drawable\
│        │  ├─ relieve.png               ← 🎯 TÚ PONES AQUÍ TU IMAGEN
│        │  │                              (para la Splash Screen)
│        │  └─ (otros archivos drawable...)
│        │
│        ├─ 📂 mipmap-mdpi\              ← DESPUÉS AQUÍ (para Iconos)
│        │  ├─ ic_launcher.webp          (48x48 px)
│        │  ├─ ic_launcher_round.webp    (48x48 px)
│        │  └─ ic_launcher_foreground.webp
│        │
│        ├─ 📂 mipmap-hdpi\              ← O AQUÍ (para Iconos)
│        │  ├─ ic_launcher.webp          (72x72 px)
│        │  ├─ ic_launcher_round.webp    (72x72 px)
│        │  └─ ic_launcher_foreground.webp
│        │
│        ├─ 📂 mipmap-xhdpi\             ← O AQUÍ (para Iconos)
│        │  ├─ ic_launcher.webp          (96x96 px)
│        │  ├─ ic_launcher_round.webp    (96x96 px)
│        │  └─ ic_launcher_foreground.webp
│        │
│        ├─ 📂 mipmap-xxhdpi\            ← O AQUÍ (para Iconos)
│        │  ├─ ic_launcher.webp          (144x144 px)
│        │  ├─ ic_launcher_round.webp    (144x144 px)
│        │  └─ ic_launcher_foreground.webp
│        │
│        ├─ 📂 mipmap-xxxhdpi\           ← O AQUÍ (para Iconos)
│        │  ├─ ic_launcher.webp          (192x192 px)
│        │  ├─ ic_launcher_round.webp    (192x192 px)
│        │  └─ ic_launcher_foreground.webp
│        │
│        ├─ 📂 mipmap-anydpi-v26\
│        │  └─ (archivo XML de configuración)
│        │
│        ├─ 📂 values\
│        │  └─ (archivos de colores, strings, etc.)
│        │
│        └─ (otras carpetas res...)
│
└─ 📂 (otras carpetas del proyecto...)
```

---

## Resumen para Copiar/Pegar la Ruta

### SPLASH SCREEN - Ruta Exacta:
```
C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app\app\src\main\res\drawable\relieve.png
```

**Que significa:**
- Carpeta del proyecto → `PiratesdAndromeda\app\`
- Módulo → `app\`
- Recursos → `src\main\res\`
- Drawable → `drawable\`
- Tu imagen → **`relieve.png`**

### ICONOS - Rutas para Cada Densidad:
```
C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app\app\src\main\res\mipmap-mdpi\ic_launcher.webp
C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app\app\src\main\res\mipmap-hdpi\ic_launcher.webp
C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app\app\src\main\res\mipmap-xhdpi\ic_launcher.webp
C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app\app\src\main\res\mipmap-xxhdpi\ic_launcher.webp
C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app\app\src\main\res\mipmap-xxxhdpi\ic_launcher.webp
```

---

## ¿Qué Es Cada Carpeta?

### `drawable/`
- Guardar imágenes y recursos gráficos generales
- **Aquí va la Splash Screen** (`relieve.png`)
- Se usa en una sola resolución y Android la escala automáticamente

### `mipmap-*` (diferentes densidades)
- Iconos de la aplicación
- Cada carpeta es para una densidad de pantalla diferente:
  - `mdpi`: ~160 dpi (densidad base)
  - `hdpi`: ~240 dpi
  - `xhdpi`: ~320 dpi
  - `xxhdpi`: ~480 dpi
  - `xxxhdpi`: ~640 dpi
  
**Android elige automáticamente** la carpeta correcta según el dispositivo

---

## Tamaños Recomendados para Cada Carpeta

| Carpeta | Tamaño en px | Caso de uso |
|---------|-------------|-----------|
| `drawable/` | 1024x1024 px | Splash Screen |
| `mipmap-mdpi` | 48x48 px | Phones con densidad base |
| `mipmap-hdpi` | 72x72 px | Phones antiguos |
| `mipmap-xhdpi` | 96x96 px | Phones comunes |
| `mipmap-xxhdpi` | 144x144 px | Phones de alta densidad |
| `mipmap-xxxhdpi` | 192x192 px | Phones de muy alta densidad |

---

## Cómo Navegar en el Explorador de Archivos

1. Abre `Explorador de Archivos`
2. Ve a: `C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app\app\src\main\res\drawable\`
3. Verás la carpeta `drawable`
4. Pero NO USES EL EXPLORADOR - Es más fácil usar Android Studio:

### Mejor Opción en Android Studio:

1. Abre **Android Studio**
2. En el panel izquierdo, abre: `app` → `src` → `main` → `res` → `drawable`
3. **Clic derecho** en la carpeta → `Paste` o arrastra tu archivo

---

## Scripts Disponibles

### `setup_app_icon_windows.ps1` (PowerShell)
- Requiere ImageMagick instalado
- Genera automáticamente todos los tamaños
- Ejecutar desde: `C:\Users\hnnzp\AndroidStudioProjects\PiratesdAndromeda\app\`

### `setup_app_icon_windows.bat` (Batch)
- Igual que arriba pero formato .bat
- Doble clic para ejecutar
- Requiere ImageMagick instalado

---

## Notas Importantes

### Splash Screen (`drawable/relieve.png`)
✅ Android la escala automáticamente  
✅ Se usa tal como está sin procesar  
✅ No necesita coincidir exactamente con 1024x1024 px  
✅ Se activa automáticamente - NO necesitas código  

### Iconos (mipmap-*/ic_launcher.*)
✅ Android elige el tamaño correcto según el dispositivo  
✅ Deben estar en TODAS las carpetas mipmap  
❌ NO puedes usar solo una densidad  
✅ Los scripts o Android Studio generan todos los tamaños  

---

## ✨ Simplificado

### Tú haces esto:
1. Coloca `relieve.png` en `drawable/` ✨
2. Genera/coloca los iconos en los `mipmap-*/` 🎨
3. Sincroniza Android Studio
4. Ejecuta la app

### Android hace esto:
- Lee `relieve.png` y la muestra en la Splash Screen ✅
- Lee el icono correcto de `mipmap-*/` según el dispositivo ✅
- Todo funciona automáticamente ✅


