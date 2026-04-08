package com.example.fluxmic.model

import android.content.Intent

data class ExternalConnectCommand(
    val targetUrl: String,
    val autoConnect: Boolean = true
) {
    companion object {
        private const val DEFAULT_HOST = "127.0.0.1"
        private const val DEFAULT_PORT = 8765
        const val ACTION_USB_CONNECT = "com.example.fluxmic.action.USB_CONNECT"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"

        fun fromIntent(intent: Intent?): ExternalConnectCommand? {
            val action = intent?.action
            val host = intent?.getStringExtra(EXTRA_HOST)
            val hasPortExtra = intent?.hasExtra(EXTRA_PORT) == true
            val port = if (hasPortExtra) intent?.getIntExtra(EXTRA_PORT, -1) else null
            return fromIntent(action, host, hasPortExtra, port)
        }

        fun fromIntent(action: String?, host: String?, port: Int?): ExternalConnectCommand? {
            return fromIntent(action, host, hasPortExtra = port != null, port = port)
        }

        internal fun fromIntent(
            action: String?,
            host: String?,
            hasPortExtra: Boolean,
            port: Int?
        ): ExternalConnectCommand? {
            if (action != ACTION_USB_CONNECT) return null

            val normalizedHost = host?.trim().takeUnless { it.isNullOrEmpty() } ?: DEFAULT_HOST
            val normalizedPort = if (hasPortExtra) {
                port?.takeIf { it in 1..65535 } ?: DEFAULT_PORT
            } else {
                DEFAULT_PORT
            }

            return ExternalConnectCommand(targetUrl = "ws://$normalizedHost:$normalizedPort")
        }
    }
}
