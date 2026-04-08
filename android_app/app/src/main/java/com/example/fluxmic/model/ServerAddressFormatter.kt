package com.example.fluxmic.model

object ServerAddressFormatter {
    private const val WEB_SOCKET_PREFIX = "ws://"

    fun normalizeForConnection(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.contains("://")) trimmed else "$WEB_SOCKET_PREFIX$trimmed"
    }

    fun toDisplayValue(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith(WEB_SOCKET_PREFIX, ignoreCase = true)) {
            trimmed.substring(WEB_SOCKET_PREFIX.length)
        } else {
            trimmed
        }
    }
}
