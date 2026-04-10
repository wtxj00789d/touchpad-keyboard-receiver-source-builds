package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassToneModeTest {
    @Test
    fun storedValueRoundTripsThroughFromStoredValue() {
        GlassToneMode.entries.forEach { mode ->
            assertEquals(mode, GlassToneMode.fromStoredValue(mode.storedValue))
        }
    }

    @Test
    fun fromStoredValueAcceptsCurrentEnumNamesCaseInsensitively() {
        assertEquals(GlassToneMode.NORMAL, GlassToneMode.fromStoredValue("NORMAL"))
        assertEquals(GlassToneMode.LIGHT, GlassToneMode.fromStoredValue("light"))
        assertEquals(GlassToneMode.DARK, GlassToneMode.fromStoredValue("Dark"))
    }

    @Test
    fun fromStoredValuePreservesLegacyNames() {
        assertEquals(GlassToneMode.LIGHT, GlassToneMode.fromStoredValue("KeySurfaceMode.LIGHT"))
        assertEquals(GlassToneMode.DARK, GlassToneMode.fromStoredValue("key_surface_dark"))
        assertEquals(GlassToneMode.NORMAL, GlassToneMode.fromStoredValue("KEY_TEXT_NORMAL"))
    }

    @Test
    fun fromStoredValueSupportsTrimAndLegacySeparatorNormalization() {
        assertEquals(GlassToneMode.LIGHT, GlassToneMode.fromStoredValue("  light  "))
        assertEquals(GlassToneMode.LIGHT, GlassToneMode.fromStoredValue("key-surface-light"))
        assertEquals(GlassToneMode.DARK, GlassToneMode.fromStoredValue("key text dark"))
    }

    @Test
    fun fromStoredValueFallsBackToNormalForUnknownValues() {
        assertEquals(GlassToneMode.NORMAL, GlassToneMode.fromStoredValue(null))
        assertEquals(GlassToneMode.NORMAL, GlassToneMode.fromStoredValue(""))
        assertEquals(GlassToneMode.NORMAL, GlassToneMode.fromStoredValue("retro-neon"))
        assertEquals(GlassToneMode.NORMAL, GlassToneMode.fromStoredValue("Foo.LIGHT"))
    }

    @Test
    fun nextCyclesNormalLightDark() {
        assertEquals(GlassToneMode.LIGHT, GlassToneMode.NORMAL.next())
        assertEquals(GlassToneMode.DARK, GlassToneMode.LIGHT.next())
        assertEquals(GlassToneMode.NORMAL, GlassToneMode.DARK.next())
    }
}
