package com.example.fluxmic.ui.viewmodel

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fluxmic.audio.AudioStreamer
import com.example.fluxmic.model.ActionMessage
import com.example.fluxmic.model.ControlMessage
import com.example.fluxmic.model.ExternalConnectCommand
import com.example.fluxmic.model.KeyConfig
import com.example.fluxmic.model.KeyboardBehavior
import com.example.fluxmic.model.KeyboardLayerState
import com.example.fluxmic.model.KeyboardLayoutMode
import com.example.fluxmic.model.LayoutConfig
import com.example.fluxmic.model.LayoutRepository
import com.example.fluxmic.model.PingMessage
import com.example.fluxmic.model.ServerAddressEditor
import com.example.fluxmic.model.ServerAddressFormatter
import com.example.fluxmic.model.StateMessage
import com.example.fluxmic.model.UiState
import com.example.fluxmic.model.WindowControlState
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
        private const val PREF_KEYBOARD_LAYOUT_MODE = "keyboard_layout_mode"
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
    private var keyboardLayerState = KeyboardLayerState()

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
        val savedLayoutMode = KeyboardLayoutMode.fromStoredValue(
            prefs.getString(PREF_KEYBOARD_LAYOUT_MODE, null)
        )
        val normalizedUrl = ServerAddressFormatter
            .normalizeForConnection(savedUrl ?: _uiState.value.serverUrl)
            .ifBlank { _uiState.value.serverUrl }
        _uiState.value = _uiState.value.copy(
            keyboardLayoutMode = savedLayoutMode,
            serverUrl = normalizedUrl,
            serverAddressDraft = ServerAddressFormatter.toDisplayValue(normalizedUrl)
        )
        loadDefaultLayout(savedLayoutMode)
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
        val normalizedUrl = ServerAddressFormatter.normalizeForConnection(url)
        if (normalizedUrl.isBlank()) return
        _uiState.value = _uiState.value.copy(
            serverUrl = normalizedUrl,
            serverAddressDraft = ServerAddressFormatter.toDisplayValue(normalizedUrl)
        )
        prefs.edit().putString(PREF_SERVER_WS_URL, normalizedUrl).apply()
    }

    fun handleExternalConnectCommand(command: ExternalConnectCommand) {
        val normalizedUrl = ServerAddressFormatter.normalizeForConnection(command.targetUrl)
        if (normalizedUrl.isBlank()) return
        val displayUrl = ServerAddressFormatter.toDisplayValue(normalizedUrl)
        _uiState.value = _uiState.value.copy(
            serverUrl = normalizedUrl,
            serverAddressDraft = displayUrl,
            addressEditorExpanded = false
        )
        prefs.edit().putString(PREF_SERVER_WS_URL, normalizedUrl).apply()

        if (_uiState.value.connected || _uiState.value.connecting) {
            disconnect()
        }

        if (command.autoConnect) {
            connect()
        }
    }

    fun toggleAddressEditor() {
        val nextExpanded = !_uiState.value.addressEditorExpanded
        val displayValue = ServerAddressFormatter.toDisplayValue(_uiState.value.serverUrl)
        _uiState.value = _uiState.value.copy(
            addressEditorExpanded = nextExpanded,
            serverAddressDraft = if (nextExpanded) {
                if (_uiState.value.serverAddressDraft.isBlank()) displayValue else _uiState.value.serverAddressDraft
            } else {
                displayValue
            }
        )
    }

    fun setKeyboardLayoutMode(layoutMode: KeyboardLayoutMode) {
        if (layoutMode == _uiState.value.keyboardLayoutMode && currentLayout != null) return
        prefs.edit().putString(PREF_KEYBOARD_LAYOUT_MODE, layoutMode.storedValue).apply()
        loadDefaultLayout(layoutMode)
    }

    fun connect() {
        val draftSource = if (_uiState.value.addressEditorExpanded) {
            _uiState.value.serverAddressDraft
        } else {
            _uiState.value.serverUrl
        }
        val url = ServerAddressFormatter.normalizeForConnection(draftSource)
        if (url.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            serverUrl = url,
            serverAddressDraft = ServerAddressFormatter.toDisplayValue(url),
            addressEditorExpanded = false,
            connecting = true,
            statusText = "Connecting..."
        )
        prefs.edit().putString(PREF_SERVER_WS_URL, url).apply()
        wsClient.connect(url)
    }

    fun disconnect() {
        wsClient.disconnect()
        pingJob?.cancel()
        pingJob = null
        audioStreamer.stop()
        keyboardLayerState = KeyboardLayerState()
        _uiState.value = _uiState.value.copy(
            connected = false,
            connecting = false,
            statusText = "Disconnected",
            activeWindow = "",
            windowControls = WindowControlState(),
            keyboardLayerState = keyboardLayerState,
            stateFlags = _uiState.value.stateFlags.toMutableMap().apply {
                this["caps_lock"] = false
                this["fn_lock"] = false
            }
        )
    }

    fun toggleMute() {
        val next = !_uiState.value.mute
        applyMute(next)
        sendControl(op = "set_mute", value = JsonPrimitive(next))
    }

    fun setRemoteVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _uiState.value = _uiState.value.copy(systemVolume = clamped)
        sendControl(op = "set_volume", value = JsonPrimitive(clamped))
    }

    fun minimizeActiveWindow() {
        val windowControls = _uiState.value.windowControls
        if (!windowControls.available || !windowControls.canMinimize) return
        sendControl(op = "window_minimize", value = null)
    }

    fun toggleMaximizeActiveWindow() {
        val windowControls = _uiState.value.windowControls
        if (!windowControls.available || !windowControls.canMaximize) return
        sendControl(op = windowControls.maximizeControlOp(), value = null)
    }

    fun closeActiveWindow() {
        val windowControls = _uiState.value.windowControls
        if (!windowControls.available || !windowControls.canClose) return
        sendControl(op = "window_close", value = null)
    }

    fun selectPage(index: Int) {
        if (index !in _uiState.value.pages.indices) return
        _uiState.value = _uiState.value.copy(selectedPageIndex = index)
    }

    fun switchLayoutByName(name: String): Boolean {
        val loaded = if (name.equals("default", ignoreCase = true)) {
            layoutRepo.loadDefault(_uiState.value.keyboardLayoutMode)
        } else {
            layoutRepo.loadByLayoutName(name)
        }
        if (loaded == null) {
            _uiState.value = _uiState.value.copy(lastEvent = "Layout not found: $name")
            return false
        }
        applyLayout(loaded, _uiState.value.keyboardLayoutMode)
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
        if (KeyboardBehavior.isFnToken(token)) {
            updateKeyboardLayerState(KeyboardBehavior.toggleFn(keyboardLayerState))
            return
        }

        val downResult = KeyboardBehavior.onKeyDown(keyboardLayerState, key.id, token)
        if (!downResult.accepted) return

        val nextLayerState = if (isCapsLockToken(token)) {
            KeyboardBehavior.toggleCaps(downResult.state)
        } else {
            downResult.state
        }
        updateKeyboardLayerState(nextLayerState)

        if (_uiState.value.addressEditorExpanded) {
            applyAddressEditorToken(
                token = KeyboardBehavior.resolveOutputToken(token, key.id, nextLayerState),
                layerState = nextLayerState
            )
            return
        }

        if (isModifierKey(token) && nextLayerState.activeKeyTokens.none(::isNonModifierToken)) {
            return
        }

        val modifiers = nextLayerState.activeKeyTokens.filter { isModifierKey(it) }
        val outputToken = KeyboardBehavior.resolveOutputToken(token, key.id, nextLayerState)
        val combo = (modifiers + outputToken).joinToString("+")
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
        if (token.isBlank() || KeyboardBehavior.isFnToken(token) || isModifierKey(token) || isCapsLockToken(token)) return
        if (!keyboardLayerState.activeKeyTokens.contains(token)) return

        if (_uiState.value.addressEditorExpanded) {
            applyAddressEditorToken(
                token = KeyboardBehavior.resolveOutputToken(token, key.id, keyboardLayerState),
                layerState = keyboardLayerState
            )
            return
        }

        val modifiers = keyboardLayerState.activeKeyTokens.filter { isModifierKey(it) }
        val outputToken = KeyboardBehavior.resolveOutputToken(token, key.id, keyboardLayerState)
        val combo = (modifiers + outputToken).joinToString("+")
        sendAction(
            actionId = key.id,
            kind = "KEY",
            payload = JsonPrimitive(combo)
        )
    }

    fun onKeyReleased(key: KeyConfig) {
        val token = jsonElementToString(key.action.payload).trim().uppercase()
        if (token.isBlank()) return
        if (KeyboardBehavior.isFnToken(token)) return
        updateKeyboardLayerState(KeyboardBehavior.onKeyUp(keyboardLayerState, key.id, token))
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
        keyboardLayerState = KeyboardLayerState()
        _uiState.value = _uiState.value.copy(
            connected = false,
            connecting = false,
            statusText = "Disconnected",
            activeWindow = "",
            windowControls = WindowControlState(),
            keyboardLayerState = keyboardLayerState,
            stateFlags = _uiState.value.stateFlags.toMutableMap().apply {
                this["caps_lock"] = false
                this["fn_lock"] = false
            },
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

        val volumeFromState = parseFloatFromJson(state.extra?.get("system_volume"))
            ?: parseFloatFromJson(state.extra?.get("volume"))

        val capsFromServer = flags["caps_lock"] ?: flags["caps"]
        if (capsFromServer != null) {
            keyboardLayerState = keyboardLayerState.copy(capsLocked = capsFromServer)
        }
        flags["caps_lock"] = keyboardLayerState.capsLocked
        flags["fn_lock"] = keyboardLayerState.fnLocked

        val windowControls = WindowControlState(
            available = flags["window_controls_available"] ?: false,
            canMinimize = flags["window_can_minimize"] ?: false,
            canMaximize = flags["window_can_maximize"] ?: false,
            canClose = flags["window_can_close"] ?: false,
            isMaximized = flags["window_maximized"] ?: false
        )

        _uiState.value = _uiState.value.copy(
            connected = state.connected || _uiState.value.connected,
            mute = state.mute,
            rttMs = state.rtt_ms ?: _uiState.value.rttMs,
            jitterMs = state.jitter_ms ?: _uiState.value.jitterMs,
            systemVolume = volumeFromState ?: _uiState.value.systemVolume,
            activeWindow = state.active_window ?: "",
            windowControls = windowControls,
            keyboardLayerState = keyboardLayerState,
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

    private fun loadDefaultLayout(layoutMode: KeyboardLayoutMode = _uiState.value.keyboardLayoutMode) {
        runCatching {
            applyLayout(layoutRepo.loadDefault(layoutMode), layoutMode)
        }.onFailure {
            _uiState.value = _uiState.value.copy(lastEvent = "Layout load failed: ${it.message}")
        }
    }

    private fun applyLayout(
        layout: LayoutConfig,
        layoutMode: KeyboardLayoutMode = _uiState.value.keyboardLayoutMode
    ) {
        currentLayout = layout
        _uiState.value = _uiState.value.copy(
            layoutName = layout.name,
            keyboardLayoutMode = layoutMode,
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

    private fun updateKeyboardLayerState(nextState: KeyboardLayerState) {
        keyboardLayerState = nextState
        _uiState.value = _uiState.value.copy(
            keyboardLayerState = nextState,
            stateFlags = _uiState.value.stateFlags.toMutableMap().apply {
                this["caps_lock"] = nextState.capsLocked
                this["fn_lock"] = nextState.fnLocked
            }
        )
    }

    private fun applyAddressEditorToken(token: String, layerState: KeyboardLayerState) {
        if (isModifierKey(token) || isCapsLockToken(token)) return
        val nextDraft = ServerAddressEditor.applyToken(
            current = _uiState.value.serverAddressDraft,
            token = token,
            layerState = layerState
        )
        if (nextDraft == _uiState.value.serverAddressDraft) return
        _uiState.value = _uiState.value.copy(serverAddressDraft = nextDraft)
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

    private fun isCapsLockToken(token: String): Boolean {
        return token == "CAPSLOCK"
    }

    private fun isNonModifierToken(token: String): Boolean {
        return !isModifierKey(token) && !isCapsLockToken(token)
    }

    private fun parseFloatFromJson(value: JsonElement?): Float? {
        val raw = runCatching { value?.jsonPrimitive?.contentOrNull }.getOrNull() ?: return null
        return raw.toFloatOrNull()?.coerceIn(0f, 1f)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(context.applicationContext) as T
        }
    }
}
