using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text.Json;
using Fleck;

namespace ReceiverWinUI.Core;

public sealed record ReceiverSnapshot(
    bool ServerRunning,
    string ServerAddress,
    bool Connected,
    string ClientIp,
    int? RttMs,
    int JitterMs,
    int BufferedMs,
    bool Mute,
    bool ActionsEnabled,
    string Codec,
    string SelectedDevice,
    bool DriverInstalled);

public sealed class ReceiverService : IDisposable
{
    private sealed class ClientSession
    {
        public ClientSession(IWebSocketConnection socket, string ip)
        {
            Socket = socket;
            Ip = ip;
        }

        public IWebSocketConnection Socket { get; }
        public string Ip { get; }
        public string Role { get; set; } = "legacy";
        public string DeviceId { get; set; } = string.Empty;
        public DateTime ConnectedAtUtc { get; } = DateTime.UtcNow;
    }

    private readonly object _sync = new();
    private readonly AudioOutputEngine _audio;
    private readonly ActionExecutor _actions;
    private readonly List<string> _logs = new();
    private readonly JsonSerializerOptions _jsonOptions = new() { PropertyNamingPolicy = null };
    private readonly System.Threading.Timer _stateTimer;
    private readonly Dictionary<IWebSocketConnection, ClientSession> _clients = new();
    private readonly HashSet<IWebSocketConnection> _nonOwnerAudioWarned = new();

    private WebSocketServer? _server;
    private IWebSocketConnection? _audioClient;

    private bool _serverRunning;
    private string _serverAddress = "-";
    private bool _connected;
    private string _clientIp = string.Empty;
    private bool _mute;
    private int? _rttMs = null;
    private bool _driverInstalled;
    private bool _disposed;

    public ReceiverService()
    {
        FleckLog.Level = LogLevel.Warn;

        _audio = new AudioOutputEngine();
        _audio.Log += message => Log(message);
        _audio.SelectPreferredDevice();

        _actions = new ActionExecutor(message => Log(message));
        _driverInstalled = DriverInstaller.IsVbCableInstalled();

        _stateTimer = new System.Threading.Timer(_ => PushStateToClients(), null, TimeSpan.FromSeconds(1), TimeSpan.FromSeconds(1));
    }

    public event Action? StateChanged;
    public event Action<string>? LogAdded;

    public bool IsServerRunning => _serverRunning;

    public void RefreshDriverStatus()
    {
        _driverInstalled = DriverInstaller.IsVbCableInstalled();
        RaiseChanged();
    }

    public IReadOnlyList<AudioDeviceInfo> ListOutputDevices()
    {
        return _audio.ListOutputDevices();
    }

    public bool SelectOutputDevice(string? deviceId)
    {
        var ok = _audio.SelectDevice(deviceId);
        RaiseChanged();
        return ok;
    }

    public void SelectPreferredOutputDevice()
    {
        _audio.SelectPreferredDevice();
        RaiseChanged();
    }

    public void SetJitterTargetMs(int value)
    {
        _audio.SetJitterTargetMs(value);
    }

    public void SetMute(bool value)
    {
        _mute = value;
        _audio.SetMuted(value);
        Log($"Mute set to {value}");
        RaiseChanged();
    }

    public bool ToggleActions()
    {
        var enabled = _actions.ToggleEnabled();
        RaiseChanged();
        return enabled;
    }

    public void SetActionsEnabled(bool enabled)
    {
        _actions.SetEnabled(enabled);
        RaiseChanged();
    }

    public bool StartServer(string host = "0.0.0.0", int port = 8765)
    {
        lock (_sync)
        {
            if (_serverRunning)
            {
                return true;
            }

            try
            {
                _server = new WebSocketServer($"ws://{host}:{port}");
                _server.RestartAfterListenError = true;
                _server.Start(socket =>
                {
                    socket.OnOpen = () => HandleClientOpen(socket);
                    socket.OnClose = () => HandleClientClose(socket);
                    socket.OnBinary = data => HandleClientBinary(socket, data);
                    socket.OnMessage = text => HandleClientMessage(socket, text);
                    socket.OnError = ex => Log($"Socket error: {ex.Message}");
                });

                _serverRunning = true;
                var connectableUrls = ResolveConnectableServerUrls(port);
                _serverAddress = connectableUrls.Count > 0
                    ? string.Join(" | ", connectableUrls)
                    : $"ws://127.0.0.1:{port}";

                Log($"WebSocket server started (bind ws://{host}:{port})");
                Log($"Connect using: {_serverAddress}");
                RaiseChanged();
                return true;
            }
            catch (Exception ex)
            {
                Log($"Failed to start server: {ex.Message}");
                _server?.Dispose();
                _server = null;
                _serverRunning = false;
                _serverAddress = "-";
                RaiseChanged();
                return false;
            }
        }
    }

    public void StopServer()
    {
        lock (_sync)
        {
            if (!_serverRunning)
            {
                return;
            }

            try
            {
                foreach (var socket in _clients.Keys.ToList())
                {
                    try
                    {
                        socket.Close();
                    }
                    catch
                    {
                        // ignore close race
                    }
                }
            }
            catch
            {
                // ignore close race
            }

            _clients.Clear();
            _nonOwnerAudioWarned.Clear();
            _audioClient = null;
            RefreshConnectionStateLocked();

            _server?.Dispose();
            _server = null;
            _serverRunning = false;
            _serverAddress = "-";
            _audio.Clear();

            Log("WebSocket server stopped");
            RaiseChanged();
        }
    }

    public ReceiverSnapshot GetSnapshot()
    {
        var stats = _audio.GetStats();
        return new ReceiverSnapshot(
            ServerRunning: _serverRunning,
            ServerAddress: _serverAddress,
            Connected: _connected,
            ClientIp: _clientIp,
            RttMs: _rttMs,
            JitterMs: stats.JitterMs,
            BufferedMs: stats.BufferedMs,
            Mute: _mute,
            ActionsEnabled: _actions.Enabled,
            Codec: stats.CodecName,
            SelectedDevice: _audio.SelectedDeviceName ?? "System default",
            DriverInstalled: _driverInstalled);
    }

    public IReadOnlyList<string> GetRecentLogs(int limit = 50)
    {
        lock (_sync)
        {
            return _logs.Take(limit).ToList();
        }
    }

    private void HandleClientOpen(IWebSocketConnection socket)
    {
        lock (_sync)
        {
            if (_clients.ContainsKey(socket))
            {
                return;
            }

            var ip = socket.ConnectionInfo.ClientIpAddress?.ToString() ?? "unknown";
            var session = new ClientSession(socket, ip);
            _clients[socket] = session;
            RefreshConnectionStateLocked();

            Log($"Client connected: {DescribeClient(session)} (total={_clients.Count})");
            RaiseChanged();
        }
    }

    private void HandleClientClose(IWebSocketConnection socket)
    {
        lock (_sync)
        {
            if (!_clients.Remove(socket, out var session))
            {
                return;
            }

            _nonOwnerAudioWarned.Remove(socket);
            if (ReferenceEquals(_audioClient, socket))
            {
                _audioClient = null;
                _audio.Clear();
                Log("Audio source disconnected");
            }

            RefreshConnectionStateLocked();
            Log($"Client disconnected: {DescribeClient(session)} (total={_clients.Count})");
            RaiseChanged();
        }
    }

    private void HandleClientBinary(IWebSocketConnection socket, byte[] data)
    {
        bool acceptAudio;
        ClientSession? session;
        lock (_sync)
        {
            if (!_clients.TryGetValue(socket, out session))
            {
                return;
            }

            if (_audioClient is null)
            {
                _audioClient = socket;
                _nonOwnerAudioWarned.Clear();
                Log($"Audio source: {DescribeClient(session)}");
            }

            acceptAudio = ReferenceEquals(_audioClient, socket);
            if (!acceptAudio && _nonOwnerAudioWarned.Add(socket))
            {
                Log($"Ignored audio from non-audio client: {DescribeClient(session)}");
            }
        }

        if (!acceptAudio)
        {
            return;
        }

        var packet = Protocol.ParseAudioFrame(data);
        if (packet is null)
        {
            return;
        }

        _audio.PushPacket(packet);
        RaiseChanged();
    }

    private void HandleClientMessage(IWebSocketConnection socket, string text)
    {
        lock (_sync)
        {
            if (!_clients.ContainsKey(socket))
            {
                return;
            }
        }

        JsonDocument document;
        try
        {
            document = JsonDocument.Parse(text);
        }
        catch
        {
            return;
        }

        using (document)
        {
            if (document.RootElement.ValueKind != JsonValueKind.Object)
            {
                return;
            }

            var root = document.RootElement;
            var type = root.TryGetProperty("type", out var typeElement)
                ? typeElement.GetString()?.Trim().ToLowerInvariant() ?? string.Empty
                : string.Empty;

            switch (type)
            {
                case "ping":
                    HandlePing(socket, root);
                    break;
                case "action":
                    HandleAction(socket, root);
                    break;
                case "control":
                    HandleControl(socket, root);
                    break;
                case "hello":
                    HandleHello(socket, root);
                    break;
            }
        }
    }

    private void HandleHello(IWebSocketConnection socket, JsonElement root)
    {
        ClientSession? session;
        lock (_sync)
        {
            if (!_clients.TryGetValue(socket, out session))
            {
                return;
            }

            if (root.TryGetProperty("role", out var roleElement) && roleElement.ValueKind == JsonValueKind.String)
            {
                var role = roleElement.GetString()?.Trim().ToLowerInvariant();
                if (!string.IsNullOrWhiteSpace(role))
                {
                    session.Role = role;
                }
            }

            if (root.TryGetProperty("device_id", out var deviceElement) && deviceElement.ValueKind == JsonValueKind.String)
            {
                var deviceId = deviceElement.GetString()?.Trim() ?? string.Empty;
                if (!string.IsNullOrEmpty(deviceId))
                {
                    session.DeviceId = deviceId.Length <= 40 ? deviceId : deviceId[..40];
                }
            }

            RefreshConnectionStateLocked();
            Log($"Client hello: {DescribeClient(session)}");
        }

        SendJson(socket, new Dictionary<string, object?>
        {
            ["type"] = "event",
            ["level"] = "info",
            ["message"] = "hello_ok"
        });
    }

    private void HandlePing(IWebSocketConnection socket, JsonElement root)
    {
        long ts = 0;
        if (root.TryGetProperty("ts", out var tsElement))
        {
            ts = tsElement.ValueKind switch
            {
                JsonValueKind.Number when tsElement.TryGetInt64(out var value) => value,
                _ => 0
            };
        }

        SendJson(socket, new Dictionary<string, object?>
        {
            ["type"] = "pong",
            ["ts"] = ts
        });
    }

    private void HandleAction(IWebSocketConnection socket, JsonElement root)
    {
        var result = _actions.ExecuteAction(root);
        SendJson(socket, new Dictionary<string, object?>
        {
            ["type"] = "event",
            ["level"] = result.Ok ? "info" : "error",
            ["message"] = result.Message
        });
    }

    private void HandleControl(IWebSocketConnection socket, JsonElement root)
    {
        var op = root.TryGetProperty("op", out var opElement)
            ? opElement.GetString() ?? string.Empty
            : string.Empty;

        if (!op.Equals("set_mute", StringComparison.OrdinalIgnoreCase))
        {
            return;
        }

        var nextMute = root.TryGetProperty("value", out var valueElement) &&
                       valueElement.ValueKind is JsonValueKind.True or JsonValueKind.False &&
                       valueElement.GetBoolean();

        SetMute(nextMute);
        SendJson(socket, new Dictionary<string, object?>
        {
            ["type"] = "event",
            ["level"] = "info",
            ["message"] = $"mute={nextMute}"
        });
    }

    private void PushStateToClients()
    {
        List<IWebSocketConnection> sockets;
        ReceiverSnapshot snapshot;

        lock (_sync)
        {
            sockets = _clients.Keys.ToList();
            snapshot = GetSnapshot();
        }

        if (sockets.Count == 0 || !snapshot.Connected)
        {
            return;
        }

        var payload = new Dictionary<string, object?>
        {
            ["type"] = "state",
            ["connected"] = snapshot.Connected,
            ["mute"] = snapshot.Mute,
            ["rtt_ms"] = snapshot.RttMs,
            ["jitter_ms"] = snapshot.JitterMs,
            ["active_window"] = ActionExecutor.GetActiveWindowTitle(),
            ["audio_codec"] = snapshot.Codec,
            ["actions_enabled"] = snapshot.ActionsEnabled,
            ["client_count"] = sockets.Count
        };

        foreach (var socket in sockets)
        {
            SendJson(socket, payload);
        }
    }

    private void RefreshConnectionStateLocked()
    {
        _connected = _clients.Count > 0;
        _clientIp = _connected ? BuildClientSummaryLocked() : string.Empty;
    }

    private string BuildClientSummaryLocked()
    {
        if (_clients.Count == 0)
        {
            return string.Empty;
        }

        var sessions = _clients.Values
            .OrderBy(s => s.ConnectedAtUtc)
            .ToList();

        if (sessions.Count == 1)
        {
            return DescribeClient(sessions[0]);
        }

        var sample = string.Join(", ", sessions.Take(2).Select(s => DescribeClient(s, includeDevice: false)));
        if (sessions.Count > 2)
        {
            sample = $"{sample}, +{sessions.Count - 2}";
        }

        return $"{sessions.Count} clients ({sample})";
    }

    private static string DescribeClient(ClientSession session, bool includeDevice = true)
    {
        var roleText = session.Role.Equals("legacy", StringComparison.OrdinalIgnoreCase)
            ? string.Empty
            : $"[{session.Role}]";

        var deviceText = includeDevice && !string.IsNullOrWhiteSpace(session.DeviceId)
            ? $"#{session.DeviceId}"
            : string.Empty;

        return $"{session.Ip}{roleText}{deviceText}";
    }

    private void SendJson(IWebSocketConnection socket, object payload)
    {
        try
        {
            var text = JsonSerializer.Serialize(payload, _jsonOptions);
            socket.Send(text);
        }
        catch (Exception ex)
        {
            Log($"Send failed: {ex.Message}");
        }
    }

    private void Log(string message)
    {
        var line = $"[{DateTime.Now:HH:mm:ss}] {message}";

        lock (_sync)
        {
            _logs.Insert(0, line);
            if (_logs.Count > 200)
            {
                _logs.RemoveAt(_logs.Count - 1);
            }
        }

        LogAdded?.Invoke(line);
        RaiseChanged();
    }

    private void RaiseChanged()
    {
        StateChanged?.Invoke();
    }

    private static IReadOnlyList<string> ResolveConnectableServerUrls(int port)
    {
        var adapters = NetworkInterface.GetAllNetworkInterfaces()
            .Where(iface => iface.OperationalStatus == OperationalStatus.Up)
            .Where(iface => iface.NetworkInterfaceType is NetworkInterfaceType.Ethernet or NetworkInterfaceType.Wireless80211)
            .Where(iface => !IsExcludedAdapter(iface))
            .Select(iface => new
            {
                Interface = iface,
                HasGateway = iface.GetIPProperties().GatewayAddresses
                    .Any(g => g.Address.AddressFamily == AddressFamily.InterNetwork && !IPAddress.Any.Equals(g.Address)),
                Priority = iface.NetworkInterfaceType == NetworkInterfaceType.Ethernet ? 0 : 1
            })
            .OrderByDescending(x => x.HasGateway)
            .ThenBy(x => x.Priority)
            .ThenBy(x => x.Interface.Name, StringComparer.OrdinalIgnoreCase)
            .ToList();

        var urls = new List<string>();
        var seenIps = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        foreach (var adapter in adapters)
        {
            foreach (var unicast in adapter.Interface.GetIPProperties().UnicastAddresses)
            {
                var ip = unicast.Address;
                if (ip.AddressFamily != AddressFamily.InterNetwork)
                {
                    continue;
                }

                if (IPAddress.IsLoopback(ip))
                {
                    continue;
                }

                var ipText = ip.ToString();
                if (ipText.StartsWith("169.254.", StringComparison.Ordinal))
                {
                    continue;
                }

                if (!seenIps.Add(ipText))
                {
                    continue;
                }

                urls.Add($"ws://{ipText}:{port}");
            }
        }

        return urls;
    }

    private static bool IsExcludedAdapter(NetworkInterface adapter)
    {
        var text = $"{adapter.Name} {adapter.Description}".ToLowerInvariant();
        var blockedKeywords = new[]
        {
            "zerotier",
            "virtual",
            "vmware",
            "hyper-v",
            "vethernet",
            "veth",
            "vbox",
            "docker",
            "loopback",
            "tap",
            "tun",
            "tailscale",
            "wireguard",
            "npcap"
        };

        return blockedKeywords.Any(text.Contains);
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        _stateTimer.Dispose();
        StopServer();
        _audio.Dispose();
    }
}
