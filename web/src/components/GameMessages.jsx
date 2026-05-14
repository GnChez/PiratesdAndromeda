import React, { useState, useEffect, useRef } from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import Paper from "@mui/material/Paper";
import Chip from "@mui/material/Chip";

import { DEFAULT_API_BASE, getWsBaseFromHttpApi } from "../config";

// ── Constantes ───────────────────────────────────────────────────────────────
const COLORS = {
  bg: "#0d0a06",
  panel: "#1a1208",
  border: "#7a5c1e",
  gold: "#c9922a",
  goldLight: "#f0c060",
  parchment: "#e8d5a3",
  muted: "#8a7a5a",
  incoming: "#1e2d1a",
  outgoing: "#1a1208",
  system: "#1a1020",
  red: "#8b2020",
};

// ── Tipos de mensaje ─────────────────────────────────────────────────────────
const MSG_SENDER = {
  incoming: { label: "Tripulación", color: "#4caf50", bg: COLORS.incoming },
  outgoing: { label: "Tú", color: COLORS.gold, bg: COLORS.outgoing },
  system: { label: "Bitácora", color: "#9c6fce", bg: COLORS.system },
};

const MSG_TYPE = {
  evento_iot:{ label:"Evento", color: "#4c82afff", bg: "#2c363fff"},
  start:{label:"Empieza misión", color: "#4caf50", bg: COLORS.incoming},
  complete: { label: "Termina misión", color: COLORS.gold, bg: COLORS.outgoing },
  sabotage: { label: "Misión saboteada", color: "#9c6fce", bg: COLORS.system },
  system: { label: "Bitácora", color: "#454ecaff", bg: "#0f1130ff" },

}

// ── Utilidades ───────────────────────────────────────────────────────────────
const timestamp = () =>
  new Date().toLocaleTimeString("es-ES", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });

// ── Sub-componente: una sola burbuja ─────────────────────────────────────────
const MessageBubble = ({ message }) => {
  console.log(message);
  
  const { type, text, sender, time } = message;
  const meta = MSG_TYPE[type] ?? MSG_SENDER.incoming;
  const isOutgoing = type === "outgoing";

  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        alignItems: isOutgoing ? "flex-end" : "flex-start",
        animation: "fadeSlideIn 0.3s ease forwards",
        mb: 1.5,
      }}
    >
      {/* Remitente + hora */}
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1,
          mb: 0.4,
          flexDirection: isOutgoing ? "row-reverse" : "row",
        }}
      >
        <Typography
          sx={{
            fontSize: "0.7rem",
            color: meta.color,
            fontFamily: "PirataOne",
          }}
        >
          {sender ?? meta.label}
        </Typography>
        <Typography sx={{ fontSize: "0.62rem", color: COLORS.muted }}>
          {time}
        </Typography>
      </Box>

      {/* Burbuja */}
      <Paper
        elevation={0}
        sx={{
          maxWidth: "75%",
          px: 2,
          py: 1.2,
          background: meta.bg,
          border: `1px solid ${meta.color}33`,
          borderRadius: isOutgoing
            ? "12px 2px 12px 12px"
            : "2px 12px 12px 12px",
          position: "relative",
          "&::before": {
            content: '""',
            position: "absolute",
            top: 0,
            [isOutgoing ? "right" : "left"]: -1,
            width: "3px",
            height: "100%",
            background: meta.color,
            borderRadius: "2px",
          },
        }}
      >
        <Typography
          sx={{
            fontSize: "0.9rem",
            color: COLORS.parchment,
            fontFamily: "PirataOne",
            lineHeight: 1.5,
            wordBreak: "break-word",
          }}
        >
          {text}
        </Typography>
      </Paper>
    </Box>
  );
};

// ── Componente principal ─────────────────────────────────────────────────────
/**
 * MessageViewer
 *
 * Props:
 *   wsUrl      – URL del WebSocket (por defecto: API en la VM)
 *   title      – Título del panel (opcional)
 *   maxMessages – Máximo de mensajes a conservar (default 100)
 *   onMessage  – Callback opcional: (rawData) => parsedMessage | null
 *                Si se omite, se espera JSON con { type, text, sender }
 */
const MessageViewer = ({
  wsUrl = getWsBaseFromHttpApi(DEFAULT_API_BASE),
  title = "Piratas de Andromeda",
  maxMessages = 100,
  onMessage,
}) => {
  const [messages, setMessages] = useState([]);
  const [status, setStatus] = useState("connecting"); // connecting | connected | disconnected | error
  const [error, setError] = useState(null);
  const bottomRef = useRef(null);
  const wsRef = useRef(null);

  function addMessage(msg) {
    setMessages((prev) => [...prev.slice(-maxMessages + 1), msg]);
  }

  function addSystemMessage(text) {
    addMessage({ type: "system", text, sender: "Sistema", time: timestamp() });
  }

  // Scroll automático al último mensaje
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Conectar WebSocket
  useEffect(() => {
    if (!wsUrl) return;

    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setStatus("connected");
      addSystemMessage("⚓ Conexión establecida con el galeón.");
    };

    ws.onmessage = (event) => {
      let parsed;
      if (onMessage) {
        parsed = onMessage(event.data);
        if (!parsed) return; // el callback puede filtrar mensajes
      } else {
        try {
          parsed = JSON.parse(event.data);
        } catch {
          parsed = { type: "incoming", data: event.data };
        }
      }
      addMessage({ ...parsed, time: timestamp() });
    };

    ws.onerror = () => {
      setStatus("error");
      setError("No se pudo conectar al WebSocket.");
    };

    ws.onclose = () => {
      setStatus("disconnected");
      addSystemMessage("🏴‍☠️ Conexión cerrada.");
    };

    return () => ws.close();
  }, [wsUrl]);

  // ── Estado de conexión (badge) ──────────────────────────────────────────
  const statusCfg = {
    connecting: { label: "Conectando...", color: COLORS.gold },
    connected: { label: "En línea", color: "#4caf50" },
    disconnected: { label: "Desconectado", color: COLORS.muted },
    error: { label: "Error", color: COLORS.red },
  }[status];

  // ── Render ──────────────────────────────────────────────────────────────
  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        height: "100%",
        minHeight: 400,
        background: COLORS.bg,
        border: `1px solid ${COLORS.border}`,
        borderRadius: 2,
        overflow: "hidden",
        fontFamily: "PirataOne",
        boxShadow: `0 0 40px #00000080, inset 0 0 80px #00000040`,
      }}
    >
      {/* ── Cabecera ── */}
      <Box
        sx={{
          px: 2.5,
          py: 1.5,
          borderBottom: `1px solid ${COLORS.border}`,
          background: `linear-gradient(90deg, ${COLORS.panel} 0%, #12100a 100%)`,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: 1,
        }}
      >
        <Typography
          variant="h6"
          sx={{
            color: COLORS.goldLight,
            fontFamily: "PirataOne",
            letterSpacing: 1,
          }}
        >
          ☠ {title}
        </Typography>

        <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
          {/* Badge de estado */}
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.8 }}>
            <Box
              sx={{
                width: 8,
                height: 8,
                borderRadius: "50%",
                background: statusCfg.color,
                animation:
                  status === "connected" ? "pulse 2s infinite" : "none",
                boxShadow: `0 0 6px ${statusCfg.color}`,
              }}
            />
            <Typography sx={{ fontSize: "0.72rem", color: statusCfg.color }}>
              {statusCfg.label}
            </Typography>
          </Box>

          {/* Contador */}
          <Chip
            label={`${messages.length} msg`}
            size="small"
            sx={{
              height: 20,
              fontSize: "0.65rem",
              background: "#ffffff10",
              color: COLORS.muted,
              border: `1px solid ${COLORS.border}`,
            }}
          />
        </Box>
      </Box>

      {/* ── Error ── */}
      {error && (
        <Box
          sx={{
            px: 2,
            py: 1,
            background: `${COLORS.red}22`,
            borderBottom: `1px solid ${COLORS.red}44`,
          }}
        >
          <Typography
            sx={{
              fontSize: "0.8rem",
              color: "#ef9a9a",
              fontFamily: "PirataOne",
            }}
          >
            ⚠ {error}
          </Typography>
        </Box>
      )}

      {/* ── Lista de mensajes ── */}
      <Box
        sx={{
          flex: 1,
          overflowY: "auto",
          px: 2,
          py: 2,
          display: "flex",
          flexDirection: "column",
          gap: 0,
          "&::-webkit-scrollbar": { width: "6px" },
          "&::-webkit-scrollbar-track": { background: "transparent" },
          "&::-webkit-scrollbar-thumb": {
            background: COLORS.border,
            borderRadius: "3px",
          },
        }}
      >
        {messages.length === 0 ? (
          <Box
            sx={{
              flex: 1,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexDirection: "column",
              gap: 1,
              opacity: 0.4,
            }}
          >
            <Typography sx={{ fontSize: "2.5rem" }}>🏴‍☠️</Typography>
            <Typography
              sx={{
                color: COLORS.muted,
                fontFamily: "PirataOne",
                fontSize: "0.9rem",
              }}
            >
              Esperando mensajes del mar...
            </Typography>
          </Box>
        ) : (
          messages.map((msg, i) => <MessageBubble key={i} message={msg} />)
        )}
        <div ref={bottomRef} />
      </Box>

      {/* ── Pie ── */}
      <Box
        sx={{
          px: 2.5,
          py: 1,
          borderTop: `1px solid ${COLORS.border}`,
          background: COLORS.panel,
        }}
      >
        <Typography sx={{ fontSize: "0.68rem", color: COLORS.muted }}>
          {wsUrl}
        </Typography>
      </Box>
    </Box>
  );
};

export default MessageViewer;
