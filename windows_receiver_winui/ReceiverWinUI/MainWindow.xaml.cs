using System.ComponentModel;
using System.Drawing;
using System.Windows;
using System.Windows.Threading;
using ReceiverWinUI.Core;
using Forms = System.Windows.Forms;

namespace ReceiverWinUI;

public partial class MainWindow : Window
{
    private readonly ReceiverService _service;
    private readonly DispatcherTimer _uiTimer;

    private Forms.NotifyIcon? _trayIcon;

    public MainWindow()
    {
        InitializeComponent();

        _service = new ReceiverService();
        _service.StateChanged += OnServiceStateChanged;

        _uiTimer = new DispatcherTimer
        {
            Interval = TimeSpan.FromMilliseconds(300)
        };
        _uiTimer.Tick += (_, _) => RefreshUi();

        Loaded += OnLoaded;
        Closing += OnClosing;
        StateChanged += OnWindowStateChanged;
    }

    private async void OnLoaded(object sender, RoutedEventArgs e)
    {
        _uiTimer.Start();
        RefreshDevices();
        RefreshUi();

        if (_service.IsServerRunning)
        {
            StartStopButton.Content = "Stop Server";
        }

        await StartupDriverCheckAsync();
    }

    private void OnServiceStateChanged()
    {
        Dispatcher.BeginInvoke(new Action(RefreshUi));
    }

    private void RefreshUi()
    {
        var snapshot = _service.GetSnapshot();

        ServerStatusValue.Text = snapshot.ServerRunning ? "Running" : "Stopped";
        ServerAddressValue.Text = snapshot.ServerRunning ? snapshot.ServerAddress : "-";
        ClientIpValue.Text = snapshot.Connected ? snapshot.ClientIp : "-";
        RttValue.Text = snapshot.RttMs.HasValue ? $"{snapshot.RttMs.Value} ms" : "-";
        JitterValue.Text = $"{snapshot.JitterMs} ms";
        BufferedValue.Text = $"{snapshot.BufferedMs} ms";
        MuteValue.Text = snapshot.Mute.ToString();
        ActionsValue.Text = snapshot.ActionsEnabled ? "Enabled" : "Disabled";
        CodecValue.Text = snapshot.Codec;
        DriverValue.Text = snapshot.DriverInstalled ? "Installed" : "Missing";
        CurrentOutputValue.Text = snapshot.SelectedDevice;

        StartStopButton.Content = snapshot.ServerRunning ? "Stop Server" : "Start Server";
        ToggleActionsButton.Content = snapshot.ActionsEnabled ? "Disable Actions" : "Enable Actions";
        ToggleMuteButton.Content = snapshot.Mute ? "Unmute" : "Mute";

        LogsList.ItemsSource = _service.GetRecentLogs(50);
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
        var result = System.Windows.MessageBox.Show(
            "未检测到 VB-CABLE。\n\n" +
            "这会导致无法把音频注入系统麦克风。\n" +
            $"请先将离线安装包放到: {hint}\n\n" +
            "现在是否一键安装（需要管理员权限/UAC）？",
            "VB-CABLE Missing",
            MessageBoxButton.YesNo,
            MessageBoxImage.Warning);

        if (result == MessageBoxResult.Yes)
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

            System.Windows.MessageBox.Show(
                result.Message,
                result.Ok ? "Driver Installed" : "Driver Install Failed",
                MessageBoxButton.OK,
                result.Ok ? MessageBoxImage.Information : MessageBoxImage.Error);
        }
        finally
        {
            SetControlsEnabled(true);
        }
    }

    private void SetControlsEnabled(bool enabled)
    {
        InstallDriverButton.IsEnabled = enabled;
        RefreshDevicesButton.IsEnabled = enabled;
        ApplyOutputButton.IsEnabled = enabled;
        StartStopButton.IsEnabled = enabled;
        ToggleActionsButton.IsEnabled = enabled;
        ToggleMuteButton.IsEnabled = enabled;
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
            _service.StartServer();
        }

        RefreshUi();
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

    private void MinimizeTrayButton_Click(object sender, RoutedEventArgs e)
    {
        MinimizeToTray();
    }

    private void OnWindowStateChanged(object? sender, EventArgs e)
    {
        if (WindowState == WindowState.Minimized)
        {
            MinimizeToTray();
        }
    }

    private void MinimizeToTray()
    {
        EnsureTrayIcon();
        Hide();
        ShowInTaskbar = false;
        if (_trayIcon is not null)
        {
            _trayIcon.Visible = true;
        }
    }

    private void RestoreFromTray()
    {
        Show();
        ShowInTaskbar = true;
        WindowState = WindowState.Normal;
        Activate();
        if (_trayIcon is not null)
        {
            _trayIcon.Visible = false;
        }
    }

    private void EnsureTrayIcon()
    {
        if (_trayIcon is not null)
        {
            return;
        }

        _trayIcon = new Forms.NotifyIcon
        {
            Text = "FluxMic Receiver",
            Icon = SystemIcons.Application,
            Visible = false
        };

        var menu = new Forms.ContextMenuStrip();
        menu.Items.Add("Open", null, (_, _) => Dispatcher.Invoke(RestoreFromTray));
        menu.Items.Add("Exit", null, (_, _) => Dispatcher.Invoke(Close));

        _trayIcon.ContextMenuStrip = menu;
        _trayIcon.DoubleClick += (_, _) => Dispatcher.Invoke(RestoreFromTray);
    }

    private void OnClosing(object? sender, CancelEventArgs e)
    {
        _uiTimer.Stop();

        if (_trayIcon is not null)
        {
            _trayIcon.Visible = false;
            _trayIcon.Dispose();
            _trayIcon = null;
        }

        _service.Dispose();
    }
}


