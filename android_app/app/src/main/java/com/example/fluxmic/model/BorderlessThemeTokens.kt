package com.example.fluxmic.model

data class BorderlessActionChrome(
    val fillAlpha: Float,
    val borderAlpha: Float,
    val glowAlpha: Float
)

data class BorderlessIndicatorDotStyle(
    val fillAlpha: Float,
    val borderAlpha: Float
)

data class BorderlessSectionStyle(
    val fillAlpha: Float,
    val borderAlpha: Float,
    val glowAlpha: Float
)

data class BorderlessSliderTokens(
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
)

object BorderlessThemeTokens {
    fun section(toneMode: GlassToneMode): BorderlessSectionStyle = when (toneMode) {
        GlassToneMode.LIGHT -> BorderlessSectionStyle(fillAlpha = 0.038f, borderAlpha = 0.18f, glowAlpha = 0.06f)
        GlassToneMode.NORMAL -> BorderlessSectionStyle(fillAlpha = 0.030f, borderAlpha = 0.14f, glowAlpha = 0.05f)
        GlassToneMode.DARK -> BorderlessSectionStyle(fillAlpha = 0.022f, borderAlpha = 0.11f, glowAlpha = 0.04f)
    }

    fun actionChrome(
        toneMode: GlassToneMode,
        pressed: Boolean,
        editing: Boolean
    ): BorderlessActionChrome {
        val revealed = pressed || editing
        return when (toneMode) {
            GlassToneMode.LIGHT -> if (revealed) {
                BorderlessActionChrome(fillAlpha = 0.105f, borderAlpha = 0.215f, glowAlpha = 0.085f)
            } else {
                BorderlessActionChrome(fillAlpha = 0f, borderAlpha = 0f, glowAlpha = 0f)
            }
            GlassToneMode.NORMAL -> if (revealed) {
                BorderlessActionChrome(fillAlpha = 0.092f, borderAlpha = 0.188f, glowAlpha = 0.072f)
            } else {
                BorderlessActionChrome(fillAlpha = 0f, borderAlpha = 0f, glowAlpha = 0f)
            }
            GlassToneMode.DARK -> if (revealed) {
                BorderlessActionChrome(fillAlpha = 0.080f, borderAlpha = 0.166f, glowAlpha = 0.064f)
            } else {
                BorderlessActionChrome(fillAlpha = 0f, borderAlpha = 0f, glowAlpha = 0f)
            }
        }
    }

    fun keySlot(
        toneMode: GlassToneMode,
        pressed: Boolean,
        editing: Boolean
    ): BorderlessActionChrome = actionChrome(
        toneMode = toneMode,
        pressed = pressed,
        editing = editing
    )

    fun indicatorDot(
        toneMode: GlassToneMode,
        on: Boolean
    ): BorderlessIndicatorDotStyle {
        if (!on) {
            return BorderlessIndicatorDotStyle(fillAlpha = 0f, borderAlpha = 0f)
        }
        return when (toneMode) {
            GlassToneMode.LIGHT -> BorderlessIndicatorDotStyle(fillAlpha = 0.86f, borderAlpha = 0.56f)
            GlassToneMode.NORMAL -> BorderlessIndicatorDotStyle(fillAlpha = 0.80f, borderAlpha = 0.52f)
            GlassToneMode.DARK -> BorderlessIndicatorDotStyle(fillAlpha = 0.76f, borderAlpha = 0.48f)
        }
    }

    fun slider(
        toneMode: GlassToneMode,
        engaged: Boolean,
        enabled: Boolean
    ): BorderlessSliderTokens {
        val base = when (toneMode) {
            GlassToneMode.LIGHT -> BorderlessSliderTokens(
                trackTopAlpha = 0.055f,
                trackBottomAlpha = 0.025f,
                trackBorderAlpha = 0.110f,
                fillTopAlpha = 0.235f,
                fillBottomAlpha = 0.145f,
                fillGlowAlpha = 0.082f,
                thumbFillAlpha = 0.28f,
                thumbBorderAlpha = 0.18f,
                thumbGlowAlpha = 0.06f,
                thumbScale = 1f
            )
            GlassToneMode.NORMAL -> BorderlessSliderTokens(
                trackTopAlpha = 0.047f,
                trackBottomAlpha = 0.022f,
                trackBorderAlpha = 0.092f,
                fillTopAlpha = 0.205f,
                fillBottomAlpha = 0.126f,
                fillGlowAlpha = 0.070f,
                thumbFillAlpha = 0.24f,
                thumbBorderAlpha = 0.16f,
                thumbGlowAlpha = 0.05f,
                thumbScale = 1f
            )
            GlassToneMode.DARK -> BorderlessSliderTokens(
                trackTopAlpha = 0.040f,
                trackBottomAlpha = 0.019f,
                trackBorderAlpha = 0.080f,
                fillTopAlpha = 0.176f,
                fillBottomAlpha = 0.108f,
                fillGlowAlpha = 0.062f,
                thumbFillAlpha = 0.21f,
                thumbBorderAlpha = 0.14f,
                thumbGlowAlpha = 0.045f,
                thumbScale = 1f
            )
        }

        val engagedBoost = if (engaged) 1.18f else 1f
        val enabledFactor = if (enabled) 1f else 0.58f

        return base.copy(
            trackTopAlpha = (base.trackTopAlpha * enabledFactor).coerceIn(0f, 1f),
            trackBottomAlpha = (base.trackBottomAlpha * enabledFactor).coerceIn(0f, 1f),
            trackBorderAlpha = (base.trackBorderAlpha * enabledFactor).coerceIn(0f, 1f),
            fillTopAlpha = (base.fillTopAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
            fillBottomAlpha = (base.fillBottomAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
            fillGlowAlpha = (base.fillGlowAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
            thumbFillAlpha = (base.thumbFillAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
            thumbBorderAlpha = (base.thumbBorderAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
            thumbGlowAlpha = (base.thumbGlowAlpha * engagedBoost * enabledFactor).coerceIn(0f, 1f),
            thumbScale = if (enabled && engaged) 1.04f else 1f
        )
    }
}
