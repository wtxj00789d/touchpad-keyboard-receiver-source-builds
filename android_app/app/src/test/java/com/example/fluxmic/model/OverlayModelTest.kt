package com.example.fluxmic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayModelTest {
    @Test
    fun showsWindowCommandsWhenAvailable() {
        val model = OverlayModel.fromState(
            activeWindow = "Visual Studio Code",
            windowControls = WindowControlState(
                available = true,
                canMinimize = true,
                canMaximize = true,
                canClose = true
            ),
            connected = true,
            muted = false,
            volume = 0.72f
        )

        assertEquals(listOf("minimize", "maximize", "close"), model.contextActions.map { it.id })
        assertEquals("Visual Studio Code", model.windowTitle)
        assertFalse(model.showVolumeCapsule)
    }

    @Test
    fun fallsBackToConnectionMuteAndVolume() {
        val model = OverlayModel.fromState(
            activeWindow = "",
            windowControls = WindowControlState(),
            connected = false,
            muted = true,
            volume = 0.35f
        )

        assertEquals(listOf("connection", "mute", "volume"), model.contextActions.map { it.id })
        assertEquals("Desktop", model.windowTitle)
        assertTrue(model.showVolumeCapsule)
        assertTrue(model.usingFallbackActions)
    }

    @Test
    fun usesRestoreLabelWhenWindowIsMaximized() {
        val model = OverlayModel.fromState(
            activeWindow = "Receiver",
            windowControls = WindowControlState(
                available = true,
                canMaximize = true,
                isMaximized = true
            ),
            connected = true,
            muted = false,
            volume = 1f
        )

        assertEquals("Restore", model.contextActions.single().title)
    }
}
