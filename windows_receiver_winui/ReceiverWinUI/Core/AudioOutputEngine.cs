using NAudio.CoreAudioApi;
using NAudio.Wave;

namespace ReceiverWinUI.Core;

public sealed record AudioDeviceInfo(string Id, string Name);

public sealed record AudioStats(int JitterMs, int BufferedMs, int DroppedFrames, string CodecName);

public sealed class AudioOutputEngine : IDisposable
{
    private const int SampleRate = 48000;
    private const int Channels = 1;
    private const int BytesPerFrame20Ms = 1920;
    private const int MinJitterMs = 20;
    private const int MaxJitterMs = 200;

    private readonly object _sync = new();
    private readonly MMDeviceEnumerator _enumerator = new();

    private WasapiOut? _output;
    private BufferedWaveProvider? _buffer;
    private MMDevice? _selectedDevice;

    private double _jitterEstimate;
    private double? _lastArrivalMs;
    private ulong? _lastSenderTs;
    private int _droppedFrames;
    private string _codecName = "PCM16";
    private bool _muted;
    private int _targetJitterMs = 60;

    public event Action<string>? Log;

    public IReadOnlyList<AudioDeviceInfo> ListOutputDevices()
    {
        var devices = _enumerator.EnumerateAudioEndPoints(DataFlow.Render, DeviceState.Active);
        return devices.Select(d => new AudioDeviceInfo(d.ID, d.FriendlyName)).ToList();
    }

    public string? SelectedDeviceName => _selectedDevice?.FriendlyName;
    public string? SelectedDeviceId => _selectedDevice?.ID;

    public bool SelectDevice(string? deviceId)
    {
        lock (_sync)
        {
            _selectedDevice?.Dispose();
            _selectedDevice = null;
            if (string.IsNullOrWhiteSpace(deviceId))
            {
                _selectedDevice = _enumerator.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia);
            }
            else
            {
                try
                {
                    _selectedDevice = _enumerator.GetDevice(deviceId);
                }
                catch
                {
                    return false;
                }
            }

            ReopenOutput();
            return _selectedDevice is not null;
        }
    }

    public void SelectPreferredDevice()
    {
        var devices = ListOutputDevices();
        var cable = devices.FirstOrDefault(d => d.Name.Contains("CABLE Input", StringComparison.OrdinalIgnoreCase));
        SelectDevice(cable?.Id);
    }

    public void SetMuted(bool value)
    {
        _muted = value;
    }

    public void SetJitterTargetMs(int value)
    {
        _targetJitterMs = Math.Clamp(value, MinJitterMs, MaxJitterMs);
    }

    public void Clear()
    {
        lock (_sync)
        {
            _buffer?.ClearBuffer();
        }
    }

    public void PushPacket(AudioPacket packet)
    {
        _codecName = packet.Codec == 0 ? "PCM16" : "OPUS";
        UpdateJitter(packet.TimestampMs);

        if (packet.Codec != 0)
        {
            return;
        }

        var frame = packet.Payload;
        if (frame.Length < BytesPerFrame20Ms)
        {
            var tmp = new byte[BytesPerFrame20Ms];
            Buffer.BlockCopy(frame, 0, tmp, 0, frame.Length);
            frame = tmp;
        }
        else if (frame.Length > BytesPerFrame20Ms)
        {
            var tmp = new byte[BytesPerFrame20Ms];
            Buffer.BlockCopy(frame, 0, tmp, 0, BytesPerFrame20Ms);
            frame = tmp;
        }

        if (_muted || packet.Muted)
        {
            frame = new byte[BytesPerFrame20Ms];
        }

        lock (_sync)
        {
            if (_buffer is null)
            {
                return;
            }

            var maxBufferedMs = _targetJitterMs + 140;
            if (_buffer.BufferedDuration.TotalMilliseconds > maxBufferedMs)
            {
                _droppedFrames++;
                return;
            }

            _buffer.AddSamples(frame, 0, frame.Length);
        }
    }

    public AudioStats GetStats()
    {
        lock (_sync)
        {
            var bufferedMs = _buffer is null ? 0 : (int)_buffer.BufferedDuration.TotalMilliseconds;
            return new AudioStats((int)_jitterEstimate, bufferedMs, _droppedFrames, _codecName);
        }
    }

    private void ReopenOutput()
    {
        _output?.Stop();
        _output?.Dispose();
        _buffer = null;

        if (_selectedDevice is null)
        {
            return;
        }

        _buffer = new BufferedWaveProvider(new WaveFormat(SampleRate, 16, Channels))
        {
            DiscardOnBufferOverflow = true,
            BufferDuration = TimeSpan.FromMilliseconds(1000)
        };

        _output = new WasapiOut(_selectedDevice, AudioClientShareMode.Shared, false, 20);
        _output.Init(_buffer);
        _output.Play();
        Log?.Invoke($"Audio output: {_selectedDevice.FriendlyName}");
    }

    private void UpdateJitter(ulong senderTsMs)
    {
        var arrival = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        if (_lastArrivalMs is null || _lastSenderTs is null)
        {
            _lastArrivalMs = arrival;
            _lastSenderTs = senderTsMs;
            return;
        }

        var transit = arrival - (double)senderTsMs;
        var prevTransit = _lastArrivalMs.Value - _lastSenderTs.Value;
        var d = Math.Abs(transit - prevTransit);
        _jitterEstimate += (d - _jitterEstimate) / 16.0;

        _lastArrivalMs = arrival;
        _lastSenderTs = senderTsMs;
    }

    public void Dispose()
    {
        lock (_sync)
        {
            _output?.Stop();
            _output?.Dispose();
            _output = null;
            _buffer = null;
            _selectedDevice?.Dispose();
            _selectedDevice = null;
        }

        _enumerator.Dispose();
    }
}
