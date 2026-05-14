from typing import Dict, List, Optional, Tuple

from fastapi import WebSocket


class ConnectionManager:
    def __init__(self):
        # {room_uuid: [list_of_websockets]} — TODOS los websockets (jugadores + monitores).
        # Se mantiene para mantener compatibilidad con el broadcast existente.
        self.active_rooms: dict[str, list[WebSocket]] = {}
        # {room_uuid: [list_of_websockets]} — solo websockets de jugadores (móvil).
        self.player_rooms: dict[str, list[WebSocket]] = {}
        # {room_uuid: [list_of_websockets]} — solo websockets de monitor (web).
        self.monitor_rooms: dict[str, list[WebSocket]] = {}
        # Jugadores con WS activo: room_uuid -> {websocket -> (player_key, nombre_mostrado)}.
        self.live_players: dict[str, dict[WebSocket, tuple[str, Optional[str]]]] = {}
        # {join_code: room_uuid}
        self.code_to_uuid: dict[str, str] = {}
        # ws_code (token devuelto en POST /join) -> (codigo_partida, id_usuario)
        self.ws_sessions: Dict[str, Tuple[str, int]] = {}

    def create_game(self, code: str, room_uuid: str):
        """Asocia un código de 4-6 dígitos con un UUID interno."""
        self.code_to_uuid[code] = room_uuid
        self.active_rooms.setdefault(room_uuid, [])
        self.player_rooms.setdefault(room_uuid, [])
        self.monitor_rooms.setdefault(room_uuid, [])

    def join_code_to_room_uuid(self, join_code: str):
        return self.code_to_uuid[join_code]

    def register_ws_session(self, ws_code: str, codigo_partida: str, id_usuario: int) -> None:
        """Un jugador activo por partida: nuevo join invalida tokens anteriores."""
        self.unregister_by_game_and_player(codigo_partida, id_usuario)
        self.ws_sessions[ws_code] = (codigo_partida, id_usuario)

    def unregister_ws_code(self, ws_code: str) -> None:
        self.ws_sessions.pop(ws_code, None)

    def unregister_by_game_and_player(self, codigo_partida: str, id_usuario: int) -> None:
        stale = [
            token
            for token, (cp, uid) in self.ws_sessions.items()
            if cp == codigo_partida and uid == id_usuario
        ]
        for token in stale:
            self.ws_sessions.pop(token, None)

    def resolve_player_from_ws_code(self, ws_code: str, codigo_partida: str) -> Optional[int]:
        """Solo acepta el token devuelto en POST /games/join (campo ws_code)."""
        entry = self.ws_sessions.get(ws_code)
        if entry is None:
            return None
        code_saved, uid = entry
        if code_saved != codigo_partida:
            return None
        return uid

    async def connect(self, websocket: WebSocket, room_uuid: str):
        """Compatibilidad histórica: registra como jugador (no monitor)."""
        await self.connect_player(websocket, room_uuid)

    async def connect_player(self, websocket: WebSocket, room_uuid: str) -> None:
        await websocket.accept()
        self.active_rooms.setdefault(room_uuid, []).append(websocket)
        self.player_rooms.setdefault(room_uuid, []).append(websocket)

    async def connect_monitor(self, websocket: WebSocket, room_uuid: str) -> None:
        await websocket.accept()
        self.active_rooms.setdefault(room_uuid, []).append(websocket)
        self.monitor_rooms.setdefault(room_uuid, []).append(websocket)

    def register_live_player(
        self,
        websocket: WebSocket,
        room_uuid: str,
        player_key: str,
        display_name: Optional[str],
    ) -> None:
        """Tras aceptar el WS de jugador: permite al monitor listar quien ya está en sala."""
        self.live_players.setdefault(room_uuid, {})[websocket] = (player_key, display_name)

    def unregister_live_player(self, websocket: WebSocket, room_uuid: str) -> None:
        room = self.live_players.get(room_uuid)
        if not room or websocket not in room:
            return
        del room[websocket]
        if not room:
            self.live_players.pop(room_uuid, None)

    def snapshot_connected_players(self, room_uuid: str) -> list[dict[str, object]]:
        """Misma forma que PLAYER_JOINED (player, nombre_usuario, player_name) para el monitor."""
        out: list[dict[str, object]] = []
        for _ws, (player_key, display_name) in self.live_players.get(room_uuid, {}).items():
            out.append(
                {
                    "player": player_key,
                    "nombre_usuario": display_name,
                    "player_name": display_name,
                    "connected": True,
                }
            )
        return out

    def disconnect(self, websocket: WebSocket, room_uuid: str):
        self.unregister_live_player(websocket, room_uuid)
        for bucket in (self.active_rooms, self.player_rooms, self.monitor_rooms):
            if room_uuid in bucket:
                try:
                    bucket[room_uuid].remove(websocket)
                except ValueError:
                    pass
                if not bucket[room_uuid]:
                    del bucket[room_uuid]

    def count_players(self, room_uuid: str) -> int:
        return len(self.player_rooms.get(room_uuid, []))

    async def broadcast_to_room(self, message: dict, room_uuid: str):
        if room_uuid not in self.active_rooms:
            return
        stale: List[WebSocket] = []
        for connection in list(self.active_rooms[room_uuid]):
            try:
                await connection.send_json(message)
            except Exception:
                stale.append(connection)
        for connection in stale:
            self.disconnect(connection, room_uuid)


manager = ConnectionManager()
