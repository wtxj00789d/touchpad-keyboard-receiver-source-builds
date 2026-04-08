package com.example.fluxmic.model

data class KeyboardLayerState(
    val fnLocked: Boolean = false,
    val capsLocked: Boolean = false,
    val shiftPressed: Boolean = false,
    val activeKeyTokens: LinkedHashSet<String> = linkedSetOf()
)

data class KeyDownResult(
    val state: KeyboardLayerState,
    val accepted: Boolean
)

data class RepeatProfile(
    val initialDelayMs: Long,
    val repeatIntervalMs: Long
)
