package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassActionButtonStyleTest {
    @Test
    fun pressedStateTightensScaleAndBrightensBorder() {
        val base = GlassActionButtonStyle.resolve(
            toneMode = GlassToneMode.NORMAL,
            active = false,
            pressed = false,
            enabled = true
        )
        val pressed = GlassActionButtonStyle.resolve(
            toneMode = GlassToneMode.NORMAL,
            active = false,
            pressed = true,
            enabled = true
        )

        assertTrue(pressed.scale < base.scale)
        assertTrue(pressed.borderAlpha > base.borderAlpha)
    }

    @Test
    fun activeStateCarriesMoreGlowThanIdle() {
        val idle = GlassActionButtonStyle.resolve(
            toneMode = GlassToneMode.DARK,
            active = false,
            pressed = false,
            enabled = true
        )
        val active = GlassActionButtonStyle.resolve(
            toneMode = GlassToneMode.DARK,
            active = true,
            pressed = false,
            enabled = true
        )

        assertTrue(active.glowAlpha > idle.glowAlpha)
    }

    @Test
    fun toneModeKeepsMappedTextTreatment() {
        val light = GlassActionButtonStyle.resolve(
            toneMode = GlassToneMode.LIGHT,
            active = false,
            pressed = false,
            enabled = true
        )
        val normal = GlassActionButtonStyle.resolve(
            toneMode = GlassToneMode.NORMAL,
            active = false,
            pressed = false,
            enabled = true
        )
        val dark = GlassActionButtonStyle.resolve(
            toneMode = GlassToneMode.DARK,
            active = false,
            pressed = false,
            enabled = true
        )

        assertEquals(GlassControlTextTreatment.DARK, light.textTreatment)
        assertEquals(GlassControlTextTreatment.LIGHT_WITH_SHADOW, normal.textTreatment)
        assertEquals(GlassControlTextTreatment.LIGHT_PLAIN, dark.textTreatment)
    }

    @Test
    fun disabledStateSoftensContentAndGlow() {
        val enabled = GlassActionButtonStyle.resolve(
            toneMode = GlassToneMode.NORMAL,
            active = true,
            pressed = false,
            enabled = true
        )
        val disabled = GlassActionButtonStyle.resolve(
            toneMode = GlassToneMode.NORMAL,
            active = true,
            pressed = false,
            enabled = false
        )

        assertTrue(disabled.contentAlpha < enabled.contentAlpha)
        assertTrue(disabled.glowAlpha < enabled.glowAlpha)
    }
}
