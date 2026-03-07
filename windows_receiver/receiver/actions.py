from __future__ import annotations

import ctypes
import time
from collections import deque
from dataclasses import dataclass
from typing import Any, Callable

LogFn = Callable[[str], None]

user32 = ctypes.windll.user32

INPUT_MOUSE = 0
INPUT_KEYBOARD = 1

KEYEVENTF_EXTENDEDKEY = 0x0001
KEYEVENTF_KEYUP = 0x0002
KEYEVENTF_UNICODE = 0x0004

MOUSEEVENTF_LEFTDOWN = 0x0002
MOUSEEVENTF_LEFTUP = 0x0004
MOUSEEVENTF_RIGHTDOWN = 0x0008
MOUSEEVENTF_RIGHTUP = 0x0010
MOUSEEVENTF_WHEEL = 0x0800

ULONG_PTR = ctypes.c_ulonglong if ctypes.sizeof(ctypes.c_void_p) == 8 else ctypes.c_ulong


class MOUSEINPUT(ctypes.Structure):
    _fields_ = [
        ("dx", ctypes.c_long),
        ("dy", ctypes.c_long),
        ("mouseData", ctypes.c_ulong),
        ("dwFlags", ctypes.c_ulong),
        ("time", ctypes.c_ulong),
        ("dwExtraInfo", ULONG_PTR),
    ]


class KEYBDINPUT(ctypes.Structure):
    _fields_ = [
        ("wVk", ctypes.c_ushort),
        ("wScan", ctypes.c_ushort),
        ("dwFlags", ctypes.c_ulong),
        ("time", ctypes.c_ulong),
        ("dwExtraInfo", ULONG_PTR),
    ]


class HARDWAREINPUT(ctypes.Structure):
    _fields_ = [("uMsg", ctypes.c_ulong), ("wParamL", ctypes.c_ushort), ("wParamH", ctypes.c_ushort)]


class INPUT_UNION(ctypes.Union):
    _fields_ = [("mi", MOUSEINPUT), ("ki", KEYBDINPUT), ("hi", HARDWAREINPUT)]


class INPUT(ctypes.Structure):
    _fields_ = [("type", ctypes.c_ulong), ("union", INPUT_UNION)]


VK = {
    "CTRL": 0x11,
    "LCTRL": 0xA2,
    "RCTRL": 0xA3,
    "SHIFT": 0x10,
    "LSHIFT": 0xA0,
    "RSHIFT": 0xA1,
    "ALT": 0x12,
    "LALT": 0xA4,
    "RALT": 0xA5,
    "WIN": 0x5B,
    "LWIN": 0x5B,
    "RWIN": 0x5C,
    "MENU": 0x5D,
    "APPS": 0x5D,
    "TAB": 0x09,
    "ENTER": 0x0D,
    "ESC": 0x1B,
    "SPACE": 0x20,
    "BACKSPACE": 0x08,
    "BKSP": 0x08,
    "CAPS": 0x14,
    "CAPSLOCK": 0x14,
    "DELETE": 0x2E,
    "DEL": 0x2E,
    "UP": 0x26,
    "DOWN": 0x28,
    "LEFT": 0x25,
    "RIGHT": 0x27,
    "HOME": 0x24,
    "END": 0x23,
    "PGUP": 0x21,
    "PGDN": 0x22,
    "VOLUME_UP": 0xAF,
    "VOLUME_DOWN": 0xAE,
    "MEDIA_NEXT": 0xB0,
    "MEDIA_PREV": 0xB1,
    "MINUS": 0xBD,
    "EQUALS": 0xBB,
    "LBRACKET": 0xDB,
    "RBRACKET": 0xDD,
    "BACKSLASH": 0xDC,
    "SEMICOLON": 0xBA,
    "APOSTROPHE": 0xDE,
    "COMMA": 0xBC,
    "PERIOD": 0xBE,
    "SLASH": 0xBF,
    "GRAVE": 0xC0,
    "BACKTICK": 0xC0,
    "-": 0xBD,
    "=": 0xBB,
    "[": 0xDB,
    "]": 0xDD,
    "\\": 0xDC,
    ";": 0xBA,
    "'": 0xDE,
    ",": 0xBC,
    ".": 0xBE,
    "/": 0xBF,
    "`": 0xC0,
}
for i in range(1, 25):
    VK[f"F{i}"] = 0x6F + i
for n in range(10):
    VK[str(n)] = 0x30 + n
for c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ":
    VK[c] = ord(c)

MODIFIERS = {"CTRL", "SHIFT", "ALT", "WIN"}
MODIFIERS |= {"LCTRL", "RCTRL", "LSHIFT", "RSHIFT", "LALT", "RALT", "LWIN", "RWIN"}


@dataclass
class ActionResult:
    ok: bool
    message: str


class ActionExecutor:
    def __init__(self, log: LogFn | None = None) -> None:
        self.enabled = True
        self._log = log or (lambda _m: None)
        self._recent = deque(maxlen=50)

    def set_enabled(self, value: bool) -> None:
        self.enabled = bool(value)
        self._add_log(f"Action execution {'enabled' if self.enabled else 'disabled'}")

    def toggle_enabled(self) -> bool:
        self.set_enabled(not self.enabled)
        return self.enabled

    def recent_logs(self) -> list[str]:
        return list(self._recent)

    def execute_action(self, action_msg: dict[str, Any]) -> ActionResult:
        kind = str(action_msg.get("kind", "")).upper()
        payload = action_msg.get("payload")

        if not self.enabled:
            msg = f"Ignored {kind}: actions disabled"
            self._add_log(msg)
            return ActionResult(True, msg)

        try:
            if kind == "KEY":
                combo = str(payload or "")
                self._send_key_combo(combo)
                msg = f"KEY {combo}"
            elif kind == "TEXT":
                text = str(payload or "")
                self._send_text(text)
                msg = f"TEXT len={len(text)}"
            elif kind == "MOUSE":
                cmd = str(payload or "").upper()
                self._send_mouse(cmd)
                msg = f"MOUSE {cmd}"
            elif kind == "MACRO":
                self._run_macro(payload)
                msg = "MACRO"
            elif kind in ("CMD", "TOGGLE"):
                msg = f"{kind} forwarded/logged"
            else:
                msg = f"Unknown action kind: {kind}"

            self._add_log(msg)
            return ActionResult(True, msg)
        except Exception as exc:
            msg = f"Action failed ({kind}): {exc}"
            self._add_log(msg)
            return ActionResult(False, msg)

    def _run_macro(self, payload: Any) -> None:
        if not isinstance(payload, list):
            return
        for step in payload:
            if not isinstance(step, dict):
                continue
            op = str(step.get("op", "")).upper()
            if op == "KEY":
                self._send_key_combo(str(step.get("value", "")))
            elif op == "TEXT":
                self._send_text(str(step.get("value", "")))
            elif op == "DELAY":
                ms = int(step.get("ms", 0))
                time.sleep(max(0, ms) / 1000.0)
            elif op == "MOUSE":
                self._send_mouse(str(step.get("value", "")).upper())

    def _send_key_combo(self, combo: str) -> None:
        tokens = [t.strip().upper() for t in combo.split("+") if t.strip()]
        if not tokens:
            return

        mod_tokens = [t for t in tokens if t in MODIFIERS]
        main_tokens = [t for t in tokens if t not in MODIFIERS]

        if not main_tokens and mod_tokens:
            main_tokens = [mod_tokens.pop()]

        for mod in mod_tokens:
            vk = VK.get(mod)
            if vk:
                _send_vk(vk, down=True)

        for t in main_tokens:
            vk = self._token_to_vk(t)
            if vk is None:
                continue
            _send_vk(vk, down=True)
            _send_vk(vk, down=False)

        for mod in reversed(mod_tokens):
            vk = VK.get(mod)
            if vk:
                _send_vk(vk, down=False)

    def _send_text(self, text: str) -> None:
        for ch in text:
            code = ord(ch)
            _send_unicode(code, down=True)
            _send_unicode(code, down=False)

    def _send_mouse(self, command: str) -> None:
        if command == "LEFT_CLICK":
            _send_mouse_flags(MOUSEEVENTF_LEFTDOWN)
            _send_mouse_flags(MOUSEEVENTF_LEFTUP)
        elif command == "DOUBLE_CLICK":
            for _ in range(2):
                _send_mouse_flags(MOUSEEVENTF_LEFTDOWN)
                _send_mouse_flags(MOUSEEVENTF_LEFTUP)
        elif command == "RIGHT_CLICK":
            _send_mouse_flags(MOUSEEVENTF_RIGHTDOWN)
            _send_mouse_flags(MOUSEEVENTF_RIGHTUP)
        elif command == "WHEEL_UP":
            _send_mouse_flags(MOUSEEVENTF_WHEEL, mouse_data=120)
        elif command == "WHEEL_DOWN":
            _send_mouse_flags(MOUSEEVENTF_WHEEL, mouse_data=ctypes.c_ulong(-120).value)

    def _token_to_vk(self, token: str) -> int | None:
        if token in VK:
            return VK[token]
        if len(token) == 1:
            return VK.get(token.upper())
        return None

    def _add_log(self, message: str) -> None:
        entry = f"[{time.strftime('%H:%M:%S')}] {message}"
        self._recent.appendleft(entry)
        self._log(entry)


def _send_vk(vk_code: int, down: bool) -> None:
    flags = 0 if down else KEYEVENTF_KEYUP
    kb = KEYBDINPUT(wVk=vk_code, wScan=0, dwFlags=flags, time=0, dwExtraInfo=0)
    inp = INPUT(type=INPUT_KEYBOARD, union=INPUT_UNION(ki=kb))
    user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))


def _send_unicode(codepoint: int, down: bool) -> None:
    flags = KEYEVENTF_UNICODE | (0 if down else KEYEVENTF_KEYUP)
    kb = KEYBDINPUT(wVk=0, wScan=codepoint, dwFlags=flags, time=0, dwExtraInfo=0)
    inp = INPUT(type=INPUT_KEYBOARD, union=INPUT_UNION(ki=kb))
    user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))


def _send_mouse_flags(flags: int, mouse_data: int = 0) -> None:
    mi = MOUSEINPUT(dx=0, dy=0, mouseData=mouse_data, dwFlags=flags, time=0, dwExtraInfo=0)
    inp = INPUT(type=INPUT_MOUSE, union=INPUT_UNION(mi=mi))
    user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))


def get_active_window_title() -> str:
    hwnd = user32.GetForegroundWindow()
    if not hwnd:
        return ""
    length = user32.GetWindowTextLengthW(hwnd)
    if length <= 0:
        return ""
    buffer = ctypes.create_unicode_buffer(length + 1)
    user32.GetWindowTextW(hwnd, buffer, length + 1)
    return buffer.value
