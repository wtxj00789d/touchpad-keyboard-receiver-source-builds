package com.example.fluxmic.model

data class GlassSliderStyle(
    val trackTopAlpha: Float,
    val trackBottomAlpha: Float,
    val trackBorderAlpha: Float,
    val fillTopAlpha: Float,
    val fillBottomAlpha: Float,
    val fillGlowAlpha: Float,
    val thumbFillAlpha: Float,
    val thumbBorderAlpha: Float,
    val thumbGlowAlpha: Float,
    val thumbScale: Float
) {
    companion object {
        fun resolve(
            toneMode: GlassToneMode,
            engaged: Boolean,
            enabled: Boolean
        ): GlassSliderStyle {
            val base = when (toneMode) {
                GlassToneMode.LIGHT -> GlassSliderStyle(
                    trackTopAlpha = 0.20f,
                    trackBottomAlpha = 0.11f,
                    trackBorderAlpha = 0.27f,
                    fillTopAlpha = 0.48f,
                    fillBottomAlpha = 0.34f,
                    fillGlowAlpha = 0.24f,
                    thumbFillAlpha = 0.94f,
                    thumbBorderAlpha = 0.56f,
                    thumbGlowAlpha = 0.28f,
                    thumbScale = 1f
                )
                GlassToneMode.NORMAL -> GlassSliderStyle(
                    trackTopAlpha = 0.17f,
                    trackBottomAlpha = 0.09f,
                    trackBorderAlpha = 0.23f,
                    fillTopAlpha = 0.41f,
                    fillBottomAlpha = 0.28f,
                    fillGlowAlpha = 0.18f,
                    thumbFillAlpha = 0.89f,
                    thumbBorderAlpha = 0.48f,
                    thumbGlowAlpha = 0.23f,
                    thumbScale = 1f
                )
                GlassToneMode.DARK -> GlassSliderStyle(
                    trackTopAlpha = 0.13f,
                    trackBottomAlpha = 0.07f,
                    trackBorderAlpha = 0.19f,
                    fillTopAlpha = 0.33f,
                    fillBottomAlpha = 0.22f,
                    fillGlowAlpha = 0.14f,
                    thumbFillAlpha = 0.82f,
                    thumbBorderAlpha = 0.39f,
                    thumbGlowAlpha = 0.18f,
                    thumbScale = 1f
                )
            }

            val engagedBoost = if (engaged) 1.08f else 1f
            val enabledFactor = if (enabled) 1f else 0.52f

            return base.copy(
                trackTopAlpha = (base.trackTopAlpha * enabledFactor).coerceIn(0f, 1f),
                trackBottomAlpha = (base.trackBottomAlpha * enabledFactor).coerceIn(0f, 1f),
                trackBorderAlpha = (base.trackBorderAlpha * enabledFactor).coerceIn(0f, 1f),
                fillTopAlpha = (base.fillTopAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
                fillBottomAlpha = (base.fillBottomAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
                fillGlowAlpha = (base.fillGlowAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
                thumbFillAlpha = (base.thumbFillAlpha * enabledFactor).coerceIn(0f, 1f),
                thumbBorderAlpha = (base.thumbBorderAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
                thumbGlowAlpha = (base.thumbGlowAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
                thumbScale = if (enabled && engaged) 1.07f else 1f
            )
        }
    }
}
