from __future__ import annotations

import asyncio
import threading
import time
from collections import deque
from dataclasses import dataclass
from typing import Any

import websockets
from websockets.server import WebSocketServerProtocol

from .actions import ActionExecutor, get_active_window_title
from .audio import AudioOutputEngine
from .config import RuntimeConfig, STATE_PUSH_INTERVAL_SEC
from .protocol import dumps_json, loads_json, make_event, make_pong, make_state_message, parse_audio_frame


@dataclass
class Snapshot:
    server_running: bool
    connected: bool
    client_ip: str
    rtt_ms: int | None
    jitter_ms: int
    mute: bool
    actions_enabled: bool
    selected_device: str
    codec: str


class ReceiverController:
    def __init__(self, config: RuntimeConfig) -> None:
        self.config = config

        self._logs = deque(maxlen=200)
        self._lock = threading.Lock()

        self.audio = AudioOutputEngine(jitter_target_ms=config.jitter_target_ms, log=self.log)
        self.audio.select_preferred_device()
        self.audio.start()

        self.actions = ActionExecutor(log=self.log)

        self._server_thread: threading.Thread | None = None
        self._loop: asyncio.AbstractEventLoop | None = None
        self._stop_event: asyncio.Event | None = None

        self._server_running = False
        self._connected = False
        self._client_ip = ""
        self._mute = False
        self._rtt_ms: int | None = None

    def log(self, message: str) -> None:
        with self._lock:
            self._logs.appendleft(message)

    def recent_logs(self, limit: int = 50) -> list[str]:
        with self._lock:
            out = list(self._logs)
        return out[:limit]

    def start_server(self) -> None:
        if self._server_thread and self._server_thread.is_alive():
            return
        self._server_thread = threading.Thread(target=self._run_server_thread, name="ReceiverServer", daemon=True)
        self._server_thread.start()

    def stop_server(self) -> None:
        loop = self._loop
        stop_event = self._stop_event
        if loop and stop_event:
            loop.call_soon_threadsafe(stop_event.set)
        if self._server_thread:
            self._server_thread.join(timeout=2.0)

    def shutdown(self) -> None:
        self.stop_server()
        self.audio.stop()

    def set_actions_enabled(self, enabled: bool) -> None:
        self.actions.set_enabled(enabled)

    def toggle_actions(self) -> bool:
        return self.actions.toggle_enabled()

    def set_mute(self, value: bool) -> None:
        self._mute = bool(value)
        self.audio.set_mute(self._mute)
        self.log(f"Mute set to {self._mute}")

    def list_output_devices(self) -> list[tuple[int, str]]:
        return self.audio.list_output_devices()

    def select_output_device(self, index: int | None) -> None:
        self.audio.set_output_device(index)

    def set_jitter_target(self, ms: int) -> None:
        self.audio.set_jitter_target_ms(ms)

    def snapshot(self) -> Snapshot:
        stats = self.audio.get_stats()
        return Snapshot(
            server_running=self._server_running,
            connected=self._connected,
            client_ip=self._client_ip,
            rtt_ms=self._rtt_ms,
            jitter_ms=stats.jitter_ms,
            mute=self._mute,
            actions_enabled=self.actions.enabled,
            selected_device=self.audio.get_selected_device_name(),
            codec=stats.codec_name,
        )

    def _run_server_thread(self) -> None:
        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)
        self._stop_event = asyncio.Event()
        try:
            self._loop.run_until_complete(self._run_server())
        finally:
            self._server_running = False
            pending = asyncio.all_tasks(self._loop)
            for task in pending:
                task.cancel()
            if pending:
                self._loop.run_until_complete(asyncio.gather(*pending, return_exceptions=True))
            self._loop.close()

    async def _run_server(self) -> None:
        self.log(f"WebSocket server starting at ws://{self.config.host}:{self.config.port}")
        self._server_running = True
        async with websockets.serve(self._client_handler, self.config.host, self.config.port, ping_interval=None):
            await self._stop_event.wait()
        self.log("WebSocket server stopped")
        self._server_running = False

    async def _client_handler(self, websocket: WebSocketServerProtocol) -> None:
        remote = websocket.remote_address
        client_ip = str(remote[0]) if remote else "unknown"

        self._connected = True
        self._client_ip = client_ip
        self.audio.clear()
        self.log(f"Client connected: {client_ip}")

        state_task = asyncio.create_task(self._push_state_loop(websocket))

        try:
            async for msg in websocket:
                if isinstance(msg, bytes):
                    packet = parse_audio_frame(msg)
                    if packet is None:
                        continue
                    self.audio.push_packet(packet)
                else:
                    await self._handle_text_message(websocket, msg)
        except Exception as exc:
            self.log(f"Client error: {exc}")
        finally:
            state_task.cancel()
            await asyncio.gather(state_task, return_exceptions=True)
            self._connected = False
            self._client_ip = ""
            self.audio.clear()
            self.log("Client disconnected")

    async def _handle_text_message(self, websocket: WebSocketServerProtocol, text: str) -> None:
        msg = loads_json(text)
        if msg is None:
            return

        typ = str(msg.get("type", "")).lower()
        if typ == "ping":
            ts = int(msg.get("ts", 0))
            await websocket.send(dumps_json(make_pong(ts)))
        elif typ == "action":
            result = await asyncio.to_thread(self.actions.execute_action, msg)
            await websocket.send(dumps_json(make_event("info" if result.ok else "error", result.message)))
        elif typ == "control":
            op = str(msg.get("op", ""))
            if op == "set_mute":
                value = bool(msg.get("value", False))
                self.set_mute(value)
                await websocket.send(dumps_json(make_event("info", f"mute={value}")))

    async def _push_state_loop(self, websocket: WebSocketServerProtocol) -> None:
        while True:
            snap = self.snapshot()
            state_msg = make_state_message(
                connected=snap.connected,
                mute=snap.mute,
                rtt_ms=snap.rtt_ms,
                jitter_ms=snap.jitter_ms,
                active_window=get_active_window_title(),
                audio_codec=snap.codec,
                extra={"actions_enabled": snap.actions_enabled},
            )
            await websocket.send(dumps_json(state_msg))
            await asyncio.sleep(STATE_PUSH_INTERVAL_SEC)
