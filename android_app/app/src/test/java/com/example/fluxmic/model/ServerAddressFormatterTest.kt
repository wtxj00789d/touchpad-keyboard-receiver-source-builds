package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerAddressFormatterTest {
    @Test
    fun normalizesBareHostPortIntoWebSocketUrl() {
        assertEquals(
            "ws://192.168.1.10:8765",
            ServerAddressFormatter.normalizeForConnection("192.168.1.10:8765")
        )
    }

    @Test
    fun keepsExplicitWebSocketSchemeUntouched() {
        assertEquals(
            "ws://127.0.0.1:8765",
            ServerAddressFormatter.normalizeForConnection("ws://127.0.0.1:8765")
        )
    }

    @Test
    fun stripsWebSocketSchemeForDisplay() {
        assertEquals(
            "127.0.0.1:8765",
            ServerAddressFormatter.toDisplayValue("ws://127.0.0.1:8765")
        )
    }
}
