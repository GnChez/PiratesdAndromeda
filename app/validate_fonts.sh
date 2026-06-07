#!/bin/bash
# Script de validación para el error de recursos duplicados

echo "================================"
echo "Validación de Recursos de Fuentes"
echo "================================"
echo ""

RES_FONT_DIR="app/src/main/res/font"

echo "📂 Archivos en $RES_FONT_DIR:"
ls -la "$RES_FONT_DIR" 2>/dev/null || echo "Directorio no encontrado"

echo ""
echo "Buscando conflictos potenciales..."
echo ""

if [ -f "$RES_FONT_DIR/pirata.xml" ]; then
    echo "⚠️  ADVERTENCIA: $RES_FONT_DIR/pirata.xml aún existe"
    echo "   Este archivo será eliminado automáticamente durante el build"
else
    echo "✅ No se encontró pirata.xml (conflicto resoluto)"
fi

if [ -f "$RES_FONT_DIR/pirata.ttf" ]; then
    echo "✅ pirata.ttf encontrado (fuente real)"
else
    echo "❌ ERROR: pirata.ttf no encontrado"
fi

if [ -f "$RES_FONT_DIR/pirata_regular.ttf" ]; then
    echo "✅ pirata_regular.ttf encontrado"
else
    echo "⚠️  ADVERTENCIA: pirata_regular.ttf no encontrado"
fi

if [ -f "$RES_FONT_DIR/pirata_font.xml" ]; then
    echo "✅ pirata_font.xml encontrado (familia de fuentes)"
else
    echo "⚠️  ADVERTENCIA: pirata_font.xml no encontrado"
fi

echo ""
echo "Buscando referencias a @font/pirata en el proyecto..."
grep -r "@font/pirata" --include="*.xml" --include="*.kt" app/ 2>/dev/null | wc -l | xargs echo "   Encontradas referencias:"

echo ""
echo "================================"
echo "✓ Validación completada"
echo "================================"

