package com.example.fluxmic.model

object ServerAddressEditor {
    private val shiftDigitMap = mapOf(
        "1" to "!",
        "2" to "@",
        "3" to "#",
        "4" to "$",
        "5" to "%",
        "6" to "^",
        "7" to "&",
        "8" to "*",
        "9" to "(",
        "0" to ")"
    )

    private val symbolMap = mapOf(
        "GRAVE" to ("`" to "~"),
        "MINUS" to ("-" to "_"),
        "EQUALS" to ("=" to "+"),
        "LBRACKET" to ("[" to "{"),
        "RBRACKET" to ("]" to "}"),
        "BACKSLASH" to ("\\" to "|"),
        "SEMICOLON" to (";" to ":"),
        "APOSTROPHE" to ("'" to "\""),
        "COMMA" to ("," to "<"),
        "PERIOD" to ("." to ">"),
        "SLASH" to ("/" to "?")
    )

    fun applyToken(current: String, token: String, layerState: KeyboardLayerState): String {
        return when (token.uppercase()) {
            "BACKSPACE", "DELETE" -> current.dropLast(1)
            "SPACE" -> current
            else -> {
                val printable = resolvePrintableToken(token.uppercase(), layerState) ?: return current
                current + printable
            }
        }
    }

    private fun resolvePrintableToken(token: String, layerState: KeyboardLayerState): String? {
        if (token.length == 1 && token[0].isLetter()) {
            val uppercase = layerState.capsLocked.xor(layerState.shiftPressed)
            return if (uppercase) token else token.lowercase()
        }
        if (token.length == 1 && token[0].isDigit()) {
            return if (layerState.shiftPressed) shiftDigitMap[token] ?: token else token
        }
        val symbolPair = symbolMap[token] ?: return null
        return if (layerState.shiftPressed) symbolPair.second else symbolPair.first
    }
}
