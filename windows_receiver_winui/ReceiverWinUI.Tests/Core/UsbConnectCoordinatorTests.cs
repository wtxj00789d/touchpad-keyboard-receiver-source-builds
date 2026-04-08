using System.Diagnostics;
using System.Reflection;
using ReceiverWinUI.Core;
using Xunit;

namespace ReceiverWinUI.Tests.Core;

public sealed class UsbConnectCoordinatorTests
{
    [Fact]
    public async Task ConnectAsync_ReturnsAdbNotFound_WhenExecutableCannotBeResolved()
    {
        var coordinator = new UsbConnectCoordinator(
            resolveAdbExecutable: () => null,
            runProcessAsync: (_, _, _) => throw new InvalidOperationException("process should not run"));

        var result = await coordinator.ConnectAsync();

        Assert.Equal(UsbConnectState.AdbNotFound, result.State);
        Assert.Null(result.AdbExecutablePath);
        Assert.Null(result.DevicesResult);
        Assert.Null(result.ReverseResult);
        Assert.Null(result.AndroidCommandResult);
    }

    [Fact]
    public async Task ConnectAsync_ReturnsDeviceUnauthorized_WhenAdbReportsUnauthorizedDevice()
    {
        var coordinator = CreateCoordinator((_, arguments) =>
            arguments == "devices"
                ? new ProcessRunResult(0, "List of devices attached\r\nemulator-5554\tunauthorized\r\n", string.Empty)
                : throw new InvalidOperationException("only adb devices should run"));

        var result = await coordinator.ConnectAsync(8765);

        Assert.Equal(UsbConnectState.DeviceUnauthorized, result.State);
        Assert.NotNull(result.DevicesResult);
        Assert.Null(result.ReverseResult);
        Assert.Null(result.AndroidCommandResult);
    }

    [Fact]
    public async Task ConnectAsync_ReturnsAdbDevicesFailed_WhenAdbDevicesCommandFails()
    {
        var coordinator = CreateCoordinator((_, arguments) =>
            arguments == "devices"
                ? new ProcessRunResult(1, string.Empty, "adb offline")
                : throw new InvalidOperationException("only adb devices should run"));

        var result = await coordinator.ConnectAsync(8765);

        Assert.Equal(UsbConnectState.AdbDevicesFailed, result.State);
        Assert.NotNull(result.DevicesResult);
        Assert.Null(result.ReverseResult);
        Assert.Null(result.AndroidCommandResult);
    }

    [Fact]
    public async Task ConnectAsync_ReturnsDeviceNotDetected_WhenAdbReportsNoReadyDevice()
    {
        var coordinator = CreateCoordinator((_, arguments) =>
            arguments == "devices"
                ? new ProcessRunResult(0, "List of devices attached\r\n\r\n", string.Empty)
                : throw new InvalidOperationException("only adb devices should run"));

        var result = await coordinator.ConnectAsync(8765);

        Assert.Equal(UsbConnectState.DeviceNotDetected, result.State);
        Assert.NotNull(result.DevicesResult);
        Assert.Null(result.ReverseResult);
        Assert.Null(result.AndroidCommandResult);
    }

    [Fact]
    public async Task ConnectAsync_ReturnsReady_WhenAllAdbStepsSucceed()
    {
        var calls = new List<(string FileName, string Arguments)>();
        var coordinator = new UsbConnectCoordinator(
            resolveAdbExecutable: () => @"C:\Android\platform-tools\adb.exe",
            runProcessAsync: (fileName, arguments, _) =>
            {
                calls.Add((fileName, arguments));
                return Task.FromResult(arguments == "devices"
                    ? new ProcessRunResult(0, "List of devices attached\r\nemulator-5554\tdevice\r\n", string.Empty)
                    : new ProcessRunResult(0, "ok", string.Empty));
            });

        var result = await coordinator.ConnectAsync(8765);

        Assert.Equal(UsbConnectState.Ready, result.State);
        Assert.Equal(@"C:\Android\platform-tools\adb.exe", result.AdbExecutablePath);
        Assert.NotNull(result.DevicesResult);
        Assert.NotNull(result.ReverseResult);
        Assert.NotNull(result.AndroidCommandResult);

        Assert.Collection(
            calls,
            call =>
            {
                Assert.Equal(@"C:\Android\platform-tools\adb.exe", call.FileName);
                Assert.Equal("devices", call.Arguments);
            },
            call =>
            {
                Assert.Equal(@"C:\Android\platform-tools\adb.exe", call.FileName);
                Assert.Equal("reverse tcp:8765 tcp:8765", call.Arguments);
            },
            call =>
            {
                Assert.Equal(@"C:\Android\platform-tools\adb.exe", call.FileName);
                Assert.Equal(
                    "shell am start -a com.example.fluxmic.action.USB_CONNECT --es host 127.0.0.1 --ei port 8765",
                    call.Arguments);
            });
    }

    [Fact]
    public async Task ConnectAsync_ReturnsReverseFailed_WhenAdbReverseFails()
    {
        var coordinator = CreateCoordinator((_, arguments) => arguments switch
        {
            "devices" => new ProcessRunResult(0, "List of devices attached\r\nemulator-5554\tdevice\r\n", string.Empty),
            "reverse tcp:8765 tcp:8765" => new ProcessRunResult(1, string.Empty, "reverse failed"),
            _ => throw new InvalidOperationException("android command should not run")
        });

        var result = await coordinator.ConnectAsync(8765);

        Assert.Equal(UsbConnectState.ReverseFailed, result.State);
        Assert.NotNull(result.DevicesResult);
        Assert.NotNull(result.ReverseResult);
        Assert.Null(result.AndroidCommandResult);
    }

    [Fact]
    public async Task ConnectAsync_ReturnsAndroidCommandFailed_WhenAndroidLaunchFails()
    {
        var coordinator = CreateCoordinator((_, arguments) => arguments switch
        {
            "devices" => new ProcessRunResult(0, "List of devices attached\r\nemulator-5554\tdevice\r\n", string.Empty),
            "reverse tcp:8765 tcp:8765" => new ProcessRunResult(0, "ok", string.Empty),
            "shell am start -a com.example.fluxmic.action.USB_CONNECT --es host 127.0.0.1 --ei port 8765" =>
                new ProcessRunResult(1, string.Empty, "android command failed"),
            _ => throw new InvalidOperationException($"unexpected command: {arguments}")
        });

        var result = await coordinator.ConnectAsync(8765);

        Assert.Equal(UsbConnectState.AndroidCommandFailed, result.State);
        Assert.NotNull(result.DevicesResult);
        Assert.NotNull(result.ReverseResult);
        Assert.NotNull(result.AndroidCommandResult);
    }

    [Fact]
    public async Task ConnectAsync_UsesRequestedAndroidCommandPort_WhenCallerSuppliesDifferentPort()
    {
        var calls = new List<string>();
        var coordinator = CreateCoordinator((_, arguments) =>
        {
            calls.Add(arguments);
            return arguments switch
            {
                "devices" => new ProcessRunResult(0, "List of devices attached\r\nemulator-5554\tdevice\r\n", string.Empty),
                "reverse tcp:9000 tcp:9000" => new ProcessRunResult(0, "ok", string.Empty),
                "shell am start -a com.example.fluxmic.action.USB_CONNECT --es host 127.0.0.1 --ei port 9000" =>
                    new ProcessRunResult(0, "ok", string.Empty),
                _ => throw new InvalidOperationException($"unexpected command: {arguments}")
            };
        });

        var result = await coordinator.ConnectAsync(9000);

        Assert.Equal(UsbConnectState.Ready, result.State);
        Assert.Contains(
            "shell am start -a com.example.fluxmic.action.USB_CONNECT --es host 127.0.0.1 --ei port 9000",
            calls);
    }

    [Fact]
    public async Task RunProcessAsync_KillsProcess_WhenCancellationIsRequestedAfterStart()
    {
        var tempRoot = Path.Combine(Path.GetTempPath(), $"usb-connect-tests-{Guid.NewGuid():N}");
        Directory.CreateDirectory(tempRoot);
        var scriptPath = Path.Combine(tempRoot, "sleep.ps1");
        var pidPath = Path.Combine(tempRoot, "pid.txt");

        await File.WriteAllTextAsync(
            scriptPath,
            $"Set-Content -LiteralPath '{pidPath}' -Value $PID{Environment.NewLine}Start-Sleep -Seconds 30");

        using var cancellationTokenSource = new CancellationTokenSource(TimeSpan.FromMilliseconds(500));

        try
        {
            await Assert.ThrowsAnyAsync<OperationCanceledException>(() =>
                InvokeRunProcessAsync(
                    "powershell.exe",
                    $"-NoProfile -ExecutionPolicy Bypass -File \"{scriptPath}\"",
                    cancellationTokenSource.Token));

            var processId = await WaitForPidFileAsync(pidPath);
            await WaitForProcessExitAsync(processId);
        }
        finally
        {
            try
            {
                Directory.Delete(tempRoot, recursive: true);
            }
            catch
            {
                // ignore cleanup failures in test temp dir
            }
        }
    }

    private static UsbConnectCoordinator CreateCoordinator(
        Func<string, string, ProcessRunResult> processResultFactory)
    {
        return new UsbConnectCoordinator(
            resolveAdbExecutable: () => @"C:\Android\platform-tools\adb.exe",
            runProcessAsync: (fileName, arguments, _) =>
                Task.FromResult(processResultFactory(fileName, arguments)));
    }

    private static Task<ProcessRunResult> InvokeRunProcessAsync(
        string fileName,
        string arguments,
        CancellationToken cancellationToken)
    {
        var method = typeof(UsbConnectCoordinator).GetMethod("RunProcessAsync", BindingFlags.NonPublic | BindingFlags.Static);
        Assert.NotNull(method);
        return (Task<ProcessRunResult>)method.Invoke(null, [fileName, arguments, cancellationToken])!;
    }

    private static async Task<int> WaitForPidFileAsync(string pidPath)
    {
        var deadline = DateTime.UtcNow.AddSeconds(5);
        while (DateTime.UtcNow < deadline)
        {
            if (File.Exists(pidPath))
            {
                var text = await File.ReadAllTextAsync(pidPath);
                if (int.TryParse(text.Trim(), out var processId))
                {
                    return processId;
                }
            }

            await Task.Delay(100);
        }

        throw new TimeoutException("Timed out waiting for child process PID file.");
    }

    private static async Task WaitForProcessExitAsync(int processId)
    {
        var deadline = DateTime.UtcNow.AddSeconds(5);
        while (DateTime.UtcNow < deadline)
        {
            try
            {
                using var process = Process.GetProcessById(processId);
                if (process.HasExited)
                {
                    return;
                }
            }
            catch (ArgumentException)
            {
                return;
            }

            await Task.Delay(100);
        }

        throw new TimeoutException($"Timed out waiting for process {processId} to exit.");
    }
}
