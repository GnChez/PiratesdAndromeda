# 📱 Instrucciones para Configurar tu Imagen Personalizada

## ¿Qué se va a cambiar?
Tu proyecto ya está completamente preparado para usar tu imagen "relieve.png" en:
1. **Splash Screen** - La pantalla que aparece al iniciar la app
2. **Icono de la Aplicación** - El icono que aparece en el menú de aplicaciones

---

## 📋 Paso 1: Preparar tu imagen

### Requisitos de la imagen:
- **Formato**: PNG (con fondo transparente si es posible)
- **Nombre**: Puede ser cualquiera, pero te recomendamos `relieve.png`
- **Tamaños recomendados**: 
  - Para **Splash Screen**: 1000x1000 px mínimo (formato cuadrado ideal)
  - Para **Icono de App**: 512x512 px
  
> **💡 Consejo**: Si tienes una sola imagen, úsala para ambos. El sistema se encargará de escalarla automáticamente.

---

## 🎯 Paso 2: Colocar la imagen en el proyecto

### Para la Splash Screen:

1. **Localización**: `app\app\src\main\res\drawable\`
2. **Nombre del archivo**: `relieve.png`
3. **Instrucciones**:
   - Ve a la carpeta: `PiratesdAndromeda\app\app\src\main\res\drawable\`
   - Reemplaza el archivo `relieve.png` con tu imagen
   - Si tu imagen tiene otro nombre, renómbrala a `relieve.png`

**La Splash Screen ya está configurada** ✅

---

## 🔧 Paso 3: Actualizar el Icono de la Aplicación

Tienes dos opciones:

### **OPCIÓN A: Automática (Recomendado) - Usar el script**

Si usas Windows y tienes ImageMagick instalado:

1. Coloca tu imagen en: `PiratesdAndromeda\app\`
2. Ejecuta: `setup_app_icon_windows.ps1` (haz doble clic)
3. El script se encargará de generar todos los tamaños automáticamente

### **OPCIÓN B: Manual - Usando Android Studio**

1. Abre el proyecto en Android Studio
2. Ve a: `File → New → Image Asset`
3. Selecciona:
   - **Image file** (la opción con imagen)
   - Haz clic en el botón "..." y selecciona tu `relieve.png`
   - Completa el asistente
4. Android Studio generará automáticamente todos los tamaños necesarios

### **OPCIÓN C: Manual - Usando online**

Si no quieres instalar nada:

1. Ve a: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
2. Sube tu imagen `relieve.png`
3. Descarga los assets generados
4. Copia el contenido de cada carpeta `mipmap-*` en:
   - `app\app\src\main\res\mipmap-mdpi\`
   - `app\app\src\main\res\mipmap-hdpi\`
   - `app\app\src\main\res\mipmap-xhdpi\`
   - `app\app\src\main\res\mipmap-xxhdpi\`
   - `app\app\src\main\res\mipmap-xxxhdpi\`

---

## ✅ Paso 4: Verificar que todo funciona

1. Abre el proyecto en **Android Studio**
2. Haz clic en `Sync Now` (si aparece)
3. Ejecuta la app:
   - **F5** o `Shift + F10` para correr en el emulador
   - O conecta un dispositivo físico

Deberías ver:
- Al iniciar: Tu imagen en la **Splash Screen** 🎬
- En el menú de apps: Tu imagen como **Icono** 🔲

---

## 📁 Resumen de Ubicaciones

```
PiratesdAndromeda/
├── app/
│   ├── app/
│   │   └── src/
│   │       └── main/
│   │           └── res/
│   │               ├── drawable/
│   │               │   └── relieve.png          ← SPLASH SCREEN
│   │               ├── mipmap-mdpi/
│   │               │   ├── ic_launcher.webp    ← ICONO (mdpi)
│   │               │   └── ic_launcher_round.webp
│   │               ├── mipmap-hdpi/
│   │               │   ├── ic_launcher.webp    ← ICONO (hdpi)
│   │               │   └── ic_launcher_round.webp
│   │               ├── mipmap-xhdpi/
│   │               ├── mipmap-xxhdpi/
│   │               └── mipmap-xxxhdpi/
│   └── setup_app_icon_windows.ps1              ← SCRIPT AUTOMÁTICO
```

---

## 🎨 Notas Importantes

### Splash Screen (`relieve.png` en drawable):
- ✅ Se usa automáticamente al iniciar la app
- ✅ Android la escala automáticamente para cualquier pantalla
- ✅ El tema está configurado en `themes.xml`

### Icono de Aplicación (mipmap-*):
- ✅ Necesita múltiples tamaños para diferentes densidades de pantalla
- ✅ Recomendamos usar el **script automático** o **Android Asset Studio**
- ✅ El sistema usará automáticamente el tamaño correcto para cada dispositivo

---

## 🆘 Solución de Problemas

**La Splash Screen no cambia:**
- Verifica que el archivo sea `relieve.png` (minúsculas)
- Debe estar en: `app/src/main/res/drawable/`
- Limpia y reconstruye: `Build → Clean Project` + `Build → Rebuild Project`

**El Icono no cambia:**
- Verifica que los archivos estén en las carpetas correctas
- Limpia y reconstruye el proyecto
- En Android Studio, presiona `Shift + F10` para reinstalar la app

**Error al ejecutar el script:**
- Instala ImageMagick desde aquí: https://imagemagick.org/script/download.php
- O usa las opciones Manual u Online

---

## 💡 Recomendaciones

### Para mejores resultados:

1. **Imagen con fondo transparente**:
   - Usa PNG con canal alpha (transparencia)
   - El fondo de la splash es naranja (#E47B1B)
   - El icono tendrá un fondo dorado (#D2AC5E)

2. **Tamaños ideales**:
   - Splash Screen: 1024x1024 px (será escalada automáticamente)
   - Icono: 512x512 px (el script/AssetStudio generará los demás tamaños)

3. **Prueba en diferentes dispositivos**:
   - Prueba en phones con diferentes densidades
   - Prueba en tablets (pantallas grandes)

---

## 📞 Próximos Pasos

Una vez hayas colocado la imagen:
1. Sincroniza el proyecto en Android Studio
2. Ejecuta en el emulador o dispositivo
3. Verifica que la Splash Screen y el Icono muestren tu imagen

¡Listo! Tu app ya tendrá tu imagen personalizada 🚀


