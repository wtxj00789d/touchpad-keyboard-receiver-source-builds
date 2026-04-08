package com.example.fluxmic.model

private const val DEFAULT_SERVER_WS_URL = "ws://127.0.0.1:8765"

data class UiState(
    val serverUrl: String = DEFAULT_SERVER_WS_URL,
    val serverAddressDraft: String = ServerAddressFormatter.toDisplayValue(DEFAULT_SERVER_WS_URL),
    val addressEditorExpanded: Boolean = false,
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val statusText: String = "Disconnected",
    val rttMs: Int? = null,
    val jitterMs: Int? = null,
    val mute: Boolean = false,
    val layoutName: String = "Default",
    val keyboardLayoutMode: KeyboardLayoutMode = KeyboardLayoutMode.LAYOUT_60,
    val keyboardLayerState: KeyboardLayerState = KeyboardLayerState(),
    val pages: List<PageConfig> = emptyList(),
    val selectedPageIndex: Int = 0,
    val micLevel: Float = 0f,
    val systemVolume: Float = 1f,
    val activeWindow: String = "",
    val windowControls: WindowControlState = WindowControlState(),
    val lastEvent: String = "",
    val connectionModeHint: String = "Wi-Fi: ws://<PC_IP>:8765 or ADB reverse: ws://127.0.0.1:8765",
    val stateFlags: Map<String, Boolean> = emptyMap()
)
