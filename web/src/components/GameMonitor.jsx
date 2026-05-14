import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import LinearProgress from "@mui/material/LinearProgress";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

import { defaultMonitorWsUrl, getWsBaseFromHttpApi, DEFAULT_API_BASE } from "../config";

const COLORS = {
  bg: "#0A0302",
  panel: "#1A0806",
  border: "#5C2A1A",
  gold: "#B85C38",
  goldLight: "#E09070",
  parchment: "#F5CDB0",
  muted: "#C47A5A",
  red: "#E0756A",
  green: "#E09070",
};

const EVENT_LABELS = {
  GAME_STARTED: "Partida iniciada",
  START_MISSION: "Misión iniciada",
  COMPLETE_MISSION: "Misión completada",
  SABOTAGE: "Sabotaje",
  DESABOTAGE: "Desabotaje",
  PLAYER_DIED: "Jugador eliminado",
  PLAYER_JOINED: "Jugador unido",
  PLAYER_LEFT: "Jugador salió",
  VOTE_CAST: "Voto emitido",
  REUNION: "Reunión de emergencia",
  REUNION_FINISHED: "Reunión finalizada",
  GAME_ENDED: "Partida finalizada",
  MONITOR_READ_ONLY: "Resumen (solo lectura)",
  MONITOR_CONNECTED: "Monitor en sala",
  IOT_COMMAND: "Dispositivo IoT",
};

const SCORE_DEFAULTS = {
  COMPLETE_MISSION: 100,
  SABOTAGE: 75,
  VOTE_CAST: 10,
  PLAYER_DIED: -50,
};

const MISSION_EVENT_TYPES = new Set(["START_MISSION", "COMPLETE_MISSION", "SABOTAGE", "DESABOTAGE"]);

const EVENT_DESCRIPTIONS = {
  START_MISSION: "Misión iniciada",
  COMPLETE_MISSION: "Misión completada",
  SABOTAGE: "Misión saboteada",
  DESABOTAGE: "Misión desaboteada",
};

const nowTime = () =>
  new Date().toLocaleTimeString("es-ES", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });

/** Texto legible para eventos MQTT de dispositivos físicos (START_MISSION, etc.). */
const formatIotCommandDescription = (msg) => {
  const action = String(msg.action ?? "").trim().toLowerCase();
  const jugador = String(msg.username ?? "").trim() || "un jugador";
  const dispositivo = String(msg.nombre_juego ?? "").trim() || "un dispositivo";
  const missionDisplay = extractMissionDisplay(msg);
  const missionSuffix = missionDisplay ? ` en la misión ${missionDisplay}` : "";
  const VERB = {
    started: "iniciado",
    completed: "completado",
    sabotaged: "saboteado",
    desabotaged: "desaboteado",
  };
  const evento = VERB[action] || (msg.action ? `realizado la acción «${msg.action}»` : "enviado un evento");
  return `${jugador} en ${dispositivo} ha ${evento}${missionSuffix}`;
};

const formatSeconds = (seconds) => {
  if (seconds == null || !Number.isFinite(seconds)) return "—:—";
  const safe = Math.max(0, Math.floor(seconds));
  const mins = String(Math.floor(safe / 60)).padStart(2, "0");
  const secs = String(safe % 60).padStart(2, "0");
  return `${mins}:${secs}`;
};

/** Claves de nombre en mensajes WS (p. ej. PLAYER_JOINED: `player` = id, `nombre_usuario` / `player_name` = texto). */
const PLAYER_DISPLAY_NAME_KEYS = [
  "nombre_usuario",
  "player_name",
  "username",
  "usuario",
  "nombre_jugador",
];

const TARGET_DISPLAY_NAME_KEYS = ["target_player_name", "target_username", "target_nombre_usuario"];
const MISSION_DISPLAY_NAME_KEYS = ["mission_name", "nombre_mision"];

function firstNonEmptyString(obj, keys) {
  if (!obj || typeof obj !== "object") return "";
  for (const k of keys) {
    const v = obj[k];
    if (v != null && String(v).trim() !== "") return String(v).trim();
  }
  return "";
}

function extractPlayerDisplayName(msg) {
  return firstNonEmptyString(msg, PLAYER_DISPLAY_NAME_KEYS);
}

function extractTargetDisplayName(msg) {
  return firstNonEmptyString(msg, TARGET_DISPLAY_NAME_KEYS);
}

function extractMissionDisplay(msg) {
  const name = firstNonEmptyString(msg, MISSION_DISPLAY_NAME_KEYS);
  if (name) return name;
  const missionId = msg?.mission_id ?? msg?.id_mision_partida;
  if (missionId == null || String(missionId).trim() === "") return "";
  return `#${String(missionId).trim()}`;
}

const getWsUrl = (forcedWsUrl, gameCode) => {
  if (forcedWsUrl) return forcedWsUrl;
  const envUrl = import.meta.env.VITE_MONITOR_WS_URL;
  const qs = new URLSearchParams(window.location.search);
  const queryUrl = qs.get("ws");
  if (queryUrl) return queryUrl;
  if (envUrl) return envUrl;
  if (gameCode) return defaultMonitorWsUrl(gameCode);
  return getWsBaseFromHttpApi(DEFAULT_API_BASE);
};

const normalizeMessage = (raw) => {
  if (typeof raw === "string") {
    return { type: "RAW", text: raw };
  }
  if (raw && typeof raw === "object") {
    return raw;
  }
  return { type: "RAW", text: String(raw) };
};

const GameMonitor = ({ gameCode, wsUrl: wsUrlProp, onBack }) => {
  const wsUrl = useMemo(() => getWsUrl(wsUrlProp, gameCode), [gameCode, wsUrlProp]);
  const wsRef = useRef(null);
  const [status, setStatus] = useState("connecting");
  const [error, setError] = useState(null);
  const [events, setEvents] = useState([]);
  const [progress, setProgress] = useState(0);
  const [timeRemaining, setTimeRemaining] = useState(null);
  const [players, setPlayers] = useState({});
  const [running, setRunning] = useState(false);
  const [readOnlyInfo, setReadOnlyInfo] = useState(null);
  const playerDisplayNamesRef = useRef({});

  const statusInfo = {
    connecting: { label: "Conectando", color: COLORS.gold },
    connected: { label: "Conectado", color: COLORS.green },
    disconnected: { label: "Desconectado", color: COLORS.muted },
    error: { label: "Error", color: COLORS.red },
  }[status];

  const ranking = useMemo(
    () =>
      Object.values(players).sort((a, b) => {
        if (b.score !== a.score) return b.score - a.score;
        return String(a.name).localeCompare(String(b.name));
      }),
    [players],
  );

  const addScore = useCallback((playerId, playerName, points) => {
    const key = String(playerId ?? playerName ?? "desconocido");
    const fromRef = playerDisplayNamesRef.current[key];
    const name = playerName || fromRef || `Jugador ${key}`;
    setPlayers((prev) => {
      const current = prev[key] || { id: key, name, score: 0, connected: true };
      return {
        ...prev,
        [key]: {
          ...current,
          name,
          score: current.score + points,
          connected: true,
        },
      };
    });
  }, []);

  const ensurePlayer = useCallback((playerId, playerName, connected = true) => {
    if (playerId === undefined || playerId === null) return;
    const key = String(playerId);
    const fromRef = playerDisplayNamesRef.current[key];
    const name = playerName || fromRef || `Jugador ${key}`;
    setPlayers((prev) => {
      const current = prev[key];
      if (current) {
        return {
          ...prev,
          [key]: {
            ...current,
            name: current.name || name,
            connected,
          },
        };
      }
      return {
        ...prev,
        [key]: {
          id: key,
          name,
          score: 0,
          connected,
        },
      };
    });
  }, []);

  const appendEvent = useCallback((label, text) => {
    setEvents((prev) => [...prev.slice(-149), { time: nowTime(), label, text }]);
  }, []);

  const handleIncomingMessage = useCallback((msg) => {
    console.log(msg);
    
    if (msg.type === "ERROR") {
      setStatus("error");
      setError(msg.message || "No se pudo abrir el monitor.");
      return;
    }
    if (msg.error && !msg.type) {
      setStatus("error");
      setError(msg.error);
      return;
    }
    if (msg.type === "MONITOR_READ_ONLY") {
      setReadOnlyInfo({
        nombre: msg.nombre_partida,
        ganador: msg.ganador_tripulacion,
        fechaFin: msg.fecha_fin,
        mensaje: msg.mensaje,
      });
      setRunning(false);
      const raw = msg.porcentaje_reparacion_actual;
      const p = raw != null ? parseFloat(String(raw), 10) : NaN;
      if (Number.isFinite(p)) {
        setProgress(Math.max(0, Math.min(100, Math.round(p))));
      }
      appendEvent(
        EVENT_LABELS.MONITOR_READ_ONLY,
        msg.mensaje || "Partida terminada: resumen desde el servidor (sin eventos en vivo).",
      );
      return;
    }

    const rememberDisplayName = (playerId, displayName) => {
      if (playerId == null || displayName == null) return;
      const k = String(playerId).trim();
      const n = String(displayName).trim();
      if (!k || !n) return;
      playerDisplayNamesRef.current[k] = n;
    };

    if (msg.player_names && typeof msg.player_names === "object") {
      for (const [pid, nm] of Object.entries(msg.player_names)) {
        rememberDisplayName(pid, nm);
      }
    }

    const nameHint = extractPlayerDisplayName(msg);
    if (msg.type === "MONITOR_CONNECTED") {
      if (msg.game_in_progress === true) {
        setRunning(true);
      }
      if (Number.isFinite(msg.time_remaining)) {
        setTimeRemaining(Math.max(0, Math.floor(msg.time_remaining)));
      }
      if (Number.isFinite(msg.progress)) {
        setProgress(Math.max(0, Math.min(100, Math.floor(msg.progress))));
      }
      if (Array.isArray(msg.connected_players)) {
        for (const row of msg.connected_players) {
          const rowName = extractPlayerDisplayName(row);
          if (row.player != null && rowName) rememberDisplayName(row.player, rowName);
          const online = row.connected !== false;
          ensurePlayer(row.player, rowName || undefined, online);
        }
      }
    }

    if (msg.player != null && nameHint) {
      rememberDisplayName(msg.player, nameHint);
    }
    const targetNameHint = extractTargetDisplayName(msg);
    if (msg.target_player != null && targetNameHint) {
      rememberDisplayName(msg.target_player, targetNameHint);
    }

    const playerIdStr = msg.player != null ? String(msg.player) : null;
    const resolvedPlayerLabel = playerIdStr ? playerDisplayNamesRef.current[playerIdStr] || "" : "";

    const eventType = msg.type || msg.event || "EVENT";
    const eventLabel = EVENT_LABELS[eventType] || eventType;
    const playerSuffix =
      playerIdStr && resolvedPlayerLabel ? ` — ${resolvedPlayerLabel}` : "";
    const missionDisplay = MISSION_EVENT_TYPES.has(eventType) ? extractMissionDisplay(msg) : "";
    const missionSuffix = missionDisplay ? ` — Misión ${missionDisplay}` : "";
    const defaultEventText = EVENT_DESCRIPTIONS[eventType] || `Evento recibido (${eventLabel})`;
    const baseEventText = msg.text || msg.descripcion || defaultEventText;
    const isDeviceMissionEvent =
      MISSION_EVENT_TYPES.has(eventType) &&
      (msg.source === "mqtt" || msg.source === "app");
    const eventText =
      eventType === "IOT_COMMAND" || isDeviceMissionEvent
        ? formatIotCommandDescription(msg)
        : `${baseEventText}${missionSuffix}${playerSuffix}`;

    if (Number.isFinite(msg.time_remaining)) {
      setTimeRemaining(Math.max(0, Math.floor(msg.time_remaining)));
    }
    if (Number.isFinite(msg.remaining_time)) {
      setTimeRemaining(Math.max(0, Math.floor(msg.remaining_time)));
    }
    if (Number.isFinite(msg.progress)) {
      setProgress(Math.max(0, Math.min(100, Math.floor(msg.progress))));
    }

    if (msg.scores && typeof msg.scores === "object") {
      setPlayers((prev) => {
        const refMap = playerDisplayNamesRef.current;
        const nextPlayers = {};
        for (const [id, score] of Object.entries(msg.scores)) {
          const resolved =
            refMap[id] ||
            (prev[id] && prev[id].name && !String(prev[id].name).startsWith("Jugador ")
              ? prev[id].name
              : null) ||
            `Jugador ${id}`;
          nextPlayers[id] = {
            id,
            name: resolved,
            score: Number(score) || 0,
            connected: prev[id]?.connected ?? true,
          };
        }
        return nextPlayers;
      });
    }

    if (eventType === "GAME_STARTED") {
      setRunning(true);
    }

    if (eventType === "GAME_ENDED") {
      setRunning(false);
    }

    if (eventType === "COMPLETE_MISSION") {
      setProgress((prev) => Math.min(100, prev + 10));
    }
    if (eventType === "SABOTAGE") {
      setProgress((prev) => Math.max(0, prev - 8));
    }

    const scoreDelta = Number.isFinite(msg.score_delta)
      ? msg.score_delta
      : SCORE_DEFAULTS[eventType] ?? 0;
    if (scoreDelta !== 0 && msg.player) {
      addScore(msg.player, nameHint || undefined, scoreDelta);
    }

    if (eventType === "PLAYER_JOINED") {
      ensurePlayer(msg.player, nameHint || undefined, true);
    }
    if (eventType === "PLAYER_LEFT") {
      ensurePlayer(msg.player, nameHint || undefined, false);
    }

    appendEvent(eventLabel, eventText);
  }, [addScore, appendEvent, ensurePlayer]);

  useEffect(() => {
    setStatus("connecting");
    setError(null);
    setReadOnlyInfo(null);
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setStatus("connected");
      setError(null);
      setEvents((prev) => [
        ...prev.slice(-149),
        { time: nowTime(), label: "Sistema", text: "Conexión establecida" },
      ]);
    };

    ws.onmessage = (event) => {
      let payload;
      try {
        payload = JSON.parse(event.data);
      } catch {
        payload = normalizeMessage(event.data);
      }
      handleIncomingMessage(normalizeMessage(payload));
    };

    ws.onerror = () => {
      setStatus("error");
      setError("No se pudo conectar al WebSocket de monitorización.");
    };

    ws.onclose = () => {
      setRunning(false);
      setStatus((prev) => (prev === "error" ? prev : "disconnected"));
      setEvents((prev) => [
        ...prev.slice(-149),
        { time: nowTime(), label: "Sistema", text: "Conexión cerrada" },
      ]);
    };

    return () => ws.close();
  }, [handleIncomingMessage, wsUrl]);

  useEffect(() => {
    if (!running) return undefined;
    const timer = setInterval(() => {
      setTimeRemaining((prev) => {
        if (prev == null || !Number.isFinite(prev)) return prev;
        if (prev <= 1) {
          setRunning(false);
          setEvents((old) => [
            ...old.slice(-149),
            { time: nowTime(), label: "Sistema", text: "Tiempo agotado" },
          ]);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [running]);

  return (
    <Box
      sx={{
        minHeight: "100vh",
        background: COLORS.bg,
        p: { xs: 2, md: 3 },
        color: COLORS.parchment,
      }}
    >
      <Stack spacing={2.5}>
        <Paper
          elevation={0}
          sx={{
            p: 2.5,
            background: `linear-gradient(90deg, ${COLORS.panel} 0%, #2A0A08 100%)`,
            border: `1px solid ${COLORS.border}`,
            borderRadius: 2,
          }}
        >
          <Stack
            direction={{ xs: "column", md: "row" }}
            justifyContent="space-between"
            alignItems={{ xs: "flex-start", md: "center" }}
            spacing={1.5}
          >
            <Box>
              <Typography sx={{ color: COLORS.goldLight, fontSize: "1.8rem" }}>
                Monitor de Partida
              </Typography>
              <Typography sx={{ color: COLORS.muted, fontSize: "0.9rem" }}>
                {readOnlyInfo
                  ? "Resumen de una partida ya finalizada (sin tiempo real)"
                  : "Vista unica de seguimiento en tiempo real"}
              </Typography>
              {gameCode && (
                <Typography sx={{ color: COLORS.parchment, fontSize: "0.8rem", mt: 0.5 }}>
                  Codigo: {gameCode}
                </Typography>
              )}
            </Box>
            <Stack direction="row" spacing={1} alignItems="center">
              {onBack && (
                <Chip
                  label="Volver"
                  onClick={onBack}
                  clickable
                  sx={{
                    color: COLORS.parchment,
                    border: `1px solid ${COLORS.border}`,
                    background: "rgba(184,92,56,0.12)",
                  }}
                />
              )}
              <Chip
                label={statusInfo.label}
                sx={{
                  color: statusInfo.color,
                  border: `1px solid ${statusInfo.color}66`,
                  background: `${statusInfo.color}11`,
                }}
              />
            </Stack>
          </Stack>
          {error && (
            <Typography sx={{ mt: 1.25, color: COLORS.red, fontSize: "0.85rem" }}>
              {error}
            </Typography>
          )}
        </Paper>

        {readOnlyInfo && (
          <Paper
            elevation={0}
            sx={{
              p: 2,
              background: "#2A0A08",
              border: `1px solid ${COLORS.gold}`,
              borderRadius: 2,
            }}
          >
            <Typography sx={{ color: COLORS.goldLight, fontWeight: 700, mb: 0.75 }}>
              Partida finalizada — solo lectura
            </Typography>
            {readOnlyInfo.nombre ? (
              <Typography sx={{ color: COLORS.parchment, fontSize: "0.9rem", mb: 0.5 }}>
                Nombre: {readOnlyInfo.nombre}
              </Typography>
            ) : null}
            {readOnlyInfo.ganador !== null && readOnlyInfo.ganador !== undefined ? (
              <Typography sx={{ color: COLORS.parchment, fontSize: "0.9rem", mb: 0.5 }}>
                Ganador: {readOnlyInfo.ganador ? "Tripulación" : "Impostores"}
              </Typography>
            ) : null}
            {readOnlyInfo.fechaFin && (
              <Typography sx={{ color: COLORS.muted, fontSize: "0.85rem", mb: 0.75 }}>
                Fin: {readOnlyInfo.fechaFin}
              </Typography>
            )}
            {readOnlyInfo.mensaje && (
              <Typography sx={{ color: COLORS.muted, fontSize: "0.82rem", lineHeight: 1.45 }}>
                {readOnlyInfo.mensaje}
              </Typography>
            )}
          </Paper>
        )}

        <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              flex: 1,
              background: COLORS.panel,
              border: `1px solid ${COLORS.border}`,
              borderRadius: 2,
            }}
          >
            <Typography sx={{ color: COLORS.muted, fontSize: "0.85rem", mb: 1 }}>
              Tiempo restante
            </Typography>
            <Typography sx={{ color: COLORS.goldLight, fontSize: "2.2rem" }}>
              {formatSeconds(timeRemaining)}
            </Typography>
          </Paper>

          <Paper
            elevation={0}
            sx={{
              p: 2,
              flex: 2,
              background: COLORS.panel,
              border: `1px solid ${COLORS.border}`,
              borderRadius: 2,
            }}
          >
            <Typography sx={{ color: COLORS.muted, fontSize: "0.85rem", mb: 1 }}>
              Progreso de partida
            </Typography>
            <Typography sx={{ color: COLORS.goldLight, fontSize: "1.5rem", mb: 1 }}>
              {progress}%
            </Typography>
            <LinearProgress
              variant="determinate"
              value={progress}
              sx={{
                height: 10,
                borderRadius: 5,
                backgroundColor: "rgba(196,122,90,0.18)",
                "& .MuiLinearProgress-bar": {
                  backgroundColor: COLORS.gold,
                },
              }}
            />
          </Paper>
        </Stack>

        <Stack direction={{ xs: "column", lg: "row" }} spacing={2}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              flex: 2,
              height: 360,
              maxHeight: 360,
              background: COLORS.panel,
              border: `1px solid ${COLORS.border}`,
              borderRadius: 2,
              display: "flex",
              flexDirection: "column",
              overflow: "hidden",
            }}
          >
            <Typography sx={{ color: COLORS.goldLight, fontSize: "1.1rem", mb: 1.5 }}>
              Acciones y eventos
            </Typography>
            <Box
              sx={{
                overflowY: "auto",
                pr: 1,
                flex: 1,
                minHeight: 0,
                display: "flex",
                flexDirection: "column",
                gap: 1,
              }}
            >
              {events.length === 0 ? (
                <Typography sx={{ color: COLORS.muted, fontSize: "0.9rem" }}>
                  Esperando eventos de la partida...
                </Typography>
              ) : (
                [...events].reverse().map((item, idx) => (
                  <Box
                    key={`${item.time}-${idx}`}
                    sx={{
                      border: `1px solid ${COLORS.border}66`,
                      borderRadius: 1.5,
                      p: 1.2,
                      background: "#ffffff08",
                    }}
                  >
                    <Typography sx={{ color: COLORS.goldLight, fontSize: "0.8rem" }}>
                      {item.time} - {item.label}
                    </Typography>
                    <Typography sx={{ color: COLORS.parchment, fontSize: "0.9rem" }}>
                      {item.text}
                    </Typography>
                  </Box>
                ))
              )}
            </Box>
          </Paper>

          <Paper
            elevation={0}
            sx={{
              p: 2,
              flex: 1,
              minHeight: 360,
              background: COLORS.panel,
              border: `1px solid ${COLORS.border}`,
              borderRadius: 2,
            }}
          >
            <Typography sx={{ color: COLORS.goldLight, fontSize: "1.1rem", mb: 1.5 }}>
              Puntuaciones
            </Typography>
            <Stack spacing={1}>
              {ranking.length === 0 ? (
                <Typography sx={{ color: COLORS.muted, fontSize: "0.9rem" }}>
                  Sin jugadores con puntuacion todavia.
                </Typography>
              ) : (
                ranking.map((player, index) => (
                  <Box
                    key={player.id}
                    sx={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      border: `1px solid ${COLORS.border}66`,
                      borderRadius: 1.5,
                      p: 1.2,
                      background: index === 0 ? "#f0c06014" : "#ffffff08",
                    }}
                  >
                    <Typography sx={{ color: COLORS.parchment, fontSize: "0.95rem" }}>
                      #{index + 1} {player.name}
                    </Typography>
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <Chip
                        size="small"
                        label={player.connected === false ? "OFF" : "ON"}
                        sx={{
                          height: 18,
                          fontSize: "0.65rem",
                          color: player.connected === false ? "#ef9a9a" : "#9be7a1",
                          border: `1px solid ${player.connected === false ? "#8b2020" : "#4caf50"}`,
                          background: player.connected === false ? "#8b202022" : "#4caf5022",
                        }}
                      />
                      <Typography sx={{ color: COLORS.goldLight, fontSize: "1rem" }}>
                        {player.score}
                      </Typography>
                    </Box>
                  </Box>
                ))
              )}
            </Stack>
          </Paper>
        </Stack>
      </Stack>
    </Box>
  );
};

export default GameMonitor;
