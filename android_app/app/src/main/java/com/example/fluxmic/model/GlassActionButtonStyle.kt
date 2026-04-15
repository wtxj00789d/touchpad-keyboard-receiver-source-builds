package com.example.fluxmic.model

data class GlassActionButtonStyle(
    val textTreatment: GlassControlTextTreatment,
    val fillTopAlpha: Float,
    val fillBottomAlpha: Float,
    val borderAlpha: Float,
    val glowAlpha: Float,
    val contentAlpha: Float,
    val scale: Float,
    val showIndicatorDot: Boolean
) {
    companion object {
        fun resolve(
            toneMode: GlassToneMode,
            themeVariant: GlassThemeVariant,
            active: Boolean,
            pressed: Boolean,
            enabled: Boolean,
            editing: Boolean = false
        ): GlassActionButtonStyle {
            if (themeVariant == GlassThemeVariant.BORDERLESS) {
                val chrome = BorderlessThemeTokens.actionChrome(
                    toneMode = toneMode,
                    pressed = pressed,
                    editing = editing
                )
                return GlassActionButtonStyle(
                    textTreatment = GlassThemeTokens.controlPalette(
                        mode = toneMode,
                        surface = GlassControlSurface.BUTTON,
                        active = false
                    ).textTreatment,
                    fillTopAlpha = chrome.fillAlpha,
                    fillBottomAlpha = chrome.fillAlpha * 0.72f,
                    borderAlpha = chrome.borderAlpha,
                    glowAlpha = chrome.glowAlpha,
                    contentAlpha = if (enabled) {
                        if (active) 1f else 0.94f
                    } else {
                        0.50f
                    },
                    scale = if (enabled && pressed) 0.992f else 1f,
                    showIndicatorDot = active
                )
            }

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
                scale = scale,
                showIndicatorDot = false
            )
        }
    }
}
