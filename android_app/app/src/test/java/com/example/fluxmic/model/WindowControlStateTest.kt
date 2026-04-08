package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowControlStateTest {
    @Test
    fun usesRestoreActionWhenWindowIsMaximized() {
        val state = WindowControlState(
            available = true,
            canMaximize = true,
            isMaximized = true
        )

        assertEquals("Restore", state.maximizeButtonLabel())
        assertEquals("window_restore", state.maximizeControlOp())
    }

    @Test
    fun reportsActionAvailabilityOnlyWhenButtonsAreUsable() {
        assertFalse(WindowControlState().hasAnyAction)
        assertTrue(WindowControlState(available = true, canClose = true).hasAnyAction)
    }
}
