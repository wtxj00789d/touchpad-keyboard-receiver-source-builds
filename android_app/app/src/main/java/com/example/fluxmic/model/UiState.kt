package com.example.fluxmic.model

data class UiState(
    val serverUrl: String = "ws://127.0.0.1:8765",
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val statusText: String = "Disconnected",
    val rttMs: Int? = null,
    val jitterMs: Int? = null,
    val mute: Boolean = false,
    val layoutName: String = "Default",
    val pages: List<PageConfig> = emptyList(),
    val selectedPageIndex: Int = 0,
    val micLevel: Float = 0f,
    val activeWindow: String = "",
    val lastEvent: String = "",
    val connectionModeHint: String = "Wi-Fi: ws://<PC_IP>:8765 or ADB reverse: ws://127.0.0.1:8765",
    val stateFlags: Map<String, Boolean> = emptyMap()
)
