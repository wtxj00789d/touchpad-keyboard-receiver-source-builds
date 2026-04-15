package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultGlassBackgroundSpecTest {
    @Test
    fun noCustomMediaUsesBundledAtmosphereAndPinsNormalToneValues() {
        val spec = DefaultGlassBackgroundSpecResolver.resolve(
            toneMode = GlassToneMode.NORMAL,
            hasCustomMedia = false
        )

        assertTrue(spec.showBundledAtmosphere)
        assertEquals(0xFF1E2E41.toInt(), spec.toneFilter.tintColorArgb)
        assertEquals(
            GlassThemeTokens.backgroundFilter(GlassToneMode.NORMAL).defaultScrimAlpha,
            spec.toneFilter.scrimAlpha,
            0.0001f
        )
        assertEquals(0.0700f, spec.toneFilter.atmosphereVeilAlpha, 0.0001f)
        assertEquals(0.0760f, spec.toneFilter.vignetteAlpha, 0.0001f)
    }

    @Test
    fun customMediaSkipsBundledAtmosphereAndPinsLightToneScaledValues() {
        val spec = DefaultGlassBackgroundSpecResolver.resolve(
            toneMode = GlassToneMode.LIGHT,
            hasCustomMedia = true
        )

        assertFalse(spec.showBundledAtmosphere)
        assertEquals(0xFFF8F4EC.toInt(), spec.toneFilter.tintColorArgb)
        assertEquals(
            GlassThemeTokens.backgroundFilter(GlassToneMode.LIGHT).customScrimAlpha,
            spec.toneFilter.scrimAlpha,
            0.0001f
        )
        assertEquals(0.0288f, spec.toneFilter.atmosphereVeilAlpha, 0.0001f)
        assertEquals(0.03456f, spec.toneFilter.vignetteAlpha, 0.0001f)
    }

    @Test
    fun darkToneCustomMediaAppliesExpectedScaleAndNonZeroFilter() {
        val noCustom = DefaultGlassBackgroundSpecResolver.resolve(
            toneMode = GlassToneMode.DARK,
            hasCustomMedia = false
        )
        val custom = DefaultGlassBackgroundSpecResolver.resolve(
            toneMode = GlassToneMode.DARK,
            hasCustomMedia = true
        )

        assertTrue(noCustom.showBundledAtmosphere)
        assertFalse(custom.showBundledAtmosphere)
        assertEquals(0xFF060A10.toInt(), custom.toneFilter.tintColorArgb)
        assertEquals(
            GlassThemeTokens.backgroundFilter(GlassToneMode.DARK).customScrimAlpha,
            custom.toneFilter.scrimAlpha,
            0.0001f
        )
        assertEquals(0.0684f, custom.toneFilter.atmosphereVeilAlpha, 0.0001f)
        assertEquals(0.08064f, custom.toneFilter.vignetteAlpha, 0.0001f)

        assertTrue(custom.toneFilter.scrimAlpha > 0f)
        assertTrue(custom.toneFilter.atmosphereVeilAlpha > 0f)
        assertTrue(custom.toneFilter.vignetteAlpha > 0f)
        assertEquals(
            0.72f,
            custom.toneFilter.atmosphereVeilAlpha / noCustom.toneFilter.atmosphereVeilAlpha,
            0.0001f
        )
        assertEquals(
            0.72f,
            custom.toneFilter.vignetteAlpha / noCustom.toneFilter.vignetteAlpha,
            0.0001f
        )
    }

    @Test
    fun resolverUsesDistinctToneTintColors() {
        val light = DefaultGlassBackgroundSpecResolver.resolve(GlassToneMode.LIGHT, hasCustomMedia = false)
        val normal = DefaultGlassBackgroundSpecResolver.resolve(GlassToneMode.NORMAL, hasCustomMedia = false)
        val dark = DefaultGlassBackgroundSpecResolver.resolve(GlassToneMode.DARK, hasCustomMedia = false)

        assertNotEquals(light.toneFilter.tintColorArgb, normal.toneFilter.tintColorArgb)
        assertNotEquals(normal.toneFilter.tintColorArgb, dark.toneFilter.tintColorArgb)
        assertNotEquals(light.toneFilter.tintColorArgb, dark.toneFilter.tintColorArgb)
    }
}
