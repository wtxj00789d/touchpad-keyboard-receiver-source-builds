from __future__ import annotations

import json
import struct
from dataclasses import dataclass
from typing import Any


@dataclass
class AudioPacket:
    codec: int
    flags: int
    seq: int
    timestamp_ms: int
    payload: bytes

    @property
    def muted(self) -> bool:
        return bool(self.flags & 0x01)


def parse_audio_frame(data: bytes) -> AudioPacket | None:
    if len(data) < 14:
        return None
    codec = data[0]
    flags = data[1]
    seq = struct.unpack_from("<I", data, 2)[0]
    timestamp_ms = struct.unpack_from("<Q", data, 6)[0]
    payload = data[14:]
    return AudioPacket(codec=codec, flags=flags, seq=seq, timestamp_ms=timestamp_ms, payload=payload)


def loads_json(text: str) -> dict[str, Any] | None:
    try:
        obj = json.loads(text)
    except json.JSONDecodeError:
        return None
    if isinstance(obj, dict):
        return obj
    return None


def dumps_json(obj: dict[str, Any]) -> str:
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))


def make_state_message(
    connected: bool,
    mute: bool,
    rtt_ms: int | None,
    jitter_ms: int | None,
    active_window: str,
    audio_codec: str,
    extra: dict[str, Any] | None = None,
) -> dict[str, Any]:
    data: dict[str, Any] = {
        "type": "state",
        "connected": connected,
        "mute": mute,
        "rtt_ms": rtt_ms,
        "jitter_ms": jitter_ms,
        "active_window": active_window,
        "audio_codec": audio_codec,
    }
    if extra:
        data.update(extra)
    return data


def make_event(level: str, message: str) -> dict[str, Any]:
    return {"type": "event", "level": level, "message": message}


def make_pong(ts: int) -> dict[str, Any]:
    return {"type": "pong", "ts": ts}
