package com.example.fluxmic.net

import android.util.Log
import com.example.fluxmic.model.StateMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class PanelWebSocketClient {
    interface Listener {
        fun onConnected()
        fun onDisconnected(reason: String)
        fun onError(reason: String)
        fun onState(state: StateMessage)
        fun onPong(sentTs: Long)
        fun onEvent(message: String)
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var listener: Listener? = null

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun connect(url: String) {
        disconnect(1000, "reconnect")
        val request = Request.Builder().url(url).build()
        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                handleSocketOpened(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleSocketTextMessage(webSocket, text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleSocketBinaryMessage(webSocket, bytes)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                handleSocketClosing(webSocket, code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleSocketClosed(webSocket, code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleSocketFailure(webSocket, t)
            }
        })
        webSocket = socket
    }

    fun disconnect(code: Int = 1000, reason: String = "manual") {
        webSocket?.close(code, reason)
        webSocket = null
    }

    fun sendText(text: String): Boolean {
        return webSocket?.send(text) ?: false
    }

    fun sendBinary(data: ByteArray): Boolean {
        return webSocket?.send(ByteString.of(*data)) ?: false
    }

    internal fun setActiveSocketForTesting(socket: WebSocket?) {
        webSocket = socket
    }

    internal fun handleSocketOpened(socket: WebSocket) {
        if (!isCurrentSocket(socket)) return
        listener?.onConnected()
    }

    internal fun handleSocketTextMessage(socket: WebSocket, text: String) {
        if (!isCurrentSocket(socket)) return
        handleTextMessage(text)
    }

    internal fun handleSocketBinaryMessage(socket: WebSocket, bytes: ByteString) {
        if (!isCurrentSocket(socket)) return
        // Android side currently sends binary audio only.
        // Keep this callback for future reverse audio/state binary features.
        Log.d(TAG, "Binary from server ignored: ${bytes.size} bytes")
    }

    internal fun handleSocketClosing(socket: WebSocket, code: Int, reason: String) {
        if (!isCurrentSocket(socket)) return
        socket.close(code, reason)
        listener?.onDisconnected("closing($code): $reason")
    }

    internal fun handleSocketClosed(socket: WebSocket, code: Int, reason: String) {
        if (!isCurrentSocket(socket)) return
        webSocket = null
        listener?.onDisconnected("closed($code): $reason")
    }

    internal fun handleSocketFailure(socket: WebSocket, throwable: Throwable) {
        if (!isCurrentSocket(socket)) return
        webSocket = null
        listener?.onError(throwable.message ?: "websocket failure")
    }

    private fun handleTextMessage(text: String) {
        runCatching {
            val root = json.parseToJsonElement(text).jsonObject
            when (root["type"]?.jsonPrimitive?.contentOrNull) {
                "state" -> {
                    val state = StateMessage(
                        connected = root["connected"]?.jsonPrimitive?.booleanOrNull ?: false,
                        mute = root["mute"]?.jsonPrimitive?.booleanOrNull ?: false,
                        rtt_ms = root["rtt_ms"]?.jsonPrimitive?.intOrNull,
                        jitter_ms = root["jitter_ms"]?.jsonPrimitive?.intOrNull,
                        active_window = root["active_window"]?.jsonPrimitive?.contentOrNull,
                        audio_codec = root["audio_codec"]?.jsonPrimitive?.contentOrNull,
                        extra = JsonObject(root)
                    )
                    listener?.onState(state)
                }

                "pong" -> {
                    val sentTs = root["ts"]?.jsonPrimitive?.longOrNull ?: return
                    listener?.onPong(sentTs)
                }

                "event" -> {
                    val message = root["message"]?.jsonPrimitive?.contentOrNull ?: ""
                    listener?.onEvent(message)
                }

                else -> Unit
            }
        }.onFailure {
            listener?.onError("Invalid server JSON: ${it.message}")
        }
    }

    companion object {
        private const val TAG = "PanelWebSocketClient"
    }

    private fun isCurrentSocket(socket: WebSocket): Boolean {
        return webSocket === socket
    }
}
