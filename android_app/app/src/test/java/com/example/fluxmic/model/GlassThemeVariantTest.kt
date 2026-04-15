package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassThemeVariantTest {
    @Test
    fun fromStoredValue_acceptsBorderless() {
        assertEquals(GlassThemeVariant.BORDERLESS, GlassThemeVariant.fromStoredValue("borderless"))
    }

    @Test
    fun fromStoredValue_fallsBackToGlass() {
        assertEquals(GlassThemeVariant.GLASS, GlassThemeVariant.fromStoredValue("???"))
    }

    @Test
    fun next_cyclesBetweenVariants() {
        assertEquals(GlassThemeVariant.BORDERLESS, GlassThemeVariant.GLASS.next())
        assertEquals(GlassThemeVariant.GLASS, GlassThemeVariant.BORDERLESS.next())
    }
}
