using System.Diagnostics;
using System.IO;

namespace ReceiverWinUI.Core;

public sealed class UsbConnectCoordinator
{
    private readonly Func<string?> _resolveAdbExecutable;
    private readonly Func<string, string, CancellationToken, Task<ProcessRunResult>> _runProcessAsync;

    public UsbConnectCoordinator(
        Func<string?>? resolveAdbExecutable = null,
        Func<string, string, CancellationToken, Task<ProcessRunResult>>? runProcessAsync = null)
    {
        _resolveAdbExecutable = resolveAdbExecutable ?? ResolveAdbExecutable;
        _runProcessAsync = runProcessAsync ?? RunProcessAsync;
    }

    public async Task<UsbConnectResult> ConnectAsync(int port = 8765, CancellationToken cancellationToken = default)
    {
        var adbExecutablePath = _resolveAdbExecutable();
        if (string.IsNullOrWhiteSpace(adbExecutablePath))
        {
            return new UsbConnectResult(
                UsbConnectState.AdbNotFound,
                "ADB executable could not be resolved.",
                null,
                null,
                null,
                null);
        }

        var devicesResult = await _runProcessAsync(adbExecutablePath, "devices", cancellationToken);
        if (devicesResult.ExitCode != 0)
        {
            return new UsbConnectResult(
                UsbConnectState.AdbDevicesFailed,
                "adb devices failed.",
                adbExecutablePath,
                devicesResult,
                null,
                null);
        }

        if (devicesResult.StandardOutput.Contains("\tunauthorized", StringComparison.Ordinal))
        {
            return new UsbConnectResult(
                UsbConnectState.DeviceUnauthorized,
                "ADB device is unauthorized.",
                adbExecutablePath,
                devicesResult,
                null,
                null);
        }

        if (!devicesResult.StandardOutput.Contains("\tdevice", StringComparison.Ordinal))
        {
            return new UsbConnectResult(
                UsbConnectState.DeviceNotDetected,
                "ADB device was not detected.",
                adbExecutablePath,
                devicesResult,
                null,
                null);
        }

        var reverseResult = await _runProcessAsync(
            adbExecutablePath,
            $"reverse tcp:{port} tcp:{port}",
            cancellationToken);
        if (reverseResult.ExitCode != 0)
        {
            return new UsbConnectResult(
                UsbConnectState.ReverseFailed,
                "adb reverse failed.",
                adbExecutablePath,
                devicesResult,
                reverseResult,
                null);
        }

        const int AndroidCommandPort = 8765;
        var androidCommandArguments =
            $"shell am start -a com.example.fluxmic.action.USB_CONNECT --es host 127.0.0.1 --ei port {AndroidCommandPort}";
        var androidCommandResult = await _runProcessAsync(
            adbExecutablePath,
            androidCommandArguments,
            cancellationToken);
        if (androidCommandResult.ExitCode != 0)
        {
            return new UsbConnectResult(
                UsbConnectState.AndroidCommandFailed,
                "Android USB connect command failed.",
                adbExecutablePath,
                devicesResult,
                reverseResult,
                androidCommandResult);
        }

        return new UsbConnectResult(
            UsbConnectState.Ready,
            "USB ADB connection is ready.",
            adbExecutablePath,
            devicesResult,
            reverseResult,
            androidCommandResult);
    }

    private static string? ResolveAdbExecutable()
    {
        foreach (var candidate in EnumerateAdbCandidates())
        {
            if (File.Exists(candidate))
            {
                return candidate;
            }
        }

        return null;
    }

    private static IEnumerable<string> EnumerateAdbCandidates()
    {
        foreach (var envVar in new[] { "ANDROID_SDK_ROOT", "ANDROID_HOME" })
        {
            var root = Environment.GetEnvironmentVariable(envVar);
            if (!string.IsNullOrWhiteSpace(root))
            {
                yield return Path.Combine(root, "platform-tools", "adb.exe");
            }
        }

        var pathValue = Environment.GetEnvironmentVariable("PATH");
        if (string.IsNullOrWhiteSpace(pathValue))
        {
            yield break;
        }

        foreach (var entry in pathValue.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            yield return Path.Combine(entry, "adb.exe");
        }
    }

    private static async Task<ProcessRunResult> RunProcessAsync(
        string fileName,
        string arguments,
        CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();

        using var process = new Process
        {
            StartInfo = new ProcessStartInfo
            {
                FileName = fileName,
                Arguments = arguments,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            }
        };

        process.Start();
        using var cancellationRegistration = cancellationToken.Register(() => TryKillProcess(process));
        var standardOutputTask = process.StandardOutput.ReadToEndAsync();
        var standardErrorTask = process.StandardError.ReadToEndAsync();

        try
        {
            await process.WaitForExitAsync(cancellationToken);
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
            TryKillProcess(process);
            throw;
        }

        var standardOutput = await standardOutputTask;
        var standardError = await standardErrorTask;

        return new ProcessRunResult(process.ExitCode, standardOutput, standardError);
    }

    private static void TryKillProcess(Process process)
    {
        try
        {
            if (!process.HasExited)
            {
                process.Kill(entireProcessTree: true);
            }
        }
        catch
        {
            // ignore kill races on cancellation/exit
        }
    }
}
