package com.example.fluxmic.model

import org.junit.Assert.assertTrue
import org.junit.Test

class BorderlessThemeTokensTest {
    @Test
    fun restActionChrome_isNearZero() {
        val style = BorderlessThemeTokens.actionChrome(
            toneMode = GlassToneMode.NORMAL,
            pressed = false,
            editing = false
        )
        assertTrue(style.fillAlpha < 0.03f)
        assertTrue(style.borderAlpha < 0.03f)
    }

    @Test
    fun pressedActionChrome_lightsUpSlot() {
        val style = BorderlessThemeTokens.actionChrome(
            toneMode = GlassToneMode.NORMAL,
            pressed = true,
            editing = false
        )
        assertTrue(style.fillAlpha > 0.07f)
        assertTrue(style.borderAlpha > 0.10f)
    }

    @Test
    fun activeIndicator_isVisible() {
        val style = BorderlessThemeTokens.indicatorDot(
            toneMode = GlassToneMode.NORMAL,
            on = true
        )
        assertTrue(style.fillAlpha > 0.60f)
    }
}
