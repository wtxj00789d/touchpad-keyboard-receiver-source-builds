package com.example.fluxmic.touchpad.ui.viewmodel

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fluxmic.model.ActionMessage
import com.example.fluxmic.model.HelloMessage
import com.example.fluxmic.model.PingMessage
import com.example.fluxmic.model.StateMessage
import com.example.fluxmic.net.PanelWebSocketClient
import com.example.fluxmic.touchpad.BuildConfig
import com.example.fluxmic.touchpad.model.TouchpadUiState
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class TouchpadViewModel(private val appContext: Context) : ViewModel(), PanelWebSocketClient.Listener {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val wsClient = PanelWebSocketClient()
    private val sequence = AtomicLong(1)
    private val deviceId = buildDeviceId(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        TouchpadUiState(serverUrl = loadSavedServerUrl())
    )
    val uiState: StateFlow<TouchpadUiState> = _uiState.asStateFlow()

    private var pingJob: Job? = null

    init {
        wsClient.setListener(this)
    }

    fun setServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
        saveServerUrl(url)
    }

    fun connect() {
        val url = _uiState.value.serverUrl.trim()
        if (url.isEmpty()) return

        _uiState.value = _uiState.value.copy(connecting = true, statusText = "Connecting...")
        wsClient.connect(url)
    }

    fun disconnect() {
        wsClient.disconnect()
        stopPingLoop()
        _uiState.value = _uiState.value.copy(
            connected = false,
            connecting = false,
            statusText = "Disconnected",
            dragging = false
        )
    }

    fun onMove(dxPx: Float, dyPx: Float) {
        if (!_uiState.value.connected) return

        val scaledX = (dxPx * POINTER_GAIN).roundToInt()
        val scaledY = (dyPx * POINTER_GAIN).roundToInt()
        if (scaledX == 0 && scaledY == 0) return

        sendMousePayload(
            buildJsonObject {
                put("op", JsonPrimitive("MOVE_REL"))
                put("dx", JsonPrimitive(scaledX))
                put("dy", JsonPrimitive(scaledY))
            }
        )
    }

    fun onScroll(delta: Int) {
        if (!_uiState.value.connected || delta == 0) return

        val adjusted = (delta * SCROLL_GAIN).roundToInt()
        if (adjusted == 0) return

        sendMousePayload(
            buildJsonObject {
                put("op", JsonPrimitive("SCROLL"))
                put("delta", JsonPrimitive(adjusted))
            }
        )
    }

    fun leftClick() {
        sendMousePayload(
            buildJsonObject {
                put("op", JsonPrimitive("CLICK"))
                put("button", JsonPrimitive("left"))
            }
        )
    }

    fun rightClick() {
        sendMousePayload(
            buildJsonObject {
                put("op", JsonPrimitive("CLICK"))
                put("button", JsonPrimitive("right"))
            }
        )
    }

    fun doubleClick() {
        sendMousePayload(
            buildJsonObject {
                put("op", JsonPrimitive("DOUBLE_CLICK"))
                put("button", JsonPrimitive("left"))
            }
        )
    }

    fun beginDrag() {
        if (!_uiState.value.connected || _uiState.value.dragging) return

        sendMousePayload(
            buildJsonObject {
                put("op", JsonPrimitive("BUTTON_DOWN"))
                put("button", JsonPrimitive("left"))
            }
        )
        _uiState.value = _uiState.value.copy(dragging = true)
    }

    fun endDrag() {
        if (!_uiState.value.dragging) return

        if (_uiState.value.connected) {
            sendMousePayload(
                buildJsonObject {
                    put("op", JsonPrimitive("BUTTON_UP"))
                    put("button", JsonPrimitive("left"))
                }
            )
        }
        _uiState.value = _uiState.value.copy(dragging = false)
    }

    override fun onConnected() {
        _uiState.value = _uiState.value.copy(
            connected = true,
            connecting = false,
            statusText = "Connected"
        )

        sendHello()
        startPingLoop()
    }

    override fun onDisconnected(reason: String) {
        stopPingLoop()
        _uiState.value = _uiState.value.copy(
            connected = false,
            connecting = false,
            statusText = "Disconnected",
            lastEvent = reason,
            dragging = false
        )
    }

    override fun onError(reason: String) {
        stopPingLoop()
        _uiState.value = _uiState.value.copy(
            connecting = false,
            statusText = "Error",
            lastEvent = reason,
            dragging = false
        )
    }

    override fun onState(state: StateMessage) {
        _uiState.value = _uiState.value.copy(
            connected = state.connected || _uiState.value.connected,
            jitterMs = state.jitter_ms ?: _uiState.value.jitterMs
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

    private fun startPingLoop() {
        stopPingLoop()
        pingJob = viewModelScope.launch {
            while (true) {
                wsClient.sendText(json.encodeToString(PingMessage(ts = SystemClock.elapsedRealtime())))
                delay(2_000)
            }
        }
    }

    private fun stopPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun sendHello() {
        val hello = HelloMessage(
            role = "touchpad",
            device_id = deviceId,
            app_version = BuildConfig.VERSION_NAME,
            seq = sequence.getAndIncrement(),
            ts = System.currentTimeMillis()
        )
        wsClient.sendText(json.encodeToString(hello))
    }

    private fun sendMousePayload(payload: JsonObject) {
        if (!_uiState.value.connected) return

        val msg = ActionMessage(
            kind = "MOUSE",
            payload = payload,
            seq = sequence.getAndIncrement(),
            ts = System.currentTimeMillis()
        )
        wsClient.sendText(json.encodeToString(msg))
    }

    private fun buildDeviceId(context: Context): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "unknown"

        val model = (Build.MODEL ?: "android").replace(" ", "_")
        return "$model-$androidId"
    }

    private fun loadSavedServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_SERVER_URL
    }

    private fun saveServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TouchpadViewModel(context.applicationContext) as T
        }
    }

    companion object {
        private const val PREFS_NAME = "fluxmic_touchpad_prefs"
        private const val KEY_SERVER_URL = "server_ws_url"
        private const val DEFAULT_SERVER_URL = "ws://127.0.0.1:8765"
        private const val POINTER_GAIN = 1.45f
        private const val SCROLL_GAIN = 1.8f
    }
}
