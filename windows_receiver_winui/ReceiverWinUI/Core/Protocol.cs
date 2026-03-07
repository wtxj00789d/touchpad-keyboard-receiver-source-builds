using System.Buffers.Binary;

namespace ReceiverWinUI.Core;

public sealed record AudioPacket(byte Codec, byte Flags, uint Seq, ulong TimestampMs, byte[] Payload)
{
    public bool Muted => (Flags & 0x01) != 0;
}

public static class Protocol
{
    public static AudioPacket? ParseAudioFrame(byte[] data)
    {
        if (data.Length < 14)
        {
            return null;
        }

        var codec = data[0];
        var flags = data[1];
        var seq = BinaryPrimitives.ReadUInt32LittleEndian(data.AsSpan(2, 4));
        var ts = BinaryPrimitives.ReadUInt64LittleEndian(data.AsSpan(6, 8));
        var payload = data.AsSpan(14).ToArray();
        return new AudioPacket(codec, flags, seq, ts, payload);
    }
}
