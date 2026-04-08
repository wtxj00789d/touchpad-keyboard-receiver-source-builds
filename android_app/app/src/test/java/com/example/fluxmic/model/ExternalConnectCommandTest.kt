package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalConnectCommandTest {
    @Test
    fun parsesUsbConnectIntentIntoWebSocketUrl() {
        val command = ExternalConnectCommand.fromIntent(
            ExternalConnectCommand.ACTION_USB_CONNECT,
            "127.0.0.1",
            true,
            8765
        )

        assertEquals("ws://127.0.0.1:8765", command?.targetUrl)
        assertTrue(command?.autoConnect == true)
    }

    @Test
    fun returnsNullWhenActionDoesNotMatch() {
        assertNull(
            ExternalConnectCommand.fromIntent(
                "com.example.fluxmic.action.SOMETHING_ELSE",
                "127.0.0.1",
                true,
                8765
            )
        )
    }

    @Test
    fun fallsBackToDefaultHostWhenHostIsMissingOrBlank() {
        assertEquals(
            "ws://127.0.0.1:8765",
            ExternalConnectCommand.fromIntent(
                ExternalConnectCommand.ACTION_USB_CONNECT,
                null,
                true,
                8765
            )?.targetUrl
        )

        assertEquals(
            "ws://127.0.0.1:8765",
            ExternalConnectCommand.fromIntent(
                ExternalConnectCommand.ACTION_USB_CONNECT,
                "   ",
                true,
                8765
            )?.targetUrl
        )
    }

    @Test
    fun fallsBackToDefaultPortWhenPortIsMissingOrInvalid() {
        assertEquals(
            "ws://127.0.0.1:8765",
            ExternalConnectCommand.fromIntent(
                ExternalConnectCommand.ACTION_USB_CONNECT,
                "127.0.0.1",
                false,
                null
            )?.targetUrl
        )

        assertEquals(
            "ws://127.0.0.1:8765",
            ExternalConnectCommand.fromIntent(
                ExternalConnectCommand.ACTION_USB_CONNECT,
                "127.0.0.1",
                true,
                0
            )?.targetUrl
        )

        assertEquals(
            "ws://127.0.0.1:8765",
            ExternalConnectCommand.fromIntent(
                ExternalConnectCommand.ACTION_USB_CONNECT,
                "127.0.0.1",
                true,
                65536
            )?.targetUrl
        )
    }

    @Test
    fun intentAdapterFallsBackToDefaultPortWhenPortExtraIsAbsent() {
        assertEquals(
            "ws://127.0.0.1:8765",
            ExternalConnectCommand.fromIntent(
                ExternalConnectCommand.ACTION_USB_CONNECT,
                "127.0.0.1",
                false,
                1234
            )?.targetUrl
        )
    }
}
