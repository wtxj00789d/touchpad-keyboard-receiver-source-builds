package com.example.fluxmic.model

import org.junit.Assert.assertTrue
import org.junit.Test

class KeySurfaceStyleResolverTest {
    @Test
    fun lightModeKeepsGradientDepthInsteadOfFlatWhiteFill() {
        val style = KeySurfaceStyleResolver.resolve(
            mode = GlassToneMode.LIGHT,
            themeVariant = GlassThemeVariant.GLASS,
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
            mode = GlassToneMode.LIGHT,
            themeVariant = GlassThemeVariant.GLASS,
            selected = false,
            lockOn = false,
            active = false
        )
        val pressed = KeySurfaceStyleResolver.resolve(
            mode = GlassToneMode.LIGHT,
            themeVariant = GlassThemeVariant.GLASS,
            selected = true,
            lockOn = false,
            active = false
        )

        assertTrue(pressed.borderAlpha > base.borderAlpha)
        assertTrue(pressed.fillTopAlpha > pressed.fillBottomAlpha)
    }

    @Test
    fun borderlessRestStateBecomesTextOnly() {
        val style = KeySurfaceStyleResolver.resolve(
            mode = GlassToneMode.NORMAL,
            themeVariant = GlassThemeVariant.BORDERLESS,
            selected = false,
            lockOn = false,
            active = false
        )

        assertTrue(style.fillTopAlpha < 0.03f)
        assertTrue(style.borderAlpha < 0.03f)
    }

    @Test
    fun borderlessPressedStateRevealsGhostSlot() {
        val pressed = KeySurfaceStyleResolver.resolve(
            mode = GlassToneMode.NORMAL,
            themeVariant = GlassThemeVariant.BORDERLESS,
            selected = true,
            lockOn = false,
            active = false
        )

        assertTrue(pressed.fillTopAlpha > 0.07f)
        assertTrue(pressed.borderAlpha > 0.10f)
    }
}
