package com.example.fluxmic.model

data class GlassActionButtonStyle(
    val textTreatment: GlassControlTextTreatment,
    val fillTopAlpha: Float,
    val fillBottomAlpha: Float,
    val borderAlpha: Float,
    val glowAlpha: Float,
    val contentAlpha: Float,
    val scale: Float
) {
    companion object {
        fun resolve(
            toneMode: GlassToneMode,
            active: Boolean,
            pressed: Boolean,
            enabled: Boolean
        ): GlassActionButtonStyle {
            val palette = GlassThemeTokens.controlPalette(
                mode = toneMode,
                surface = GlassControlSurface.BUTTON,
                active = active || pressed
            )

            val topBoost = when {
                pressed -> 0.056f
                active -> 0.034f
                else -> 0f
            }
            val pressedBorderBoost = if (pressed) 1.14f else 1f
            val pressedGlowBoost = if (pressed) 1.10f else 1f
            val enabledGlassFactor = if (enabled) 1f else 0.56f

            val fillTop = (palette.fillAlpha + topBoost) * enabledGlassFactor
            val fillBottom = (palette.fillAlpha * 0.70f) * enabledGlassFactor
            val border = (palette.borderAlpha * pressedBorderBoost) * enabledGlassFactor
            val glow = (palette.glowAlpha * pressedGlowBoost) * enabledGlassFactor
            val content = if (enabled) 1f else 0.58f
            val scale = if (enabled && pressed) 0.982f else 1f

            return GlassActionButtonStyle(
                textTreatment = palette.textTreatment,
                fillTopAlpha = fillTop,
                fillBottomAlpha = fillBottom,
                borderAlpha = border,
                glowAlpha = glow,
                contentAlpha = content,
                scale = scale
            )
        }
    }
}
