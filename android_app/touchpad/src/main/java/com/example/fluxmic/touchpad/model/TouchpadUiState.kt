package com.example.fluxmic.touchpad.model

data class TouchpadUiState(
    val serverUrl: String = "ws://127.0.0.1:8765",
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val statusText: String = "Disconnected",
    val rttMs: Int? = null,
    val jitterMs: Int? = null,
    val lastEvent: String = "",
    val dragging: Boolean = false
)
