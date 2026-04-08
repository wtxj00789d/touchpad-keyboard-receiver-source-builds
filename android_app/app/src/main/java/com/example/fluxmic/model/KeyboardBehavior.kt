package com.example.fluxmic.model

object KeyboardBehavior {
    private const val MAX_ACTIVE_KEYS = 8
    private val FN_OUTPUT_TOKEN_MAP = mapOf(
        "k_1" to "F1",
        "k_2" to "F2",
        "k_3" to "F3",
        "k_4" to "F4",
        "k_5" to "F5",
        "k_6" to "F6",
        "k_7" to "F7",
        "k_8" to "F8",
        "k_9" to "F9",
        "k_0" to "F10",
        "k_minus" to "F11",
        "k_equal" to "F12",
        "k_backspace" to "DELETE",
        "k_p" to "PRINTSCREEN",
        "k_lbracket" to "HOME",
        "k_rbracket" to "END",
        "k_semicolon" to "PGUP",
        "k_apostrophe" to "PGDN"
    )
    private val FN_LABEL_MAP = mapOf(
        "k_1" to "F1",
        "k_2" to "F2",
        "k_3" to "F3",
        "k_4" to "F4",
        "k_5" to "F5",
        "k_6" to "F6",
        "k_7" to "F7",
        "k_8" to "F8",
        "k_9" to "F9",
        "k_0" to "F10",
        "k_minus" to "F11",
        "k_equal" to "F12",
        "k_backspace" to "Delete",
        "k_p" to "PrtSc",
        "k_lbracket" to "Home",
        "k_rbracket" to "End",
        "k_semicolon" to "PgUp",
        "k_apostrophe" to "PgDn"
    )

    fun maxActiveKeys(): Int = MAX_ACTIVE_KEYS

    fun toggleFn(state: KeyboardLayerState): KeyboardLayerState {
        return state.copy(fnLocked = !state.fnLocked)
    }

    fun toggleCaps(state: KeyboardLayerState): KeyboardLayerState {
        return state.copy(capsLocked = !state.capsLocked)
    }

    fun onKeyDown(
        state: KeyboardLayerState,
        keyId: String,
        token: String
    ): KeyDownResult {
        if (state.activeKeyTokens.contains(token)) {
            return KeyDownResult(state = state, accepted = true)
        }
        if (state.activeKeyTokens.size >= MAX_ACTIVE_KEYS) {
            return KeyDownResult(state = state, accepted = false)
        }

        val nextKeys = LinkedHashSet(state.activeKeyTokens).apply { add(token) }
        val nextState = state.copy(
            activeKeyTokens = nextKeys,
            shiftPressed = nextKeys.any(::isShiftToken)
        )
        return KeyDownResult(state = nextState, accepted = true)
    }

    fun onKeyUp(
        state: KeyboardLayerState,
        keyId: String,
        token: String
    ): KeyboardLayerState {
        val nextKeys = LinkedHashSet(state.activeKeyTokens).apply { remove(token) }
        return state.copy(
            activeKeyTokens = nextKeys,
            shiftPressed = nextKeys.any(::isShiftToken)
        )
    }

    fun defaultRepeatProfile(): RepeatProfile {
        return RepeatProfile(initialDelayMs = 350L, repeatIntervalMs = 45L)
    }

    fun resolveOutputToken(
        token: String,
        keyId: String,
        layerState: KeyboardLayerState
    ): String {
        if (!layerState.fnLocked) {
            return token
        }
        return FN_OUTPUT_TOKEN_MAP[keyId] ?: token
    }

    fun remapLabel(
        label: String,
        keyId: String,
        layerState: KeyboardLayerState
    ): String {
        if (layerState.fnLocked) {
            return FN_LABEL_MAP[keyId] ?: label
        }

        if (label.length == 1 && label[0].isLetter()) {
            val uppercase = layerState.capsLocked.xor(layerState.shiftPressed)
            return if (uppercase) label.uppercase() else label.lowercase()
        }
        if (layerState.shiftPressed && label == "1") {
            return "!"
        }
        return label
    }

    fun isFnToken(token: String): Boolean {
        return token.equals("FN", ignoreCase = true)
    }

    fun isShiftToken(token: String): Boolean {
        return token.equals("SHIFT", ignoreCase = true) ||
            token.equals("LSHIFT", ignoreCase = true) ||
            token.equals("RSHIFT", ignoreCase = true)
    }
}
