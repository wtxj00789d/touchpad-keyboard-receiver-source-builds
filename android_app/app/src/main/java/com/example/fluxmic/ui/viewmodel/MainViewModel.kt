package com.example.fluxmic.ui.viewmodel

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fluxmic.audio.AudioStreamer
import com.example.fluxmic.model.ActionMessage
import com.example.fluxmic.model.ControlMessage
import com.example.fluxmic.model.KeyConfig
import com.example.fluxmic.model.LayoutConfig
import com.example.fluxmic.model.LayoutRepository
import com.example.fluxmic.model.PingMessage
import com.example.fluxmic.model.StateMessage
import com.example.fluxmic.model.UiState
import com.example.fluxmic.net.PanelWebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.atomic.AtomicLong

class MainViewModel(private val appContext: Context) : ViewModel(), PanelWebSocketClient.Listener {
    companion object {
        private const val PREFS_NAME = "fluxmic_prefs"
        private const val PREF_SERVER_WS_URL = "server_ws_url"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val layoutRepo = LayoutRepository(appContext)
    private val wsClient = PanelWebSocketClient()
    private val sequence = AtomicLong(1)
    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentLayout: LayoutConfig? = null
    private var pingJob: Job? = null
    private var micPermissionGranted = false
    private val activePressedKeys = LinkedHashSet<String>()
    private var localCapsLockOn = false

    private val audioStreamer = AudioStreamer(
        onAudioFrame = { frame ->
            if (_uiState.value.connected) {
                wsClient.sendBinary(frame)
            }
        },
        onLevel = { level ->
            _uiState.value = _uiState.value.copy(micLevel = level)
        },
        onError = { reason ->
            _uiState.value = _uiState.value.copy(lastEvent = "Audio: $reason")
        }
    )

    init {
        wsClient.setListener(this)
        val savedUrl = prefs.getString(PREF_SERVER_WS_URL, null)
        if (!savedUrl.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(serverUrl = savedUrl)
        }
        loadDefaultLayout()
    }

    fun setMicPermission(granted: Boolean) {
        micPermissionGranted = granted
        if (!granted) {
            audioStreamer.stop()
            _uiState.value = _uiState.value.copy(lastEvent = "Microphone permission denied")
        } else if (_uiState.value.connected) {
            audioStreamer.start()
        }
    }

    fun setServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
        prefs.edit().putString(PREF_SERVER_WS_URL, url).apply()
    }

    fun connect() {
        val url = _uiState.value.serverUrl.trim()
        if (url.isEmpty()) return
        _uiState.value = _uiState.value.copy(connecting = true, statusText = "Connecting...")
        wsClient.connect(url)
    }

    fun disconnect() {
        wsClient.disconnect()
        pingJob?.cancel()
        pingJob = null
        audioStreamer.stop()
        activePressedKeys.clear()
        localCapsLockOn = false
        _uiState.value = _uiState.value.copy(
            connected = false,
            connecting = false,
            statusText = "Disconnected",
            stateFlags = _uiState.value.stateFlags.toMutableMap().apply { this["caps_lock"] = false }
        )
    }

    fun toggleMute() {
        val next = !_uiState.value.mute
        applyMute(next)
        sendControl(op = "set_mute", value = JsonPrimitive(next))
    }

    fun selectPage(index: Int) {
        if (index !in _uiState.value.pages.indices) return
        _uiState.value = _uiState.value.copy(selectedPageIndex = index)
    }

    fun switchLayoutByName(name: String): Boolean {
        val loaded = layoutRepo.loadByLayoutName(name)
        if (loaded == null) {
            _uiState.value = _uiState.value.copy(lastEvent = "Layout not found: $name")
            return false
        }
        applyLayout(loaded)
        return true
    }

    fun onKeyTriggered(key: KeyConfig, longPress: Boolean) {
        val spec = if (longPress) key.holdAction ?: key.action else key.action
        if (spec.kind.name == "CMD") {
            val cmd = jsonElementToString(spec.payload)
            if (cmd.startsWith("switch_layout:", ignoreCase = true)) {
                val target = cmd.substringAfter(':').trim()
                switchLayoutByName(target)
                return
            }
        }

        if (spec.kind.name == "TOGGLE") {
            val payloadObj = spec.payload as? JsonObject
            val op = payloadObj?.get("op")?.jsonPrimitive?.contentOrNull
            val value = payloadObj?.get("value")
            if (op != null) {
                if (op == "set_mute") {
                    applyMute(value?.jsonPrimitive?.booleanOrNull ?: false)
                }
                sendControl(op, value)
                return
            }
        }

        sendAction(
            actionId = key.id,
            kind = spec.kind.name,
            payload = spec.payload
        )
    }

    fun onKeyPressed(key: KeyConfig) {
        val spec = key.action
        if (spec.kind.name != "KEY") {
            onKeyTriggered(key, longPress = false)
            return
        }

        val token = jsonElementToString(spec.payload).trim().uppercase()
        if (token.isBlank()) return
        if (activePressedKeys.contains(token)) return
        if (activePressedKeys.size >= 3) return

        activePressedKeys.add(token)
        if (token == "CAPSLOCK") {
            localCapsLockOn = !localCapsLockOn
            _uiState.value = _uiState.value.copy(
                stateFlags = _uiState.value.stateFlags.toMutableMap().apply {
                    this["caps_lock"] = localCapsLockOn
                }
            )
        }
        if (isModifierKey(token) && activePressedKeys.none { !isModifierKey(it) }) {
            return
        }

        val modifiers = activePressedKeys.filter { isModifierKey(it) }
        val normal = activePressedKeys.filter { !isModifierKey(it) }
        val combo = (modifiers + normal).joinToString("+")
        sendAction(
            actionId = key.id,
            kind = "KEY",
            payload = JsonPrimitive(combo)
        )
    }

    fun onKeyRepeated(key: KeyConfig) {
        val spec = key.action
        if (spec.kind.name != "KEY") return

        val token = jsonElementToString(spec.payload).trim().uppercase()
        if (token.isBlank() || isModifierKey(token) || token == "CAPSLOCK") return
        if (!activePressedKeys.contains(token)) return

        val modifiers = activePressedKeys.filter { isModifierKey(it) }
        val combo = (modifiers + token).joinToString("+")
        sendAction(
            actionId = key.id,
            kind = "KEY",
            payload = JsonPrimitive(combo)
        )
    }

    fun onKeyReleased(key: KeyConfig) {
        val token = jsonElementToString(key.action.payload).trim().uppercase()
        if (token.isBlank()) return
        activePressedKeys.remove(token)
    }

    override fun onConnected() {
        _uiState.value = _uiState.value.copy(
            connected = true,
            connecting = false,
            statusText = "Connected"
        )

        if (micPermissionGranted) {
            audioStreamer.start()
        }

        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            while (true) {
                val ts = SystemClock.elapsedRealtime()
                wsClient.sendText(json.encodeToString(PingMessage(ts = ts)))
                delay(2_000)
            }
        }
    }

    override fun onDisconnected(reason: String) {
        pingJob?.cancel()
        pingJob = null
        audioStreamer.stop()
        activePressedKeys.clear()
        _uiState.value = _uiState.value.copy(
            connected = false,
            connecting = false,
            statusText = "Disconnected",
            lastEvent = reason
        )
    }

    override fun onError(reason: String) {
        _uiState.value = _uiState.value.copy(
            connecting = false,
            statusText = "Error",
            lastEvent = reason
        )
    }

    override fun onState(state: StateMessage) {
        val flags = mutableMapOf<String, Boolean>()
        flags["mute"] = state.mute
        state.extra?.forEach { (k, v) ->
            val boolValue = runCatching { v.jsonPrimitive.booleanOrNull }.getOrNull()
            if (boolValue != null) flags[k] = boolValue
        }

        val capsFromServer = flags["caps_lock"] ?: flags["caps"]
        if (capsFromServer != null) {
            localCapsLockOn = capsFromServer
        }
        flags["caps_lock"] = localCapsLockOn

        _uiState.value = _uiState.value.copy(
            connected = state.connected || _uiState.value.connected,
            mute = state.mute,
            rttMs = state.rtt_ms ?: _uiState.value.rttMs,
            jitterMs = state.jitter_ms ?: _uiState.value.jitterMs,
            activeWindow = state.active_window ?: "",
            stateFlags = flags
        )
    }

    override fun onPong(sentTs: Long) {
        val rtt = (SystemClock.elapsedRealtime() - sentTs).toInt()
        _uiState.value = _uiState.value.copy(rttMs = rtt)
    }

    override fun onEvent(message: String) {
        _uiState.value = _uiState.value.copy(lastEvent = message)
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
        wsClient.setListener(null)
    }

    private fun loadDefaultLayout() {
        runCatching {
            applyLayout(layoutRepo.loadDefault())
        }.onFailure {
            _uiState.value = _uiState.value.copy(lastEvent = "Layout load failed: ${it.message}")
        }
    }

    private fun applyLayout(layout: LayoutConfig) {
        currentLayout = layout
        _uiState.value = _uiState.value.copy(
            layoutName = layout.name,
            pages = layout.pages,
            selectedPageIndex = 0,
            lastEvent = "Layout: ${layout.name}"
        )
    }

    private fun sendAction(actionId: String?, kind: String, payload: JsonElement?) {
        val msg = ActionMessage(
            action_id = actionId,
            kind = kind,
            payload = payload,
            seq = sequence.getAndIncrement(),
            ts = System.currentTimeMillis()
        )
        wsClient.sendText(json.encodeToString(msg))
    }

    private fun sendControl(op: String, value: JsonElement?) {
        val msg = ControlMessage(
            op = op,
            value = value,
            seq = sequence.getAndIncrement(),
            ts = System.currentTimeMillis()
        )
        wsClient.sendText(json.encodeToString(msg))
    }

    private fun applyMute(value: Boolean) {
        audioStreamer.setMuted(value)
        _uiState.value = _uiState.value.copy(mute = value)
    }

    private fun jsonElementToString(payload: JsonElement?): String {
        return when {
            payload == null -> ""
            payload is JsonPrimitive && payload.isString -> payload.content
            else -> payload.toString()
        }
    }

    private fun isModifierKey(token: String): Boolean {
        return token in setOf("CTRL", "SHIFT", "ALT", "WIN", "LCTRL", "RCTRL", "LSHIFT", "RSHIFT", "LALT", "RALT", "LWIN", "RWIN")
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(context.applicationContext) as T
        }
    }
}
