from dataclasses import dataclass

WS_HOST = "0.0.0.0"
WS_PORT = 8765
SAMPLE_RATE = 48_000
CHANNELS = 1
FRAME_MS = 20
FRAME_SAMPLES = SAMPLE_RATE * FRAME_MS // 1000
FRAME_BYTES_PCM16 = FRAME_SAMPLES * 2
DEFAULT_JITTER_MS = 60
MIN_JITTER_MS = 20
MAX_JITTER_MS = 200
STATE_PUSH_INTERVAL_SEC = 1.0


@dataclass
class RuntimeConfig:
    host: str = WS_HOST
    port: int = WS_PORT
    jitter_target_ms: int = DEFAULT_JITTER_MS
