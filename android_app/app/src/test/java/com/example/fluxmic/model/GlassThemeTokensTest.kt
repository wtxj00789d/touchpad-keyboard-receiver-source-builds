package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassThemeTokensTest {
    @Test
    fun controlPaletteExposesDistinctTextTreatmentPerTone() {
        val light = GlassThemeTokens.controlPalette(GlassToneMode.LIGHT, GlassControlSurface.BUTTON, active = false)
        val normal = GlassThemeTokens.controlPalette(GlassToneMode.NORMAL, GlassControlSurface.BUTTON, active = false)
        val dark = GlassThemeTokens.controlPalette(GlassToneMode.DARK, GlassControlSurface.BUTTON, active = false)

        assertEquals(GlassControlTextTreatment.DARK, light.textTreatment)
        assertEquals(GlassControlTextTreatment.LIGHT_WITH_SHADOW, normal.textTreatment)
        assertEquals(GlassControlTextTreatment.LIGHT_PLAIN, dark.textTreatment)
    }

    @Test
    fun controlPalettePinsRepresentativeButtonValues() {
        val lightActive = GlassThemeTokens.controlPalette(
            mode = GlassToneMode.LIGHT,
            surface = GlassControlSurface.BUTTON,
            active = true
        )
        val normalInactive = GlassThemeTokens.controlPalette(
            mode = GlassToneMode.NORMAL,
            surface = GlassControlSurface.BUTTON,
            active = false
        )
        val darkInactive = GlassThemeTokens.controlPalette(
            mode = GlassToneMode.DARK,
            surface = GlassControlSurface.BUTTON,
            active = false
        )

        assertEquals(GlassControlTextTreatment.DARK, lightActive.textTreatment)
        assertEquals(0.255f, lightActive.fillAlpha, 0.0001f)
        assertEquals(0.44f, lightActive.borderAlpha, 0.0001f)
        assertEquals(0.31f, lightActive.glowAlpha, 0.0001f)

        assertEquals(GlassControlTextTreatment.LIGHT_WITH_SHADOW, normalInactive.textTreatment)
        assertEquals(0.17f, normalInactive.fillAlpha, 0.0001f)
        assertEquals(0.23f, normalInactive.borderAlpha, 0.0001f)
        assertEquals(0.14f, normalInactive.glowAlpha, 0.0001f)

        assertEquals(GlassControlTextTreatment.LIGHT_PLAIN, darkInactive.textTreatment)
        assertEquals(0.13f, darkInactive.fillAlpha, 0.0001f)
        assertEquals(0.20f, darkInactive.borderAlpha, 0.0001f)
        assertEquals(0.12f, darkInactive.glowAlpha, 0.0001f)
    }

    @Test
    fun controlPaletteButtonAndSliderDifferencesMatchToneScaling() {
        val lightButton = GlassThemeTokens.controlPalette(GlassToneMode.LIGHT, GlassControlSurface.BUTTON, active = true)
        val lightSlider = GlassThemeTokens.controlPalette(GlassToneMode.LIGHT, GlassControlSurface.SLIDER, active = true)
        val normalButton = GlassThemeTokens.controlPalette(GlassToneMode.NORMAL, GlassControlSurface.BUTTON, active = true)
        val normalSlider = GlassThemeTokens.controlPalette(GlassToneMode.NORMAL, GlassControlSurface.SLIDER, active = true)
        val darkButton = GlassThemeTokens.controlPalette(GlassToneMode.DARK, GlassControlSurface.BUTTON, active = true)
        val darkSlider = GlassThemeTokens.controlPalette(GlassToneMode.DARK, GlassControlSurface.SLIDER, active = true)

        assertTrue(lightSlider.fillAlpha > lightButton.fillAlpha)
        assertTrue(normalSlider.fillAlpha > normalButton.fillAlpha)
        assertTrue(darkSlider.fillAlpha < darkButton.fillAlpha)

        assertEquals(0.2754f, lightSlider.fillAlpha, 0.0001f)
        assertEquals(0.2236f, normalSlider.fillAlpha, 0.0001f)
        assertEquals(0.1584f, darkSlider.fillAlpha, 0.0001f)
    }

    @Test
    fun controlPaletteActiveStateIncreasesGlassStrength() {
        val inactive = GlassThemeTokens.controlPalette(GlassToneMode.NORMAL, GlassControlSurface.BUTTON, active = false)
        val active = GlassThemeTokens.controlPalette(GlassToneMode.NORMAL, GlassControlSurface.BUTTON, active = true)

        assertTrue(active.fillAlpha > inactive.fillAlpha)
        assertTrue(active.borderAlpha > inactive.borderAlpha)
        assertTrue(active.glowAlpha > inactive.glowAlpha)
        assertEquals(inactive.textTreatment, active.textTreatment)
    }

    @Test
    fun backgroundFilterScrimStrengthOrdersLightNormalDark() {
        val light = GlassThemeTokens.backgroundFilter(GlassToneMode.LIGHT)
        val normal = GlassThemeTokens.backgroundFilter(GlassToneMode.NORMAL)
        val dark = GlassThemeTokens.backgroundFilter(GlassToneMode.DARK)

        assertTrue(light.defaultScrimAlpha < normal.defaultScrimAlpha)
        assertTrue(normal.defaultScrimAlpha < dark.defaultScrimAlpha)
        assertTrue(light.customScrimAlpha < normal.customScrimAlpha)
        assertTrue(normal.customScrimAlpha < dark.customScrimAlpha)
    }
}
