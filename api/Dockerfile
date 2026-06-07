# syntax=docker/dockerfile:1
# Construir con el código actual de piratas-server (contexto = esta carpeta):
#   docker build -t piratas-andromeda-api .
#
# En compose, si usas build: ./backend, esa carpeta debe ser una copia actualizada de este repo;
# de lo contrario seguirás con la imagen antigua (p. ej. MissingGreenlet con mysql+aiomysql).
#
# DATABASE_URL recomendada (motor síncrono):
#   mysql+pymysql://USER:PASS@HOST:3306/DATABASE
# Ejecutar (ejemplo):
#   docker run --rm -p 8000:8000 \
#     -e DATABASE_URL="mysql+pymysql://user:pass@host:3306/andromeda_db" \
#     -e MQTT_HOST=mosquitto \
#     piratas-andromeda-api

FROM python:3.12-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

# Dependencias del proyecto
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Código de la aplicación
COPY . .

RUN useradd --create-home --uid 10001 appuser \
    && chown -R appuser:appuser /app

USER appuser

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/', timeout=3)"]

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
