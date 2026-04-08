namespace ReceiverWinUI.Core;

public sealed record ProcessRunResult(int ExitCode, string StandardOutput, string StandardError);

public enum UsbConnectState
{
    NotReady,
    AdbNotFound,
    AdbDevicesFailed,
    DeviceNotDetected,
    DeviceUnauthorized,
    ReverseFailed,
    AndroidCommandFailed,
    Ready
}

public sealed record UsbConnectResult(
    UsbConnectState State,
    string Message,
    string? AdbExecutablePath,
    ProcessRunResult? DevicesResult,
    ProcessRunResult? ReverseResult,
    ProcessRunResult? AndroidCommandResult);
