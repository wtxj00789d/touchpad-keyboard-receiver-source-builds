using System.Collections.ObjectModel;
using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using ReceiverWinUI.Core;
using WinRT.Interop;
using Windows.Graphics;

namespace ReceiverWinUI;

public sealed partial class MainWindow : Window
{
    private const int MaxLogItems = 50;
    private const int UsbServerPort = 8765;

    private readonly ReceiverService _service;
    private readonly UsbConnectCoordinator _usbConnectCoordinator;
    private readonly Microsoft.UI.Dispatching.DispatcherQueueTimer _uiTimer;
    private readonly ObservableCollection<string> _logs = new();
    private readonly CancellationTokenSource _windowLifetimeCts = new();

    private AppWindow? _appWindow;
    private CancellationTokenSource? _usbConnectCts;
    private bool _initialized;
    private bool _isClosing;
    private bool _syncingVolumeSlider;

    public MainWindow()
    {
        InitializeComponent();

        _service = new ReceiverService();
        _usbConnectCoordinator = new UsbConnectCoordinator();
        _service.StateChanged += OnServiceStateChanged;
        _service.LogAdded += OnLogAdded;

        LogsList.ItemsSource = _logs;

        _uiTimer = DispatcherQueue.CreateTimer();
        _uiTimer.Interval = TimeSpan.FromMilliseconds(300);
        _uiTimer.IsRepeating = true;
        _uiTimer.Tick += (_, _) => RefreshUi();

        Closed += OnClosed;

        InitializeWindow();
        NavList.SelectedIndex = 0;
        ShowPage("overview");
    }

    private void InitializeWindow()
    {
        try
        {
            var hwnd = WindowNative.GetWindowHandle(this);
            var windowId = Microsoft.UI.Win32Interop.GetWindowIdFromWindow(hwnd);
            _appWindow = AppWindow.GetFromWindowId(windowId);
            _appWindow.Title = "FluxMic Receiver";
            _appWindow.Resize(new SizeInt32(1320, 840));
        }
        catch
        {
            // Ignore shell integration failures and continue with defaults.
        }
    }

    private async void RootGrid_Loaded(object sender, RoutedEventArgs e)
    {
        if (_initialized)
        {
            return;
        }

        _initialized = true;
        InitializeLogView();
        _uiTimer.Start();
        RefreshDevices();
        _service.EnsureServerStarted(port: UsbServerPort);
        RefreshUi();

        await StartupDriverCheckAsync();
    }

    private void NavList_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (NavList.SelectedItem is not ListBoxItem item)
        {
            return;
        }

        var tag = item.Tag as string ?? "overview";
        ShowPage(tag);
    }

    private void SearchBox_QuerySubmitted(AutoSuggestBox sender, AutoSuggestBoxQuerySubmittedEventArgs args)
    {
        var text = (args.QueryText ?? string.Empty).Trim().ToLowerInvariant();
        if (string.IsNullOrEmpty(text))
        {
            return;
        }

        if (text.Contains("audio") || text.Contains("driver") || text.Contains("device"))
        {
            SelectPage("audio");
            return;
        }

        if (text.Contains("mute") || text.Contains("action") || text.Contains("server") || text.Contains("control") || text.Contains("volume"))
        {
            SelectPage("controls");
            return;
        }

        if (text.Contains("log") || text.Contains("activity") || text.Contains("event"))
        {
            SelectPage("activity");
            return;
        }

        SelectPage("overview");
    }

    private void SelectPage(string pageKey)
    {
        foreach (var raw in NavList.Items)
        {
            if (raw is not ListBoxItem item)
            {
                continue;
            }

            if (string.Equals(item.Tag as string, pageKey, StringComparison.OrdinalIgnoreCase))
            {
                NavList.SelectedItem = item;
                break;
            }
        }

        ShowPage(pageKey);
    }

    private void ShowPage(string pageKey)
    {
        OverviewPage.Visibility = pageKey == "overview" ? Visibility.Visible : Visibility.Collapsed;
        AudioPage.Visibility = pageKey == "audio" ? Visibility.Visible : Visibility.Collapsed;
        ControlsPage.Visibility = pageKey == "controls" ? Visibility.Visible : Visibility.Collapsed;
        ActivityPage.Visibility = pageKey == "activity" ? Visibility.Visible : Visibility.Collapsed;
    }

    private void OnServiceStateChanged()
    {
        if (_isClosing)
        {
            return;
        }

        if (DispatcherQueue.HasThreadAccess)
        {
            RefreshUi();
            return;
        }

        DispatcherQueue.TryEnqueue(RefreshUi);
    }

    private void RefreshUi()
    {
        if (_isClosing)
        {
            return;
        }

        var snapshot = _service.GetSnapshot();

        ServerStatusValue.Text = snapshot.ServerRunning ? "Running" : "Stopped";
        ServerAddressValue.Text = snapshot.ServerRunning ? snapshot.ServerAddress : "-";
        ClientIpValue.Text = snapshot.Connected ? snapshot.ClientIp : "-";
        RttValue.Text = snapshot.RttMs.HasValue ? $"{snapshot.RttMs.Value} ms" : "-";
        JitterValue.Text = $"{snapshot.JitterMs} ms";
        BufferedValue.Text = $"{snapshot.BufferedMs} ms";
        MuteValue.Text = snapshot.Mute.ToString();
        ActionsValue.Text = snapshot.ActionsEnabled ? "Enabled" : "Disabled";
        ServerErrorValue.Text = string.IsNullOrWhiteSpace(snapshot.LastServerError) ? "-" : snapshot.LastServerError;
        UsbStatusValue.Text = snapshot.UsbState.ToString();
        UsbMessageValue.Text = string.IsNullOrWhiteSpace(snapshot.UsbMessage) ? "-" : snapshot.UsbMessage;
        CodecValue.Text = snapshot.Codec;
        DriverValue.Text = snapshot.DriverInstalled ? "Installed" : "Missing";
        CurrentOutputValue.Text = snapshot.SelectedDevice;

        var volumePercent = snapshot.SystemVolume * 100.0;
        if (Math.Abs(SystemVolumeSlider.Value - volumePercent) > 0.5)
        {
            _syncingVolumeSlider = true;
            SystemVolumeSlider.Value = volumePercent;
            _syncingVolumeSlider = false;
        }
        SystemVolumeValueText.Text = $"{Math.Round(volumePercent)}%";

        StartStopButton.Content = snapshot.ServerRunning ? "Stop Server" : "Start Server";
        ToggleActionsButton.Content = snapshot.ActionsEnabled ? "Disable Actions" : "Enable Actions";
        ToggleMuteButton.Content = snapshot.Mute ? "Unmute" : "Mute";
    }

    private void RefreshDevices()
    {
        var devices = _service.ListOutputDevices();
        OutputDeviceCombo.ItemsSource = devices;

        if (devices.Count > 0)
        {
            var selectedName = _service.GetSnapshot().SelectedDevice;
            var selected = devices.FirstOrDefault(d => d.Name.Equals(selectedName, StringComparison.OrdinalIgnoreCase))
                           ?? devices.FirstOrDefault(d => d.Name.Contains("CABLE Input", StringComparison.OrdinalIgnoreCase))
                           ?? devices[0];
            OutputDeviceCombo.SelectedItem = selected;
        }

        _service.RefreshDriverStatus();
        RefreshUi();
    }

    private async Task StartupDriverCheckAsync()
    {
        _service.RefreshDriverStatus();
        if (_service.GetSnapshot().DriverInstalled)
        {
            return;
        }

        var hint = DriverInstaller.InstallerPathHint();
        var shouldInstall = await ShowConfirmDialogAsync(
            "VB-CABLE Missing",
            "VB-CABLE was not detected.\n\n" +
            "Audio injection into the system microphone may fail.\n" +
            $"Place offline installer package at: {hint}\n\n" +
            "Install now (requires administrator/UAC)?");

        if (shouldInstall)
        {
            await InstallDriverAsync();
        }
    }

    private async Task InstallDriverAsync()
    {
        SetControlsEnabled(false);
        try
        {
            var result = await Task.Run(() => DriverInstaller.Install());
            _service.RefreshDriverStatus();
            RefreshDevices();

            await ShowInfoDialogAsync(
                result.Ok ? "Driver Installed" : "Driver Install Failed",
                result.Message);
        }
        finally
        {
            SetControlsEnabled(true);
        }
    }

    private async Task<bool> ShowConfirmDialogAsync(string title, string content)
    {
        if (RootGrid.XamlRoot is null)
        {
            return false;
        }

        var dialog = new ContentDialog
        {
            Title = title,
            Content = content,
            PrimaryButtonText = "Yes",
            CloseButtonText = "No",
            DefaultButton = ContentDialogButton.Primary,
            XamlRoot = RootGrid.XamlRoot
        };

        var result = await dialog.ShowAsync();
        return result == ContentDialogResult.Primary;
    }

    private async Task ShowInfoDialogAsync(string title, string content)
    {
        if (RootGrid.XamlRoot is null)
        {
            return;
        }

        var dialog = new ContentDialog
        {
            Title = title,
            Content = content,
            CloseButtonText = "OK",
            XamlRoot = RootGrid.XamlRoot
        };

        await dialog.ShowAsync();
    }

    private void InitializeLogView()
    {
        _logs.Clear();
        foreach (var line in _service.GetRecentLogs(MaxLogItems))
        {
            _logs.Add(line);
        }
    }

    private void OnLogAdded(string line)
    {
        if (_isClosing)
        {
            return;
        }

        if (DispatcherQueue.HasThreadAccess)
        {
            AddLogLine(line);
            return;
        }

        DispatcherQueue.TryEnqueue(() => AddLogLine(line));
    }

    private void AddLogLine(string line)
    {
        if (_isClosing)
        {
            return;
        }

        if (_logs.Count > 0 && _logs[0] == line)
        {
            return;
        }

        _logs.Insert(0, line);
        while (_logs.Count > MaxLogItems)
        {
            _logs.RemoveAt(_logs.Count - 1);
        }
    }

    private void SetControlsEnabled(bool enabled)
    {
        if (_isClosing)
        {
            return;
        }

        InstallDriverButton.IsEnabled = enabled;
        RefreshDevicesButton.IsEnabled = enabled;
        ApplyOutputButton.IsEnabled = enabled;
        StartStopButton.IsEnabled = enabled;
        UsbConnectButton.IsEnabled = enabled;
        UsbRetryButton.IsEnabled = enabled;
        ToggleActionsButton.IsEnabled = enabled;
        ToggleMuteButton.IsEnabled = enabled;
        SystemVolumeSlider.IsEnabled = enabled;
    }

    private async void InstallDriverButton_Click(object sender, RoutedEventArgs e)
    {
        await InstallDriverAsync();
    }

    private void RefreshDevicesButton_Click(object sender, RoutedEventArgs e)
    {
        RefreshDevices();
    }

    private void ApplyOutputButton_Click(object sender, RoutedEventArgs e)
    {
        if (OutputDeviceCombo.SelectedItem is AudioDeviceInfo selected)
        {
            _service.SelectOutputDevice(selected.Id);
            RefreshUi();
        }
    }

    private void StartStopButton_Click(object sender, RoutedEventArgs e)
    {
        if (_service.IsServerRunning)
        {
            _service.StopServer();
        }
        else
        {
            _service.EnsureServerStarted(port: UsbServerPort);
        }

        RefreshUi();
    }

    private async void UsbConnectButton_Click(object sender, RoutedEventArgs e)
    {
        await RunUsbConnectAsync();
    }

    private async void UsbRetryButton_Click(object sender, RoutedEventArgs e)
    {
        await RunUsbConnectAsync();
    }

    private void ToggleActionsButton_Click(object sender, RoutedEventArgs e)
    {
        _service.ToggleActions();
        RefreshUi();
    }

    private void ToggleMuteButton_Click(object sender, RoutedEventArgs e)
    {
        var snapshot = _service.GetSnapshot();
        _service.SetMute(!snapshot.Mute);
        RefreshUi();
    }

    private void SystemVolumeSlider_ValueChanged(object sender, RangeBaseValueChangedEventArgs e)
    {
        if (_syncingVolumeSlider)
        {
            return;
        }

        var next = (float)(e.NewValue / 100.0);
        _service.SetSystemVolume(next);
        SystemVolumeValueText.Text = $"{Math.Round(e.NewValue)}%";
    }

    private void MinimizeTrayButton_Click(object sender, RoutedEventArgs e)
    {
        if (_appWindow?.Presenter is OverlappedPresenter presenter)
        {
            presenter.Minimize();
        }
    }

    private void OnClosed(object sender, WindowEventArgs args)
    {
        _isClosing = true;
        _windowLifetimeCts.Cancel();
        _usbConnectCts?.Cancel();
        _usbConnectCts?.Dispose();
        _usbConnectCts = null;
        _uiTimer.Stop();
        _service.StateChanged -= OnServiceStateChanged;
        _service.LogAdded -= OnLogAdded;
        _service.Dispose();
        _windowLifetimeCts.Dispose();
    }

    private async Task RunUsbConnectAsync()
    {
        if (_isClosing)
        {
            return;
        }

        SetControlsEnabled(false);
        CancellationTokenSource? usbConnectCts = null;
        try
        {
            _service.SetUsbStatus(new UsbConnectResult(
                UsbConnectState.NotReady,
                "Connecting over USB...",
                null,
                null,
                null,
                null));
            RefreshUi();

            var serverStarted = _service.EnsureServerStarted(port: UsbServerPort);
            if (!serverStarted)
            {
                if (_isClosing)
                {
                    return;
                }

                var snapshot = _service.GetSnapshot();
                _service.SetUsbStatus(new UsbConnectResult(
                    UsbConnectState.NotReady,
                    string.IsNullOrWhiteSpace(snapshot.LastServerError)
                        ? "Receiver server failed to start."
                        : $"Receiver server failed to start: {snapshot.LastServerError}",
                    null,
                    null,
                    null,
                    null));
                RefreshUi();
                return;
            }

            usbConnectCts = CancellationTokenSource.CreateLinkedTokenSource(_windowLifetimeCts.Token);
            _usbConnectCts = usbConnectCts;

            var result = await _usbConnectCoordinator.ConnectAsync(UsbServerPort, usbConnectCts.Token);
            if (_isClosing || usbConnectCts.IsCancellationRequested)
            {
                return;
            }

            _service.SetUsbStatus(result);
            RefreshUi();
        }
        catch (OperationCanceledException) when (_isClosing || (usbConnectCts?.IsCancellationRequested ?? false))
        {
            // Window is closing or the in-flight USB connect was canceled intentionally.
        }
        catch (Exception ex)
        {
            if (_isClosing || (usbConnectCts?.IsCancellationRequested ?? false))
            {
                return;
            }

            _service.SetUsbStatus(new UsbConnectResult(
                UsbConnectState.NotReady,
                $"USB connect failed: {ex.Message}",
                null,
                null,
                null,
                null));
            RefreshUi();
        }
        finally
        {
            if (ReferenceEquals(_usbConnectCts, usbConnectCts))
            {
                _usbConnectCts = null;
            }

            usbConnectCts?.Dispose();

            if (!_isClosing)
            {
                SetControlsEnabled(true);
            }
        }
    }
}
