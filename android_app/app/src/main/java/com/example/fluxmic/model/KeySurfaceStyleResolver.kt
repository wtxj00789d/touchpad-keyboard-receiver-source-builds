package com.example.fluxmic.model

enum class KeySurfaceMode {
    NORMAL,
    LIGHT,
    DARK
}

data class KeySurfaceStyle(
    val fillTopAlpha: Float,
    val fillBottomAlpha: Float,
    val borderAlpha: Float
)

object KeySurfaceStyleResolver {
    fun resolve(
        mode: KeySurfaceMode,
        selected: Boolean,
        lockOn: Boolean,
        active: Boolean
    ): KeySurfaceStyle {
        return when (mode) {
            KeySurfaceMode.LIGHT -> when {
                selected -> KeySurfaceStyle(fillTopAlpha = 0.145f, fillBottomAlpha = 0.075f, borderAlpha = 0.70f)
                lockOn -> KeySurfaceStyle(fillTopAlpha = 0.135f, fillBottomAlpha = 0.068f, borderAlpha = 0.64f)
                active -> KeySurfaceStyle(fillTopAlpha = 0.125f, fillBottomAlpha = 0.060f, borderAlpha = 0.54f)
                else -> KeySurfaceStyle(fillTopAlpha = 0.115f, fillBottomAlpha = 0.052f, borderAlpha = 0.42f)
            }
            KeySurfaceMode.NORMAL -> when {
                selected -> KeySurfaceStyle(fillTopAlpha = 0.20f, fillBottomAlpha = 0.14f, borderAlpha = 0.34f)
                lockOn -> KeySurfaceStyle(fillTopAlpha = 0.18f, fillBottomAlpha = 0.13f, borderAlpha = 0.42f)
                active -> KeySurfaceStyle(fillTopAlpha = 0.16f, fillBottomAlpha = 0.12f, borderAlpha = 0.18f)
                else -> KeySurfaceStyle(fillTopAlpha = 0.14f, fillBottomAlpha = 0.10f, borderAlpha = 0.18f)
            }
            KeySurfaceMode.DARK -> when {
                selected -> KeySurfaceStyle(fillTopAlpha = 0.20f, fillBottomAlpha = 0.14f, borderAlpha = 0.34f)
                lockOn -> KeySurfaceStyle(fillTopAlpha = 0.18f, fillBottomAlpha = 0.13f, borderAlpha = 0.42f)
                active -> KeySurfaceStyle(fillTopAlpha = 0.16f, fillBottomAlpha = 0.12f, borderAlpha = 0.18f)
                else -> KeySurfaceStyle(fillTopAlpha = 0.14f, fillBottomAlpha = 0.10f, borderAlpha = 0.18f)
            }
        }
    }
}
