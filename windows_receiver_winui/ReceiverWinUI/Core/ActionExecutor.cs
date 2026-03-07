using System.Runtime.InteropServices;
using System.Text.Json;

namespace ReceiverWinUI.Core;

public sealed record ActionResult(bool Ok, string Message);

public sealed class ActionExecutor
{
    private static readonly HashSet<string> ModifierTokens = new(StringComparer.OrdinalIgnoreCase)
    {
        "CTRL", "LCTRL", "RCTRL",
        "SHIFT", "LSHIFT", "RSHIFT",
        "ALT", "LALT", "RALT",
        "WIN", "LWIN", "RWIN"
    };

    private static readonly Dictionary<string, ushort> Vk = BuildVkMap();

    private readonly Action<string> _log;

    public ActionExecutor(Action<string> log)
    {
        _log = log;
    }

    public bool Enabled { get; private set; } = true;

    public void SetEnabled(bool enabled)
    {
        Enabled = enabled;
        _log($"Action execution {(Enabled ? "enabled" : "disabled")}");
    }

    public bool ToggleEnabled()
    {
        SetEnabled(!Enabled);
        return Enabled;
    }

    public ActionResult ExecuteAction(JsonElement actionMessage)
    {
        var kind = actionMessage.TryGetProperty("kind", out var kindElement)
            ? kindElement.GetString()?.Trim().ToUpperInvariant() ?? string.Empty
            : string.Empty;

        if (!Enabled)
        {
            var ignored = $"Ignored {kind}: actions disabled";
            _log(ignored);
            return new ActionResult(true, ignored);
        }

        try
        {
            var payload = actionMessage.TryGetProperty("payload", out var payloadElement)
                ? payloadElement
                : default;

            string message;
            switch (kind)
            {
                case "KEY":
                {
                    var combo = JsonAsString(payload);
                    SendKeyCombo(combo);
                    message = $"KEY {combo}";
                    break;
                }
                case "TEXT":
                {
                    var text = JsonAsString(payload);
                    SendText(text);
                    message = $"TEXT len={text.Length}";
                    break;
                }
                case "MOUSE":
                {
                    var mouseMessage = SendMouse(payload);
                    message = $"MOUSE {mouseMessage}";
                    break;
                }
                case "MACRO":
                {
                    RunMacro(payload);
                    message = "MACRO";
                    break;
                }
                case "CMD":
                case "TOGGLE":
                    message = $"{kind} forwarded/logged";
                    break;
                default:
                    message = $"Unknown action kind: {kind}";
                    break;
            }

            _log(message);
            return new ActionResult(true, message);
        }
        catch (Exception ex)
        {
            var error = $"Action failed ({kind}): {ex.Message}";
            _log(error);
            return new ActionResult(false, error);
        }
    }

    public static string GetActiveWindowTitle()
    {
        var hwnd = GetForegroundWindow();
        if (hwnd == IntPtr.Zero)
        {
            return string.Empty;
        }

        var len = GetWindowTextLengthW(hwnd);
        if (len <= 0)
        {
            return string.Empty;
        }

        var buffer = new char[len + 1];
        return GetWindowTextW(hwnd, buffer, buffer.Length) > 0
            ? new string(buffer, 0, len)
            : string.Empty;
    }

    private static string JsonAsString(JsonElement element)
    {
        return element.ValueKind switch
        {
            JsonValueKind.String => element.GetString() ?? string.Empty,
            JsonValueKind.Undefined => string.Empty,
            JsonValueKind.Null => string.Empty,
            _ => element.GetRawText()
        };
    }

    private void RunMacro(JsonElement payload)
    {
        if (payload.ValueKind != JsonValueKind.Array)
        {
            return;
        }

        foreach (var step in payload.EnumerateArray())
        {
            if (step.ValueKind != JsonValueKind.Object)
            {
                continue;
            }

            var op = step.TryGetProperty("op", out var opElement)
                ? (opElement.GetString() ?? string.Empty).Trim().ToUpperInvariant()
                : string.Empty;

            switch (op)
            {
                case "KEY":
                    SendKeyCombo(step.TryGetProperty("value", out var keyValue) ? JsonAsString(keyValue) : string.Empty);
                    break;
                case "TEXT":
                    SendText(step.TryGetProperty("value", out var textValue) ? JsonAsString(textValue) : string.Empty);
                    break;
                case "MOUSE":
                    SendMouse(step.TryGetProperty("value", out var mouseValue)
                        ? mouseValue
                        : default);
                    break;
                case "DELAY":
                {
                    var delayMs = 0;
                    if (step.TryGetProperty("ms", out var msElement) && msElement.TryGetInt32(out var parsedMs))
                    {
                        delayMs = parsedMs;
                    }
                    else if (step.TryGetProperty("value", out var valueElement) && valueElement.TryGetInt32(out var parsedValue))
                    {
                        delayMs = parsedValue;
                    }

                    if (delayMs > 0)
                    {
                        Thread.Sleep(delayMs);
                    }

                    break;
                }
            }
        }
    }

    private static void SendKeyCombo(string combo)
    {
        var tokens = combo.Split('+', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(t => t.ToUpperInvariant())
            .ToList();
        if (tokens.Count == 0)
        {
            return;
        }

        var modifierTokens = tokens.Where(t => ModifierTokens.Contains(t)).ToList();
        var mainTokens = tokens.Where(t => !ModifierTokens.Contains(t)).ToList();

        if (mainTokens.Count == 0 && modifierTokens.Count > 0)
        {
            mainTokens.Add(modifierTokens[^1]);
            modifierTokens.RemoveAt(modifierTokens.Count - 1);
        }

        foreach (var modifier in modifierTokens)
        {
            if (TryResolveVk(modifier, out var vkCode))
            {
                SendVirtualKey(vkCode, down: true);
            }
        }

        foreach (var token in mainTokens)
        {
            if (!TryResolveVk(token, out var vkCode))
            {
                continue;
            }

            SendVirtualKey(vkCode, down: true);
            SendVirtualKey(vkCode, down: false);
        }

        for (var i = modifierTokens.Count - 1; i >= 0; i--)
        {
            if (TryResolveVk(modifierTokens[i], out var vkCode))
            {
                SendVirtualKey(vkCode, down: false);
            }
        }
    }

    private static void SendText(string text)
    {
        foreach (var ch in text)
        {
            SendUnicode(ch, down: true);
            SendUnicode(ch, down: false);
        }
    }

    private static string SendMouse(JsonElement payload)
    {
        if (payload.ValueKind == JsonValueKind.Object)
        {
            var op = payload.TryGetProperty("op", out var opElement)
                ? (opElement.GetString() ?? string.Empty).Trim().ToUpperInvariant()
                : string.Empty;
            var button = payload.TryGetProperty("button", out var buttonElement)
                ? (buttonElement.GetString() ?? "left").Trim().ToLowerInvariant()
                : "left";

            switch (op)
            {
                case "MOVE_REL":
                {
                    var dx = payload.TryGetProperty("dx", out var dxElement) && dxElement.TryGetInt32(out var parsedDx)
                        ? parsedDx
                        : 0;
                    var dy = payload.TryGetProperty("dy", out var dyElement) && dyElement.TryGetInt32(out var parsedDy)
                        ? parsedDy
                        : 0;
                    SendMouseMove(dx, dy);
                    return $"MOVE_REL dx={dx} dy={dy}";
                }
                case "SCROLL":
                {
                    var delta = payload.TryGetProperty("delta", out var deltaElement) && deltaElement.TryGetInt32(out var parsedDelta)
                        ? parsedDelta
                        : 0;
                    SendMouseFlags(MouseEventfWheel, NormalizeWheelDelta(delta));
                    return $"SCROLL delta={delta}";
                }
                case "BUTTON_DOWN":
                    SendMouseButton(button, down: true);
                    return $"BUTTON_DOWN {button}";
                case "BUTTON_UP":
                    SendMouseButton(button, down: false);
                    return $"BUTTON_UP {button}";
                case "CLICK":
                    SendMouseClick(button);
                    return $"CLICK {button}";
                case "DOUBLE_CLICK":
                    SendMouseDoubleClick(button);
                    return $"DOUBLE_CLICK {button}";
                case "LEFT_CLICK":
                    SendMouseClick("left");
                    return "LEFT_CLICK";
                case "RIGHT_CLICK":
                    SendMouseClick("right");
                    return "RIGHT_CLICK";
                case "WHEEL_UP":
                    SendMouseFlags(MouseEventfWheel, 120);
                    return "WHEEL_UP";
                case "WHEEL_DOWN":
                    SendMouseFlags(MouseEventfWheel, unchecked((uint)-120));
                    return "WHEEL_DOWN";
            }

            return $"UNKNOWN_OP {op}";
        }

        var command = JsonAsString(payload).Trim().ToUpperInvariant();
        if (string.IsNullOrWhiteSpace(command))
        {
            return "EMPTY";
        }

        switch (command)
        {
            case "LEFT_CLICK":
                SendMouseClick("left");
                return "LEFT_CLICK";
            case "DOUBLE_CLICK":
                SendMouseDoubleClick("left");
                return "DOUBLE_CLICK";
            case "RIGHT_CLICK":
                SendMouseClick("right");
                return "RIGHT_CLICK";
            case "WHEEL_UP":
                SendMouseFlags(MouseEventfWheel, 120);
                return "WHEEL_UP";
            case "WHEEL_DOWN":
                SendMouseFlags(MouseEventfWheel, unchecked((uint)-120));
                return "WHEEL_DOWN";
        }

        return command;
    }

    private static void SendMouseButton(string button, bool down)
    {
        var normalized = button.Trim().ToLowerInvariant();
        switch (normalized)
        {
            case "right":
                SendMouseFlags(down ? MouseEventfRightDown : MouseEventfRightUp);
                break;
            default:
                SendMouseFlags(down ? MouseEventfLeftDown : MouseEventfLeftUp);
                break;
        }
    }

    private static void SendMouseClick(string button)
    {
        SendMouseButton(button, down: true);
        SendMouseButton(button, down: false);
    }

    private static void SendMouseDoubleClick(string button)
    {
        SendMouseClick(button);
        SendMouseClick(button);
    }

    private static uint NormalizeWheelDelta(int delta)
    {
        if (delta == 0)
        {
            return 0;
        }

        if (Math.Abs(delta) <= 10)
        {
            delta *= 30;
        }

        return unchecked((uint)delta);
    }

    private static bool TryResolveVk(string token, out ushort vkCode)
    {
        if (Vk.TryGetValue(token, out vkCode))
        {
            return true;
        }

        if (token.Length == 1)
        {
            return Vk.TryGetValue(token.ToUpperInvariant(), out vkCode);
        }

        return false;
    }

    private static Dictionary<string, ushort> BuildVkMap()
    {
        var map = new Dictionary<string, ushort>(StringComparer.OrdinalIgnoreCase)
        {
            ["CTRL"] = 0x11,
            ["LCTRL"] = 0xA2,
            ["RCTRL"] = 0xA3,
            ["SHIFT"] = 0x10,
            ["LSHIFT"] = 0xA0,
            ["RSHIFT"] = 0xA1,
            ["ALT"] = 0x12,
            ["LALT"] = 0xA4,
            ["RALT"] = 0xA5,
            ["WIN"] = 0x5B,
            ["LWIN"] = 0x5B,
            ["RWIN"] = 0x5C,
            ["MENU"] = 0x5D,
            ["APPS"] = 0x5D,
            ["TAB"] = 0x09,
            ["ENTER"] = 0x0D,
            ["ESC"] = 0x1B,
            ["SPACE"] = 0x20,
            ["BACKSPACE"] = 0x08,
            ["BKSP"] = 0x08,
            ["CAPS"] = 0x14,
            ["CAPSLOCK"] = 0x14,
            ["DELETE"] = 0x2E,
            ["DEL"] = 0x2E,
            ["UP"] = 0x26,
            ["DOWN"] = 0x28,
            ["LEFT"] = 0x25,
            ["RIGHT"] = 0x27,
            ["HOME"] = 0x24,
            ["END"] = 0x23,
            ["PGUP"] = 0x21,
            ["PGDN"] = 0x22,
            ["VOLUME_UP"] = 0xAF,
            ["VOLUME_DOWN"] = 0xAE,
            ["MEDIA_NEXT"] = 0xB0,
            ["MEDIA_PREV"] = 0xB1,
            ["MINUS"] = 0xBD,
            ["EQUALS"] = 0xBB,
            ["LBRACKET"] = 0xDB,
            ["RBRACKET"] = 0xDD,
            ["BACKSLASH"] = 0xDC,
            ["SEMICOLON"] = 0xBA,
            ["APOSTROPHE"] = 0xDE,
            ["COMMA"] = 0xBC,
            ["PERIOD"] = 0xBE,
            ["SLASH"] = 0xBF,
            ["GRAVE"] = 0xC0,
            ["BACKTICK"] = 0xC0,
            ["-"] = 0xBD,
            ["="] = 0xBB,
            ["["] = 0xDB,
            ["]"] = 0xDD,
            ["\\"] = 0xDC,
            [";"] = 0xBA,
            ["'"] = 0xDE,
            [","] = 0xBC,
            ["."] = 0xBE,
            ["/"] = 0xBF,
            ["`"] = 0xC0
        };

        for (ushort i = 1; i <= 24; i++)
        {
            map[$"F{i}"] = (ushort)(0x6F + i);
        }

        for (ushort i = 0; i <= 9; i++)
        {
            map[$"{i}"] = (ushort)(0x30 + i);
        }

        for (var c = 'A'; c <= 'Z'; c++)
        {
            map[$"{c}"] = c;
        }

        return map;
    }

    private static void SendVirtualKey(ushort keyCode, bool down)
    {
        var input = new INPUT
        {
            Type = InputKeyboard,
            Data = new InputUnion
            {
                Ki = new KEYBDINPUT
                {
                    Vk = keyCode,
                    Scan = 0,
                    Flags = down ? 0u : KeyEventfKeyUp,
                    Time = 0,
                    ExtraInfo = IntPtr.Zero
                }
            }
        };

        SendInput(1, [input], Marshal.SizeOf<INPUT>());
    }

    private static void SendUnicode(char ch, bool down)
    {
        var input = new INPUT
        {
            Type = InputKeyboard,
            Data = new InputUnion
            {
                Ki = new KEYBDINPUT
                {
                    Vk = 0,
                    Scan = ch,
                    Flags = KeyEventfUnicode | (down ? 0u : KeyEventfKeyUp),
                    Time = 0,
                    ExtraInfo = IntPtr.Zero
                }
            }
        };

        SendInput(1, [input], Marshal.SizeOf<INPUT>());
    }

    private static void SendMouseMove(int dx, int dy)
    {
        if (dx == 0 && dy == 0)
        {
            return;
        }

        if (TryMoveCursorAbsolute(dx, dy))
        {
            return;
        }

        var input = new INPUT
        {
            Type = InputMouse,
            Data = new InputUnion
            {
                Mi = new MOUSEINPUT
                {
                    Dx = dx,
                    Dy = dy,
                    MouseData = 0,
                    Flags = MouseEventfMove,
                    Time = 0,
                    ExtraInfo = IntPtr.Zero
                }
            }
        };

        SendInput(1, [input], Marshal.SizeOf<INPUT>());
    }

    private static bool TryMoveCursorAbsolute(int dx, int dy)
    {
        if (!GetCursorPos(out var pt))
        {
            return false;
        }

        return SetCursorPos(pt.X + dx, pt.Y + dy);
    }

    private static void SendMouseFlags(uint flags, uint mouseData = 0, int dx = 0, int dy = 0)
    {
        var input = new INPUT
        {
            Type = InputMouse,
            Data = new InputUnion
            {
                Mi = new MOUSEINPUT
                {
                    Dx = dx,
                    Dy = dy,
                    MouseData = mouseData,
                    Flags = flags,
                    Time = 0,
                    ExtraInfo = IntPtr.Zero
                }
            }
        };

        SendInput(1, [input], Marshal.SizeOf<INPUT>());
    }

    private const uint InputMouse = 0;
    private const uint InputKeyboard = 1;

    private const uint KeyEventfKeyUp = 0x0002;
    private const uint KeyEventfUnicode = 0x0004;

    private const uint MouseEventfLeftDown = 0x0002;
    private const uint MouseEventfLeftUp = 0x0004;
    private const uint MouseEventfRightDown = 0x0008;
    private const uint MouseEventfRightUp = 0x0010;
    private const uint MouseEventfMove = 0x0001;
    private const uint MouseEventfWheel = 0x0800;

    [StructLayout(LayoutKind.Sequential)]
    private struct INPUT
    {
        public uint Type;
        public InputUnion Data;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)]
        public MOUSEINPUT Mi;

        [FieldOffset(0)]
        public KEYBDINPUT Ki;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MOUSEINPUT
    {
        public int Dx;
        public int Dy;
        public uint MouseData;
        public uint Flags;
        public uint Time;
        public IntPtr ExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct POINT
    {
        public int X;
        public int Y;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KEYBDINPUT
    {
        public ushort Vk;
        public ushort Scan;
        public uint Flags;
        public uint Time;
        public IntPtr ExtraInfo;
    }

    [DllImport("user32.dll", SetLastError = true)]
    private static extern uint SendInput(uint numberOfInputs, INPUT[] inputs, int sizeOfInputStructure);

    [DllImport("user32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool GetCursorPos(out POINT point);

    [DllImport("user32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool SetCursorPos(int x, int y);

    [DllImport("user32.dll")]
    private static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetWindowTextW(IntPtr hWnd, [Out] char[] text, int maxCount);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetWindowTextLengthW(IntPtr hWnd);
}
