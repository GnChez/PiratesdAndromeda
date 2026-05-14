/**
 * API por defecto: backend desplegado en producción.
 * Sobrescribir en desarrollo con .env: VITE_BACKEND_URL=http://localhost:8000
 */
export const DEFAULT_API_BASE = "https://api.piratasandromeda.me";

export function normalizeApiBase(url) {
  return String(url || "").replace(/\/+$/, "");
}

/** Orden: query ?api= → Vite env → DEFAULT_API_BASE */
export function getApiBaseFromEnvOrDefault(queryApiBase) {
  const envBase = import.meta.env.VITE_BACKEND_URL;
  return normalizeApiBase(queryApiBase || envBase || DEFAULT_API_BASE);
}

/** http→ws, https→wss */
export function getWsBaseFromHttpApi(apiBase) {
  const base = normalizeApiBase(apiBase);
  if (base.startsWith("https://")) return `wss://${base.slice("https://".length)}`;
  if (base.startsWith("http://")) return `ws://${base.slice("http://".length)}`;
  return base;
}

export function defaultMonitorWsUrl(gameCode) {
  const ws = getWsBaseFromHttpApi(DEFAULT_API_BASE);
  if (gameCode) return `${ws}/ws/monitor/${encodeURIComponent(gameCode)}`;
  return ws;
}
