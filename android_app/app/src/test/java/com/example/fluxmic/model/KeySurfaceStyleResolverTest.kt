package com.example.fluxmic.model

import org.junit.Assert.assertTrue
import org.junit.Test

class KeySurfaceStyleResolverTest {
    @Test
    fun lightModeKeepsGradientDepthInsteadOfFlatWhiteFill() {
        val style = KeySurfaceStyleResolver.resolve(
            mode = KeySurfaceMode.LIGHT,
            selected = false,
            lockOn = false,
            active = false
        )

        assertTrue(style.fillTopAlpha > style.fillBottomAlpha)
        assertTrue(style.fillTopAlpha < 0.16f)
        assertTrue(style.fillBottomAlpha > 0f)
    }

    @Test
    fun lightModePressedStateBrightensBorderWithoutLosingGradient() {
        val base = KeySurfaceStyleResolver.resolve(
            mode = KeySurfaceMode.LIGHT,
            selected = false,
            lockOn = false,
            active = false
        )
        val pressed = KeySurfaceStyleResolver.resolve(
            mode = KeySurfaceMode.LIGHT,
            selected = true,
            lockOn = false,
            active = false
        )

        assertTrue(pressed.borderAlpha > base.borderAlpha)
        assertTrue(pressed.fillTopAlpha > pressed.fillBottomAlpha)
    }
}
