package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardBehaviorTest {
    @Test
    fun fnAndCapsTogglePersistently() {
        val fnState = KeyboardBehavior.toggleFn(KeyboardLayerState())
        val capsState = KeyboardBehavior.toggleCaps(fnState)

        assertTrue(fnState.fnLocked)
        assertTrue(capsState.capsLocked)
    }

    @Test
    fun remapsLabelsForShiftFnAndCaps() {
        assertEquals("!", KeyboardBehavior.remapLabel("1", "k_1", KeyboardLayerState(shiftPressed = true)))
        assertEquals("F1", KeyboardBehavior.remapLabel("1", "k_1", KeyboardLayerState(fnLocked = true)))
        assertEquals("Delete", KeyboardBehavior.remapLabel("Backspace", "k_backspace", KeyboardLayerState(fnLocked = true)))
        assertEquals("A", KeyboardBehavior.remapLabel("a", "k_a", KeyboardLayerState(capsLocked = true)))
    }

    @Test
    fun remapsFnOutputTokens() {
        val fnState = KeyboardLayerState(fnLocked = true)

        assertEquals("F12", KeyboardBehavior.resolveOutputToken("EQUALS", "k_equal", fnState))
        assertEquals("DELETE", KeyboardBehavior.resolveOutputToken("BACKSPACE", "k_backspace", fnState))
        assertEquals("PRINTSCREEN", KeyboardBehavior.resolveOutputToken("P", "k_p", fnState))
    }

    @Test
    fun shiftPressedTracksDownAndUpState() {
        val downState = KeyboardBehavior.onKeyDown(KeyboardLayerState(), "k_lshift", "LSHIFT").state
        val upState = KeyboardBehavior.onKeyUp(downState, "k_lshift", "LSHIFT")

        assertTrue(downState.shiftPressed)
        assertFalse(upState.shiftPressed)
    }

    @Test
    fun rejectsNinthSimultaneousKey() {
        val state = (1..8).fold(KeyboardLayerState()) { acc, i ->
            KeyboardBehavior.onKeyDown(acc, "k_$i", "KEY$i").state
        }

        assertFalse(KeyboardBehavior.onKeyDown(state, "k_9", "KEY9").accepted)
    }

    @Test
    fun repeatProfileMatchesKeyboardCadence() {
        val profile = KeyboardBehavior.defaultRepeatProfile()

        assertEquals(350L, profile.initialDelayMs)
        assertEquals(45L, profile.repeatIntervalMs)
    }
}
