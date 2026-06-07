# Pirates of Andromeda

**Pirates of Andromeda** (*Piratas de Andromeda*) is a cooperative escape-room experience where a crew must repair a hostile spaceship, complete missions, defuse sabotages, and uncover impostors — all in real time. The project combines a mobile Android client, a FastAPI backend with WebSockets and MQTT, a React web dashboard for live game monitoring, and optional ESP32-based physical devices for in-person sessions.

Production API: [https://api.piratasandromeda.me](https://api.piratasandromeda.me)

---

## Features

- **User accounts** — Register and log in from the Android app.
- **Game lobby** — Create or join a game with a short room code; configure rooms, characters, and mission settings.
- **Real-time gameplay** — WebSocket channels for player actions, emergency meetings, voting, sabotages, and score updates.
- **Social deduction** — Hidden impostors, eliminations, and crew voting during emergency meetings.
- **Mission system** — Start, complete, sabotage, and repair missions; track ship repair progress toward victory.
- **Live monitor** — Web dashboard that connects to `/ws/monitor/{game_code}` to follow events, scores, and player status.
- **IoT integration** — ESP32 devices publish mission events over MQTT for physical, in-person missions (NFC, LEDs, buttons).

---

## Architecture

```text
┌─────────────────┐     REST / WebSocket      ┌──────────────────┐
│  Android App    │ ◄────────────────────────► │  FastAPI Backend │
│  (Kotlin)       │                            │  + MySQL/SQLite  │
└─────────────────┘                            └────────┬─────────┘
                                                        │
┌─────────────────┐     WebSocket (monitor)             │ MQTT
│  Web Dashboard  │ ◄─────────────────────────────────┤
│  (React + Vite) │                                     │
└─────────────────┘                            ┌────────▼─────────┐
                                               │  ESP32 Devices   │
                                               │  (Arduino)       │
                                               └──────────────────┘
```

| Component | Stack | Role |
|-----------|-------|------|
| [`app/`](app/) | Kotlin, Android SDK, Retrofit, Room | Player-facing mobile client |
| [`api/`](api/) | Python, FastAPI, SQLModel, WebSockets, aiomqtt | Game server, persistence, real-time events |
| [`web/`](web/) | React 19, Vite, Material UI | Promotional site and live game monitor |
| [`arduino/`](arduino/) | C++ (ESP32), MFRC522, PubSubClient | Physical mission stations for in-person games |

---

## Project structure

```text
PiratesdAndromeda/
├── api/                 # FastAPI backend
│   ├── main.py          # App entry point, WebSocket handlers
│   ├── endpoints/       # REST routes (games, users)
│   ├── services/        # Business logic
│   ├── CRUD/            # Database operations
│   ├── models/          # SQLModel entities
│   ├── core/            # WebSocket manager, MQTT client
│   └── db/              # Database connection and seed data
├── app/                 # Android application (Gradle project root)
│   └── app/             # Android module source
├── web/                 # React web frontend
├── arduino/             # ESP32 firmware for physical missions
└── docs/                # Additional documentation
```

---

## Prerequisites

| Component | Requirements |
|-----------|--------------|
| **API** | Python 3.12+, MySQL (recommended) or SQLite for local dev |
| **Android app** | Android Studio, JDK 11+, Android SDK 36, min device API 26 |
| **Web** | Node.js 18+, npm |
| **Arduino** | Arduino IDE or PlatformIO, ESP32 board, MFRC522 RFID module |

---

## Getting started

### 1. Backend (API)

```bash
cd api
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

Create a `.env` file in `api/` (optional for local SQLite; required for production):

```env
DATABASE_URL=mysql+pymysql://user:password@localhost:3306/andromeda_db
MQTT_USER=your_mqtt_user
PASSWORD=your_mqtt_password
```

Run the server:

```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at `http://localhost:8000`. OpenAPI docs: `http://localhost:8000/docs`.

**Docker:**

```bash
cd api
docker build -t piratas-andromeda-api .
docker run --rm -p 8000:8000 \
  -e DATABASE_URL="mysql+pymysql://user:pass@host:3306/andromeda_db" \
  -e MQTT_USER=pirata \
  -e PASSWORD=your_password \
  piratas-andromeda-api
```

> Use a synchronous MySQL driver in `DATABASE_URL` (`mysql+pymysql://…`). Async drivers such as `mysql+aiomysql://` are converted automatically but `pymysql` is recommended.

---

### 2. Web dashboard

```bash
cd web
npm install
npm run dev
```

For local development against a local API, create `web/.env`:

```env
VITE_BACKEND_URL=http://localhost:8000
VITE_APP_DOWNLOAD_URL=https://your-app-download-url
```

Build for production:

```bash
npm run build
npm run preview
```

Open the monitor at `/monitorizaje`, enter a game code, and connect to the live WebSocket feed. You can also pass query parameters: `?game=ABCD&api=http://localhost:8000`.

---

### 3. Android app

1. Open the `app/` folder in Android Studio.
2. Sync Gradle and connect a device or emulator (API 26+).
3. Run the **debug** build (`Shift+F10`).

The app connects to the production API by default (`https://api.piratasandromeda.me`). To point at a local server, change `BASE_HTTP_URL` and `BASE_WS_URL` in `app/app/build.gradle.kts`.

Build a debug APK from the command line:

```bash
cd app
./gradlew assembleDebug
```

The APK is generated under `app/app/build/outputs/apk/debug/`.

For splash screen and launcher icon customization, see [`app/SETUP_RAPIDO.md`](app/SETUP_RAPIDO.md).

---

### 4. Arduino / ESP32 devices

Firmware lives in [`arduino/atrapa_verde_definitivo/`](arduino/atrapa_verde_definitivo/). Each device:

- Connects to Wi-Fi and an MQTT broker
- Reads NFC tags to identify players
- Publishes mission events (`started`, `completed`) to MQTT topics consumed by the API

Before flashing, update Wi-Fi credentials, MQTT broker settings, and client ID in the `.ino` file. See [`arduino/README.md`](arduino/README.md) for details.

---

## API overview

### REST endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/users/register` | Create a new user account |
| `POST` | `/users/login` | Log in |
| `POST` | `/games/create` | Create a new game session |
| `POST` | `/games/join` | Join an existing game |
| `POST` | `/games/leave` | Leave a game |
| `POST` | `/games/start` | Start the game (host) |
| `POST` | `/games/end` | End the game (host) |

### WebSocket endpoints

| Path | Client | Description |
|------|--------|-------------|
| `/ws/join/{game_code}/{ws_code}` | Android app | Player channel for in-game actions |
| `/ws/monitor/{game_code}` | Web monitor | Read-only live event stream for a game |

Player WebSocket actions include: `inicio_partida`, `fin_partida`, `mision_iniciada`, `mision_completada`, `mision_saboteada`, `mision_desaboteada`, `reunion_emergencia`, `voto`, `jugador_eliminado`, and `salir`.

---

## Game flow

1. **Register / log in** on the Android app.
2. **Create a game** (host) or **join** with a room code.
3. **Configure** rooms, characters, and settings in the lobby.
4. **Connect** — each player receives a `ws_code` token for the WebSocket session.
5. **Start** — the host triggers `inicio_partida`; missions are distributed and impostors are assigned.
6. **Play** — crew completes missions and repairs the ship; impostors sabotage and eliminate players; anyone can call an emergency meeting to vote.
7. **End** — the host ends the game when crew or impostors win; the web monitor shows a read-only summary for finished games.

In-person games (`presencial: true`) can require physical device confirmation before sabotages are applied.

---

## Environment variables

| Variable | Component | Description |
|----------|-----------|-------------|
| `DATABASE_URL` | API | SQLAlchemy connection string (MySQL recommended) |
| `MQTT_USER` | API | MQTT broker username |
| `PASSWORD` | API | MQTT broker password |
| `VITE_BACKEND_URL` | Web | Override API base URL (dev) |
| `VITE_APP_DOWNLOAD_URL` | Web | App download link for QR page |

MQTT topics follow the pattern `juego/devices/#` (device events) and `juego/commands/#` (server commands).

---

## Additional documentation

| Document | Description |
|----------|-------------|
| [`app/SETUP_RAPIDO.md`](app/SETUP_RAPIDO.md) | Quick setup for splash screen and app icon |
| [`CAMBIOS_IMPLEMENTADOS.md`](CAMBIOS_IMPLEMENTADOS.md) | WebSocket and connection implementation notes |
| [`ESTRUCTURA_CARPETAS.md`](ESTRUCTURA_CARPETAS.md) | Folder structure reference (Spanish) |
| [`INDICE_DOCUMENTOS.md`](INDICE_DOCUMENTOS.md) | Index of all project documents |

---

## License

No license file is included in this repository. Contact the project maintainers for usage terms.
