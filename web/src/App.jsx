import React, { useMemo, useState } from "react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import Link from "@mui/material/Link";
import Divider from "@mui/material/Divider";
import { Link as RouterLink, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import AppAppBar from "./components/AppBar";
import GameMonitor from "./components/GameMonitor";
import { getApiBaseFromEnvOrDefault, getWsBaseFromHttpApi } from "./config";

const DOWNLOAD_URL =
  import.meta.env.VITE_APP_DOWNLOAD_URL || "https://example.com/piratas-app";

const buildQrUrl = (value) =>
  `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(value)}`;

const getQueryParams = () => {
  const params = new URLSearchParams(window.location.search);
  return {
    gameCode: params.get("game") || "",
    apiBase: params.get("api") || "",
  };
};

const getApiBaseUrl = (queryApiBase) => getApiBaseFromEnvOrDefault(queryApiBase);

const getWsBaseUrl = (apiBase) => getWsBaseFromHttpApi(apiBase);

function HomePage() {
  return (
    <Box sx={{ px: 2, py: { xs: 4, md: 7 } }}>
      <Paper
        elevation={0}
        sx={{
          mx: "auto",
          maxWidth: 900,
          p: { xs: 3, md: 5 },
          background: "linear-gradient(135deg, #1A0806 0%, #2A0A08 100%)",
          border: "1px solid #5C2A1A",
          borderRadius: 2,
        }}
      >
        <Stack spacing={2.2}>
          <Typography variant="h3" sx={{ color: "text.primary", fontSize: { xs: "2rem", md: "2.5rem" } }}>
            Escape Room Piratas de Andromeda
          </Typography>
          <Typography sx={{ color: "text.primary", fontSize: "1.08rem" }}>
            Vive una mision cooperativa en la que la tripulacion debe escapar de una nave hostil resolviendo enigmas,
            desactivando sabotajes y coordinando decisiones bajo presion.
          </Typography>
          <Typography sx={{ color: "text.secondary" }}>
            Reserva tu sesion, prepara a tu equipo y descubre una experiencia inmersiva con narrativa en tiempo real.
          </Typography>
          <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1.2, pt: 1 }}>
            <Button component={RouterLink} to="/monitorizaje" variant="contained">
              Abrir monitorizaje
            </Button>
            <Button component={RouterLink} to="/tutorial" variant="outlined" color="primary">
              Ver tutorial
            </Button>
          </Box>
        </Stack>
      </Paper>
    </Box>
  );
}

function TutorialPage() {
  return (
    <Box sx={{ px: 2, py: 4 }}>
      <Paper elevation={0} sx={{ mx: "auto", maxWidth: 720, p: { xs: 2.5, md: 4 }, borderRadius: 2 }}>
        <Typography variant="h3" sx={{ fontSize: "2rem", mb: 2 }}>Tutorial rapido</Typography>
        <Typography sx={{ color: "text.primary", mb: 1.5 }}>1. Ve a la seccion Monitorizaje del menu superior.</Typography>
        <Typography sx={{ color: "text.primary", mb: 1.5 }}>2. Introduce el codigo de partida y pulsa "Entrar al monitor".</Typography>
        <Typography sx={{ color: "text.primary" }}>
          3. La vista se conectara a <code>/ws/monitor/{`{game_code}`}</code> para seguir solo los eventos de esa partida.
        </Typography>
      </Paper>
    </Box>
  );
}

function QrPage() {
  const qrUrl = useMemo(() => buildQrUrl(DOWNLOAD_URL), []);
  return (
    <Box sx={{ px: 2, py: 4 }}>
      <Paper elevation={0} sx={{ mx: "auto", maxWidth: 460, p: { xs: 2.5, md: 4 }, borderRadius: 2 }}>
        <Stack spacing={1.5} alignItems="center">
          <Typography variant="h3" sx={{ fontSize: "2rem" }}>Descargar aplicacion</Typography>
          <img src={qrUrl} alt="QR descarga app" width={220} height={220} />
          <Typography sx={{ textAlign: "center", color: "text.primary" }}>Escanea el QR para descargar la app:</Typography>
          <Link href={DOWNLOAD_URL} target="_blank" rel="noreferrer" sx={{ color: "secondary.light", wordBreak: "break-all" }}>
            {DOWNLOAD_URL}
          </Link>
        </Stack>
      </Paper>
    </Box>
  );
}

function AboutPage() {
  return (
    <Box sx={{ px: 2, py: 4 }}>
      <Paper elevation={0} sx={{ mx: "auto", maxWidth: 760, p: { xs: 2.5, md: 4 }, borderRadius: 2 }}>
        <Typography variant="h3" sx={{ fontSize: "2rem", mb: 2 }}>About us</Typography>
        <Typography sx={{ color: "text.primary" }}>
          Piratas de Andromeda es una experiencia cooperativa de misiones, sabotajes y estrategia en tiempo real.
        </Typography>
        <Typography sx={{ color: "text.primary", mt: 1.5 }}>
          Esta pantalla web incluye informacion promocional y un panel de monitorizaje para seguir cada partida en vivo.
        </Typography>
      </Paper>
    </Box>
  );
}

function MonitorPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const initialParams = getQueryParams();
  const apiBaseUrl = getApiBaseUrl(initialParams.apiBase);
  const wsBaseUrl = getWsBaseUrl(apiBaseUrl);
  const initialCode = initialParams.gameCode.toUpperCase();
  const [gameCodeInput, setGameCodeInput] = useState(initialCode);
  const [selectedGameCode, setSelectedGameCode] = useState(initialCode);

  const monitorWsUrl = selectedGameCode
    ? `${wsBaseUrl}/ws/monitor/${encodeURIComponent(selectedGameCode)}`
    : "";
  const isMonitoring = Boolean(selectedGameCode && monitorWsUrl);

  const openMonitor = () => {
    const normalizedGameCode = gameCodeInput.trim().toUpperCase();
    if (!normalizedGameCode) return;
    setSelectedGameCode(normalizedGameCode);
    const params = new URLSearchParams(location.search);
    params.set("game", normalizedGameCode);
    navigate(`/monitorizaje?${params.toString()}`);
  };

  const backToMonitorHome = () => {
    setSelectedGameCode("");
    const params = new URLSearchParams(location.search);
    params.delete("game");
    const query = params.toString();
    navigate(`/monitorizaje${query ? `?${query}` : ""}`);
  };

  return (
    <Box sx={{ px: 2, py: 4 }}>
      {!isMonitoring ? (
        <Paper elevation={0} sx={{ mx: "auto", maxWidth: 720, p: { xs: 2.5, md: 4 }, borderRadius: 2 }}>
          <Stack spacing={2}>
            <Typography variant="h3" sx={{ fontSize: "2rem" }}>Acceder al monitor de partida</Typography>
            <Typography sx={{ color: "text.primary", fontSize: "1rem" }}>
              Introduce el codigo de partida para abrir la pantalla de monitorizacion en tiempo real.
            </Typography>
            <TextField
              label="Codigo de partida"
              value={gameCodeInput}
              onChange={(event) => setGameCodeInput(event.target.value)}
              placeholder="Ej: XR101P"
              autoComplete="off"
            />
            <Box sx={{ display: "flex", justifyContent: "flex-end" }}>
              <Button variant="contained" onClick={openMonitor} disabled={!gameCodeInput.trim()}>
                Entrar al monitor
              </Button>
            </Box>
          </Stack>
        </Paper>
      ) : (
        <GameMonitor gameCode={selectedGameCode} wsUrl={monitorWsUrl} onBack={backToMonitorHome} />
      )}
    </Box>
  );
}

function Footer() {
  return (
    <Box component="footer" sx={{ borderTop: "1px solid", borderColor: "divider", background: "#1A0806", mt: "auto" }}>
      <Box sx={{ maxWidth: 1200, mx: "auto", px: 3, py: 2 }}>
        <Typography sx={{ color: "text.secondary", fontSize: "0.92rem", textAlign: "center" }}>
          Piratas de Andromeda - Escape Room | Monitorizaje en tiempo real para cada partida.
        </Typography>
      </Box>
    </Box>
  );
}

function App() {
  return (
    <Box sx={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
      <AppAppBar />
      <Box sx={{ flex: 1 }}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/tutorial" element={<TutorialPage />} />
          <Route path="/qr-app" element={<QrPage />} />
          <Route path="/about-us" element={<AboutPage />} />
          <Route path="/monitorizaje" element={<MonitorPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Box>
      <Divider sx={{ borderColor: "divider" }} />
      <Footer />
    </Box>
  );
}

export default App;