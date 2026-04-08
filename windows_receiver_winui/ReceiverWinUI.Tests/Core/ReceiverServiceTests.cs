using ReceiverWinUI.Core;
using Xunit;

namespace ReceiverWinUI.Tests.Core;

public sealed class ReceiverServiceTests
{
    [Fact]
    public void GetSnapshot_ReturnsIdleUsbStateAndClearedServerError_ByDefault()
    {
        using var service = new ReceiverService();

        var snapshot = service.GetSnapshot();

        Assert.False(snapshot.ServerRunning);
        Assert.Equal(string.Empty, snapshot.LastServerError);
        Assert.Equal("NotReady", snapshot.UsbState.ToString());
        Assert.Equal("USB connect not started", snapshot.UsbMessage);
    }

    [Fact]
    public void SetUsbStatus_UpdatesSnapshot()
    {
        using var service = new ReceiverService();

        service.SetUsbStatus(new UsbConnectResult(
            UsbConnectState.Ready,
            "USB connect ready",
            AdbExecutablePath: @"C:\Android\platform-tools\adb.exe",
            DevicesResult: null,
            ReverseResult: null,
            AndroidCommandResult: null));

        var snapshot = service.GetSnapshot();

        Assert.Equal(UsbConnectState.Ready, snapshot.UsbState);
        Assert.Equal("USB connect ready", snapshot.UsbMessage);
    }

    [Fact]
    public void StartServer_RecordsLastServerError_OnFailure()
    {
        using var service = new ReceiverService();

        var ok = service.StartServer("0.0.0.0", 70000);

        var snapshot = service.GetSnapshot();

        Assert.False(ok);
        Assert.False(snapshot.ServerRunning);
        Assert.NotEqual(string.Empty, snapshot.LastServerError);
    }
}
