package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLayoutModeTest {
    @Test
    fun fallsBackTo60WhenStoredValueIsUnknown() {
        assertEquals(KeyboardLayoutMode.LAYOUT_60, KeyboardLayoutMode.fromStoredValue("unknown"))
    }

    @Test
    fun resolves68AssetName() {
        assertEquals("default68.json", KeyboardLayoutMode.fromStoredValue("layout_68").assetName)
    }

    @Test
    fun cyclesBetweenSupportedModes() {
        assertEquals(KeyboardLayoutMode.LAYOUT_68, KeyboardLayoutMode.LAYOUT_60.next())
        assertEquals(KeyboardLayoutMode.LAYOUT_60, KeyboardLayoutMode.LAYOUT_68.next())
    }
}
