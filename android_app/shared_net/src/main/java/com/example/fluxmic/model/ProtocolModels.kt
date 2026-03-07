package com.example.fluxmic.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class StateMessage(
    val type: String = "state",
    val connected: Boolean = false,
    val mute: Boolean = false,
    val rtt_ms: Int? = null,
    val jitter_ms: Int? = null,
    val active_window: String? = null,
    val audio_codec: String? = null,
    val extra: JsonObject? = null
)

@Serializable
data class ActionMessage(
    val type: String = "action",
    val action_id: String? = null,
    val kind: String,
    val payload: JsonElement? = null,
    val seq: Long,
    val ts: Long
)

@Serializable
data class ControlMessage(
    val type: String = "control",
    val op: String,
    val value: JsonElement? = null,
    val seq: Long,
    val ts: Long
)

@Serializable
data class PingMessage(
    val type: String = "ping",
    val ts: Long
)

@Serializable
data class HelloMessage(
    val type: String = "hello",
    val role: String,
    val device_id: String? = null,
    val app_version: String? = null,
    val seq: Long? = null,
    val ts: Long? = null
)

data class AudioConfig(
    val sampleRate: Int = 48_000,
    val channels: Int = 1,
    val frameMs: Int = 20,
    val codec: Int = 0,
    val bitrateKbps: Int = 24
)
