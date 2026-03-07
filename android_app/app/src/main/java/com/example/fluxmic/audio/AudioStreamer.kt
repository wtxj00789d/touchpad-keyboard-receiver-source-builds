package com.example.fluxmic.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import kotlin.concurrent.thread
import kotlin.math.abs

class AudioStreamer(
    private val onAudioFrame: (ByteArray) -> Unit,
    private val onLevel: (Float) -> Unit,
    private val onError: (String) -> Unit
) {
    @Volatile
    private var running = false

    @Volatile
    private var muted = false

    private var worker: Thread? = null
    private var seq: Long = 0L

    private val sampleRate = 48_000
    private val frameSamples = 960 // 20ms @ 48k

    fun setMuted(value: Boolean) {
        muted = value
    }

    fun start() {
        if (running) return
        running = true
        worker = thread(start = true, name = "AudioStreamer") {
            val audioRecord = createAudioRecord()
            if (audioRecord == null) {
                onError("Failed to init AudioRecord")
                running = false
                return@thread
            }

            val shortBuffer = ShortArray(frameSamples)
            audioRecord.startRecording()

            while (running) {
                val read = audioRecord.read(shortBuffer, 0, shortBuffer.size)
                if (read <= 0) continue

                val level = computeLevel(shortBuffer, read)
                onLevel(level)

                val payload = if (muted) {
                    ByteArray(read * 2)
                } else {
                    shortToBytes(shortBuffer, read)
                }

                val frame = packAudioFrame(
                    codec = 0,
                    flags = if (muted) 0x01 else 0x00,
                    seq = (seq++ and 0xFFFFFFFFL).toInt(),
                    timestampMs = SystemClock.elapsedRealtimeNanos() / 1_000_000,
                    payload = payload
                )
                onAudioFrame(frame)
            }

            runCatching { audioRecord.stop() }
            runCatching { audioRecord.release() }
        }
    }

    fun stop() {
        running = false
        worker?.join(300)
        worker = null
    }

    private fun createAudioRecord(): AudioRecord? {
        val minSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minSize, frameSamples * 4)

        val candidates = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )

        for (source in candidates) {
            val record = runCatching {
                AudioRecord(
                    source,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }.getOrNull() ?: continue

            if (record.state == AudioRecord.STATE_INITIALIZED) {
                return record
            }
            runCatching { record.release() }
        }
        return null
    }

    private fun computeLevel(data: ShortArray, len: Int): Float {
        var peak = 0
        for (i in 0 until len) {
            peak = maxOf(peak, abs(data[i].toInt()))
        }
        return peak / 32767f
    }

    private fun shortToBytes(data: ShortArray, len: Int): ByteArray {
        val bytes = ByteArray(len * 2)
        var bi = 0
        for (i in 0 until len) {
            val s = data[i].toInt()
            bytes[bi++] = (s and 0xFF).toByte()
            bytes[bi++] = ((s shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun packAudioFrame(
        codec: Int,
        flags: Int,
        seq: Int,
        timestampMs: Long,
        payload: ByteArray
    ): ByteArray {
        val out = ByteArray(14 + payload.size)
        out[0] = codec.toByte()
        out[1] = flags.toByte()

        out[2] = (seq and 0xFF).toByte()
        out[3] = ((seq ushr 8) and 0xFF).toByte()
        out[4] = ((seq ushr 16) and 0xFF).toByte()
        out[5] = ((seq ushr 24) and 0xFF).toByte()

        for (i in 0 until 8) {
            out[6 + i] = ((timestampMs ushr (i * 8)) and 0xFF).toByte()
        }

        payload.copyInto(out, destinationOffset = 14)
        return out
    }
}
