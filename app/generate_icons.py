#!/usr/bin/env python3
"""
Script para generar iconos en múltiples tamaños desde una imagen base.
Requiere: Pillow (PIL)

Instalación:
pip install Pillow
"""

import os
import sys
from PIL import Image

# Colores del proyecto
BACKGROUND_COLOR = (210, 172, 94)  # #D2AC5E en RGB

# Definir tamaños para cada densidad
ICON_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

def generate_icons(image_path):
    """Genera los iconos en múltiples tamaños."""

    # Verificar que la imagen existe
    if not os.path.exists(image_path):
        print(f"❌ Error: No se encontró la imagen: {image_path}")
        return False

    # Abrir la imagen
    try:
        img = Image.open(image_path)
        print(f"✅ Imagen cargada: {image_path}")
        print(f"   Tamaño original: {img.size}")
    except Exception as e:
        print(f"❌ Error al abrir la imagen: {e}")
        return False

    # Ruta base
    base_path = "app/src/main/res"

    success = True

    for density, size in ICON_SIZES.items():
        # Crear carpeta si no existe
        folder = os.path.join(base_path, f"mipmap-{density}")
        os.makedirs(folder, exist_ok=True)

        # Redimensionar imagen
        try:
            # Crear imagen con fondo
            resized = Image.new('RGBA', (size, size), BACKGROUND_COLOR + (255,))

            # Convertir imagen de entrada a RGBA si es necesario
            if img.mode != 'RGBA':
                img_rgba = img.convert('RGBA')
            else:
                img_rgba = img

            # Redimensionar manteniendo aspecto
            img_rgba.thumbnail((size, size), Image.Resampling.LANCZOS)

            # Pegar en el centro
            offset = ((size - img_rgba.size[0]) // 2,
                     (size - img_rgba.size[1]) // 2)
            resized.paste(img_rgba, offset, img_rgba)

            # Guardar como PNG
            output_path = os.path.join(folder, "ic_launcher.png")
            resized.save(output_path, 'PNG')
            print(f"✅ Generado: {output_path} ({size}x{size}px)")

        except Exception as e:
            print(f"❌ Error generando {density}: {e}")
            success = False

    return success

if __name__ == "__main__":
    # Buscar relieve.png
    image_file = "relieve.png"
    if not os.path.exists(image_file):
        print(f"❌ No se encontró {image_file} en la carpeta actual: {os.getcwd()}")
        print("\nPor favor, coloca relieve.png en esta carpeta:")
        print(f"   {os.getcwd()}")
        sys.exit(1)

    print("=" * 60)
    print("GENERADOR DE ICONOS - PIRATAS DE ANDROMEDA")
    print("=" * 60)
    print()

    if generate_icons(image_file):
        print()
        print("=" * 60)
        print("✅ ¡Iconos generados exitosamente!")
        print("=" * 60)
        print()
        print("Próximos pasos:")
        print("1. Abre Android Studio")
        print("2. Build → Sync Now")
        print("3. Build → Clean Project")
        print("4. Build → Rebuild Project")
        print("5. Shift + F10 para ejecutar")
        print()
    else:
        print()
        print("❌ Error al generar los iconos")
        sys.exit(1)

