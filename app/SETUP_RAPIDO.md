# 🚀 CONFIGURACIÓN RÁPIDA: Splash Screen e Icono Personalizado

## ¿Qué hay que hacer?

Tu proyecto ya está **100% preparado** para usar tu imagen personalizada. Solo necesitas:

### Paso 1: Splash Screen (5 segundos) ✨
1. Coloca tu `relieve.png` en: **`app\src\main\res\drawable\relieve.png`**
2. ¡Listo! Se mostrará automáticamente al iniciar la app

### Paso 2: Icono de App (elige una opción) 🎨

**Opción A: Automática (Si tienes ImageMagick)**
```bash
1. Coloca relieve.png aquí: PiratesdAndromeda\app\
2. Ejecuta: setup_app_icon_windows.ps1 (doble clic)
3. El script genera automáticamente todos los tamaños
```

**Opción B: Usar Android Studio (Recomendado)**
```
1. File → New → Image Asset
2. Selecciona Image file y elige tu relieve.png
3. Android Studio genera todo automáticamente
```

**Opción C: Online (Sin instalar nada)**
```
1. Ve a: https://romannunik.github.io/AndroidAssetStudio/icons-launcher.html
2. Sube tu relieve.png
3. Descarga y copia los archivos en: app\src\main\res\mipmap-*\
```

---

## 📋 Estructura de Carpetas Necesarias

```
app/
└── src/main/
    └── res/
        ├── drawable/
        │   └── relieve.png ← AQUÍ VA TU IMAGEN (splash)
        ├── mipmap-mdpi/
        ├── mipmap-hdpi/
        ├── mipmap-xhdpi/
        ├── mipmap-xxhdpi/
        └── mipmap-xxxhdpi/
```

---

## 📖 Para más detalles

Lee: **`INSTRUCCIONES_IMAGEN_PERSONALIZADA.md`** (en la raíz del proyecto)

Contiene:
- ✅ Requisitos de la imagen
- ✅ Instrucciones paso a paso
- ✅ Solución de problemas
- ✅ Tamaños recomendados
- ✅ Notas importantes

---

## ⚡ TL;DR (Muy Rápido)

```
1. relieve.png → drawable/relieve.png
2. Usa Android Studio: File → New → Image Asset
3. Sincroniza y ejecuta
4. ¡Listo!
```

---

## 💡 Requisitos Mínimos de la Imagen

- Formato: **PNG** (preferible con fondo transparente)
- Tamaño mínimo: **512x512 px**
- Ratio: **Cuadrado (1:1)** es lo mejor

---

## ❓ ¿Necesitas Ayuda?

Si algo no funciona:
1. Limpia el proyecto: `Build → Clean Project`
2. Reconstruye: `Build → Rebuild Project`
3. Reinstala: `Shift + F10`
4. Lee el archivo detallado: `INSTRUCCIONES_IMAGEN_PERSONALIZADA.md`

---

## ✨ ¡Listo para Empezar!

Tu proyecto está completamente configurado. Solo falta que agregues tu imagen y todo funcionará automáticamente.


