# Protocol Specification

Unified WebSocket connection: `ws://<host>:8765`

- Binary frame: audio frame
- Text frame (JSON): action / control / state / ping / hello

## 1) Audio Binary Frame

Header (fixed 14 bytes, little-endian):

1. `codec` (uint8)
   - `0` = PCM16 (48k, mono, 16-bit)
   - `1` = OPUS (reserved)
2. `flags` (uint8)
   - bit0: muted
   - bit1~7: reserved
3. `seq` (uint32 little-endian)
4. `timestamp_ms` (uint64 little-endian, Android monotonic clock)
5. `payload` (bytes)
   - codec=0: 20ms PCM = 960 samples = 1920 bytes
   - codec=1: Opus frame bytes

## 2) Text JSON Messages

### Android -> Windows

Hello (optional, for client role metadata):

```json
{"type":"hello","role":"touchpad","device_id":"PixelTablet-abc123","app_version":"0.1.0","seq":1,"ts":1710000000000}
```

Action (keyboard):

```json
{"type":"action","action_id":"save","kind":"KEY","payload":"CTRL+S","seq":123,"ts":1710000000000}
```

Action (touchpad move):

```json
{"type":"action","kind":"MOUSE","payload":{"op":"MOVE_REL","dx":18,"dy":-6},"seq":124,"ts":1710000000020}
```

Action (touchpad click/drag/scroll):

```json
{"type":"action","kind":"MOUSE","payload":{"op":"CLICK","button":"left"},"seq":125,"ts":1710000000040}
```

```json
{"type":"action","kind":"MOUSE","payload":{"op":"BUTTON_DOWN","button":"left"},"seq":126,"ts":1710000000060}
```

```json
{"type":"action","kind":"MOUSE","payload":{"op":"BUTTON_UP","button":"left"},"seq":127,"ts":1710000000080}
```

```json
{"type":"action","kind":"MOUSE","payload":{"op":"SCROLL","delta":120},"seq":128,"ts":1710000000100}
```

Macro:

```json
{"type":"action","kind":"MACRO","payload":[{"op":"KEY","value":"CTRL+K"},{"op":"DELAY","ms":50},{"op":"TEXT","value":"hello"},{"op":"KEY","value":"ENTER"}],"seq":129,"ts":1710000000120}
```

Control:

```json
{"type":"control","op":"set_mute","value":true,"seq":200,"ts":1710000001000}
```

Ping:

```json
{"type":"ping","ts":1710000001200}
```

### Windows -> Android

State:

```json
{"type":"state","connected":true,"mute":false,"rtt_ms":32,"jitter_ms":10,"active_window":"Visual Studio Code","audio_codec":"PCM16"}
```

Pong:

```json
{"type":"pong","ts":1710000001200}
```

Log/Event:

```json
{"type":"event","level":"info","message":"Action executed: CTRL+S"}
```

## 3) Reliability / Ordering

- WebSocket guarantees in-order delivery on one connection.
- Audio stream uses `seq` for packet-loss stats.
- Receiver jitter buffer target defaults to 60ms, configurable in 20~200ms range.

## 4) Backward Compatibility

- New fields must be optional.
- Unknown `type` / `kind` / `payload.op` should be ignored and logged.
- `kind=MOUSE` remains backward compatible with legacy string payloads (`LEFT_CLICK`, `RIGHT_CLICK`, `WHEEL_UP`, `WHEEL_DOWN`).
