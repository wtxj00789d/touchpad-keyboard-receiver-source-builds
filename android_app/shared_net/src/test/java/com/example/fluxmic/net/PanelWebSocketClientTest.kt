package com.example.fluxmic.net

import okhttp3.Request
import okhttp3.WebSocket
import org.junit.Assert.assertEquals
import org.junit.Test

class PanelWebSocketClientTest {
    @Test
    fun staleSocketCallbacksAreIgnoredAfterActiveSocketChanges() {
        val events = mutableListOf<String>()
        val client = PanelWebSocketClient().apply {
            setListener(
                object : PanelWebSocketClient.Listener {
                    override fun onConnected() {
                        events += "connected"
                    }

                    override fun onDisconnected(reason: String) {
                        events += "disconnected:$reason"
                    }

                    override fun onError(reason: String) {
                        events += "error:$reason"
                    }

                    override fun onState(state: com.example.fluxmic.model.StateMessage) = Unit

                    override fun onPong(sentTs: Long) = Unit

                    override fun onEvent(message: String) = Unit
                }
            )
        }

        val oldSocket = FakeWebSocket("old")
        val newSocket = FakeWebSocket("new")

        client.setActiveSocketForTesting(oldSocket)
        client.handleSocketOpened(oldSocket)
        client.setActiveSocketForTesting(newSocket)
        client.handleSocketOpened(newSocket)
        client.handleSocketClosed(oldSocket, 1000, "stale")

        assertEquals(listOf("connected", "connected"), events)
    }

    @Test
    fun activeSocketCloseStillDispatchesDisconnect() {
        val events = mutableListOf<String>()
        val client = PanelWebSocketClient().apply {
            setListener(
                object : PanelWebSocketClient.Listener {
                    override fun onConnected() = Unit

                    override fun onDisconnected(reason: String) {
                        events += reason
                    }

                    override fun onError(reason: String) = Unit

                    override fun onState(state: com.example.fluxmic.model.StateMessage) = Unit

                    override fun onPong(sentTs: Long) = Unit

                    override fun onEvent(message: String) = Unit
                }
            )
        }

        val socket = FakeWebSocket("current")
        client.setActiveSocketForTesting(socket)
        client.handleSocketClosed(socket, 1000, "done")

        assertEquals(listOf("closed(1000): done"), events)
    }

    private class FakeWebSocket(private val label: String) : WebSocket {
        var closed = false

        override fun request(): Request {
            return Request.Builder().url("ws://example.test/$label").build()
        }

        override fun queueSize(): Long = 0

        override fun send(text: String): Boolean = true

        override fun send(bytes: okio.ByteString): Boolean = true

        override fun close(code: Int, reason: String?): Boolean {
            closed = true
            return true
        }

        override fun cancel() = Unit
    }
}
