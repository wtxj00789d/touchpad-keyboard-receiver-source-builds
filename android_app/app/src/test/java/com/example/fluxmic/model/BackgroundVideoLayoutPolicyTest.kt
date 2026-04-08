package com.example.fluxmic.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundVideoLayoutPolicyTest {
    @Test
    fun requestsRelayoutWhenVideoSizeBecomesValidForTheFirstTime() {
        val result = BackgroundVideoLayoutPolicy.reduce(
            previousState = BackgroundVideoLayoutState(),
            videoWidth = 1920,
            videoHeight = 1080,
            firstFrameRendered = false
        )

        assertTrue(result.shouldRequestLayout)
        assertTrue(result.state.hasValidVideoSize)
    }

    @Test
    fun requestsRelayoutWhenFirstFrameArrivesAfterValidVideoSize() {
        val result = BackgroundVideoLayoutPolicy.reduce(
            previousState = BackgroundVideoLayoutState(hasValidVideoSize = true),
            videoWidth = 1920,
            videoHeight = 1080,
            firstFrameRendered = true
        )

        assertTrue(result.shouldRequestLayout)
        assertTrue(result.state.firstFrameRendered)
    }

    @Test
    fun ignoresInvalidVideoSizeUntilMetadataIsAvailable() {
        val result = BackgroundVideoLayoutPolicy.reduce(
            previousState = BackgroundVideoLayoutState(),
            videoWidth = 0,
            videoHeight = 0,
            firstFrameRendered = false
        )

        assertFalse(result.shouldRequestLayout)
        assertFalse(result.state.hasValidVideoSize)
    }
}
