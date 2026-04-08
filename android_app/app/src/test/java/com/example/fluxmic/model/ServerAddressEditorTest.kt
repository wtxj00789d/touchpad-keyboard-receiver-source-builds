package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerAddressEditorTest {
    @Test
    fun appendsPrintableCharactersForHostAndPortEntry() {
        val layerState = KeyboardLayerState()

        val typed = listOf("1", "9", "2", "PERIOD", "1", "6", "8", "PERIOD", "1", "PERIOD", "1", "0")
            .fold("") { acc, token -> ServerAddressEditor.applyToken(acc, token, layerState) }

        assertEquals("192.168.1.10", typed)
    }

    @Test
    fun supportsShiftedSymbolsNeededForFullWebSocketUrls() {
        val shifted = KeyboardLayerState(shiftPressed = true)

        var typed = "ws"
        typed = ServerAddressEditor.applyToken(typed, "SEMICOLON", shifted)
        typed = ServerAddressEditor.applyToken(typed, "SLASH", KeyboardLayerState())
        typed = ServerAddressEditor.applyToken(typed, "SLASH", KeyboardLayerState())

        assertEquals("ws://", typed)
    }

    @Test
    fun treatsDeleteLikeBackspaceForInlineEditing() {
        assertEquals(
            "192.168.1.1",
            ServerAddressEditor.applyToken("192.168.1.10", "DELETE", KeyboardLayerState())
        )
    }
}
