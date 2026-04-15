package com.example.fluxmic.model

data class DefaultGlassToneFilterSpec(
    val tintColorArgb: Int,
    val scrimAlpha: Float,
    val atmosphereVeilAlpha: Float,
    val vignetteAlpha: Float
)

data class DefaultGlassBackgroundSpec(
    val showBundledAtmosphere: Boolean,
    val toneFilter: DefaultGlassToneFilterSpec
)

object DefaultGlassBackgroundSpecResolver {
    fun resolve(
        toneMode: GlassToneMode,
        hasCustomMedia: Boolean
    ): DefaultGlassBackgroundSpec {
        val filter = GlassThemeTokens.backgroundFilter(toneMode)
        val scrimAlpha = if (hasCustomMedia) filter.customScrimAlpha else filter.defaultScrimAlpha
        val customScale = if (hasCustomMedia) 0.72f else 1f

        val tintColor = when (toneMode) {
            GlassToneMode.LIGHT -> 0xFFF8F4EC.toInt()
            GlassToneMode.NORMAL -> 0xFF1E2E41.toInt()
            GlassToneMode.DARK -> 0xFF060A10.toInt()
        }

        val atmosphereVeil = when (toneMode) {
            GlassToneMode.LIGHT -> 0.040f
            GlassToneMode.NORMAL -> 0.070f
            GlassToneMode.DARK -> 0.095f
        } * customScale

        val vignette = when (toneMode) {
            GlassToneMode.LIGHT -> 0.048f
            GlassToneMode.NORMAL -> 0.076f
            GlassToneMode.DARK -> 0.112f
        } * customScale

        return DefaultGlassBackgroundSpec(
            showBundledAtmosphere = !hasCustomMedia,
            toneFilter = DefaultGlassToneFilterSpec(
                tintColorArgb = tintColor,
                scrimAlpha = scrimAlpha,
                atmosphereVeilAlpha = atmosphereVeil,
                vignetteAlpha = vignette
            )
        )
    }
}
