package com.example.fluxmic.model

enum class GlassControlSurface {
    BUTTON,
    SLIDER
}

enum class GlassControlTextTreatment {
    DARK,
    LIGHT_WITH_SHADOW,
    LIGHT_PLAIN
}

data class GlassControlPalette(
    val textTreatment: GlassControlTextTreatment,
    val fillAlpha: Float,
    val borderAlpha: Float,
    val glowAlpha: Float
) {
    val useDarkText: Boolean
        get() = textTreatment == GlassControlTextTreatment.DARK
}

data class GlassBackgroundFilter(
    val defaultScrimAlpha: Float,
    val customScrimAlpha: Float
)

object GlassThemeTokens {
    fun controlPalette(
        mode: GlassToneMode,
        surface: GlassControlSurface,
        active: Boolean
    ): GlassControlPalette {
        return when (mode) {
            GlassToneMode.LIGHT -> {
                val base = if (surface == GlassControlSurface.SLIDER) 1.08f else 1f
                if (active) {
                    GlassControlPalette(
                        textTreatment = GlassControlTextTreatment.DARK,
                        fillAlpha = 0.255f * base,
                        borderAlpha = 0.44f * base,
                        glowAlpha = 0.31f * base
                    )
                } else {
                    GlassControlPalette(
                        textTreatment = GlassControlTextTreatment.DARK,
                        fillAlpha = 0.205f * base,
                        borderAlpha = 0.34f * base,
                        glowAlpha = 0.22f * base
                    )
                }
            }
            GlassToneMode.NORMAL -> {
                val base = if (surface == GlassControlSurface.SLIDER) 1.04f else 1f
                if (active) {
                    GlassControlPalette(
                        textTreatment = GlassControlTextTreatment.LIGHT_WITH_SHADOW,
                        fillAlpha = 0.215f * base,
                        borderAlpha = 0.30f * base,
                        glowAlpha = 0.21f * base
                    )
                } else {
                    GlassControlPalette(
                        textTreatment = GlassControlTextTreatment.LIGHT_WITH_SHADOW,
                        fillAlpha = 0.17f * base,
                        borderAlpha = 0.23f * base,
                        glowAlpha = 0.14f * base
                    )
                }
            }
            GlassToneMode.DARK -> {
                val base = if (surface == GlassControlSurface.SLIDER) 0.96f else 1f
                if (active) {
                    GlassControlPalette(
                        textTreatment = GlassControlTextTreatment.LIGHT_PLAIN,
                        fillAlpha = 0.165f * base,
                        borderAlpha = 0.27f * base,
                        glowAlpha = 0.18f * base
                    )
                } else {
                    GlassControlPalette(
                        textTreatment = GlassControlTextTreatment.LIGHT_PLAIN,
                        fillAlpha = 0.13f * base,
                        borderAlpha = 0.20f * base,
                        glowAlpha = 0.12f * base
                    )
                }
            }
        }
    }

    fun backgroundFilter(mode: GlassToneMode): GlassBackgroundFilter {
        return when (mode) {
            GlassToneMode.LIGHT -> GlassBackgroundFilter(
                defaultScrimAlpha = 0.11f,
                customScrimAlpha = 0.08f
            )
            GlassToneMode.NORMAL -> GlassBackgroundFilter(
                defaultScrimAlpha = 0.18f,
                customScrimAlpha = 0.14f
            )
            GlassToneMode.DARK -> GlassBackgroundFilter(
                defaultScrimAlpha = 0.27f,
                customScrimAlpha = 0.22f
            )
        }
    }
}
