from __future__ import annotations

import threading
import time
from collections import deque
from dataclasses import dataclass
from typing import Callable

import sounddevice as sd

from .config import FRAME_BYTES_PCM16, FRAME_SAMPLES, SAMPLE_RATE
from .protocol import AudioPacket

LogFn = Callable[[str], None]


@dataclass
class AudioStats:
    jitter_ms: int = 0
    queue_frames: int = 0
    underruns: int = 0
    dropped_frames: int = 0
    codec_name: str = "PCM16"


class AudioOutputEngine:
    def __init__(self, jitter_target_ms: int, log: LogFn | None = None) -> None:
        self._log = log or (lambda _msg: None)
        self._jitter_target_ms = max(20, min(200, int(jitter_target_ms)))
        self._target_frames = max(1, self._jitter_target_ms // 20)

        self._queue: deque[bytes] = deque(maxlen=512)
        self._lock = threading.Lock()
        self._running = False
        self._thread: threading.Thread | None = None
        self._reopen_stream = False

        self._muted = False
        self._device_index: int | None = None

        self._last_arrival_ms: float | None = None
        self._last_timestamp_ms: int | None = None
        self._jitter_estimate = 0.0

        self._underruns = 0
        self._dropped_frames = 0
        self._codec_name = "PCM16"

    def list_output_devices(self) -> list[tuple[int, str]]:
        devices = []
        for idx, d in enumerate(sd.query_devices()):
            if d.get("max_output_channels", 0) > 0:
                devices.append((idx, str(d.get("name", f"device-{idx}"))))
        return devices

    def select_preferred_device(self) -> None:
        outputs = self.list_output_devices()
        chosen = None
        for idx, name in outputs:
            if "CABLE Input" in name:
                chosen = idx
                break
        if chosen is None:
            default = sd.default.device
            if isinstance(default, (list, tuple)) and len(default) >= 2 and default[1] is not None:
                chosen = int(default[1])
            elif outputs:
                chosen = outputs[0][0]
        self.set_output_device(chosen)

    def set_output_device(self, index: int | None) -> None:
        with self._lock:
            self._device_index = index
            self._reopen_stream = True

    def get_selected_device_name(self) -> str:
        with self._lock:
            idx = self._device_index
        if idx is None:
            return "System default"
        try:
            d = sd.query_devices(idx)
            return str(d.get("name", f"#{idx}"))
        except Exception:
            return f"#{idx}"

    def set_mute(self, value: bool) -> None:
        self._muted = value

    def start(self) -> None:
        if self._running:
            return
        self._running = True
        self._thread = threading.Thread(target=self._run, name="AudioOutputEngine", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._running = False
        if self._thread is not None:
            self._thread.join(timeout=1.5)
            self._thread = None

    def set_jitter_target_ms(self, ms: int) -> None:
        with self._lock:
            self._jitter_target_ms = max(20, min(200, int(ms)))
            self._target_frames = max(1, self._jitter_target_ms // 20)

    def push_packet(self, packet: AudioPacket) -> None:
        arrival_ms = time.monotonic() * 1000.0
        self._update_jitter(packet.timestamp_ms, arrival_ms)

        self._codec_name = "PCM16" if packet.codec == 0 else "OPUS"
        if packet.codec != 0:
            # OPUS path reserved. Ignore payload for now.
            return

        frame = packet.payload
        if len(frame) < FRAME_BYTES_PCM16:
            frame = frame + b"\x00" * (FRAME_BYTES_PCM16 - len(frame))
        elif len(frame) > FRAME_BYTES_PCM16:
            frame = frame[:FRAME_BYTES_PCM16]

        with self._lock:
            if len(self._queue) >= self._queue.maxlen:
                self._queue.popleft()
                self._dropped_frames += 1
            self._queue.append(frame)

    def clear(self) -> None:
        with self._lock:
            self._queue.clear()

    def get_stats(self) -> AudioStats:
        with self._lock:
            queue_frames = len(self._queue)
        return AudioStats(
            jitter_ms=int(self._jitter_estimate),
            queue_frames=queue_frames,
            underruns=self._underruns,
            dropped_frames=self._dropped_frames,
            codec_name=self._codec_name,
        )

    def _run(self) -> None:
        silence = b"\x00" * FRAME_BYTES_PCM16

        while self._running:
            try:
                with self._open_stream() as stream:
                    if stream is None:
                        time.sleep(1.0)
                        continue

                    self._log(f"Audio stream opened -> {self.get_selected_device_name()}")
                    while self._running:
                        if self._consume_reopen_flag():
                            break

                        frame = self._next_frame(silence)
                        if self._muted:
                            frame = silence

                        stream.write(frame)
            except Exception as exc:
                self._log(f"Audio output error: {exc}")
                time.sleep(1.0)

    def _open_stream(self):
        device = self._device_index
        try:
            stream = sd.RawOutputStream(
                samplerate=SAMPLE_RATE,
                channels=1,
                dtype="int16",
                blocksize=FRAME_SAMPLES,
                device=device,
            )
            return stream
        except Exception as exc:
            self._log(f"Failed to open audio device ({device}): {exc}")
            return _NullContext()

    def _next_frame(self, silence: bytes) -> bytes:
        # Hold playback until queue reaches jitter target, to absorb burst jitter.
        deadline = time.monotonic() + 0.3
        while self._running:
            with self._lock:
                size = len(self._queue)
                enough = size >= self._target_frames
                if enough:
                    break
            if time.monotonic() >= deadline:
                break
            time.sleep(0.005)

        with self._lock:
            if self._queue:
                return self._queue.popleft()

        self._underruns += 1
        return silence

    def _update_jitter(self, sender_ts_ms: int, arrival_ms: float) -> None:
        if self._last_arrival_ms is None or self._last_timestamp_ms is None:
            self._last_arrival_ms = arrival_ms
            self._last_timestamp_ms = sender_ts_ms
            return

        transit = arrival_ms - sender_ts_ms
        prev_transit = self._last_arrival_ms - self._last_timestamp_ms
        d = abs(transit - prev_transit)
        self._jitter_estimate += (d - self._jitter_estimate) / 16.0

        self._last_arrival_ms = arrival_ms
        self._last_timestamp_ms = sender_ts_ms

    def _consume_reopen_flag(self) -> bool:
        with self._lock:
            if self._reopen_stream:
                self._reopen_stream = False
                return True
            return False


class _NullContext:
    def __enter__(self):
        return None

    def __exit__(self, exc_type, exc, tb):
        return False
