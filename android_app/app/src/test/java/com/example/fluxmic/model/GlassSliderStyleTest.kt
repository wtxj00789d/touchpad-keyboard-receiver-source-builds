package com.example.fluxmic.model

import org.junit.Assert.assertTrue
import org.junit.Test

class GlassSliderStyleTest {
    @Test
    fun lightModeTrackAndThumbStayBrighterThanDarkMode() {
        val light = GlassSliderStyle.resolve(
            toneMode = GlassToneMode.LIGHT,
            engaged = false,
            enabled = true
        )
        val dark = GlassSliderStyle.resolve(
            toneMode = GlassToneMode.DARK,
            engaged = false,
            enabled = true
        )

        assertTrue(light.trackTopAlpha > dark.trackTopAlpha)
        assertTrue(light.thumbFillAlpha > dark.thumbFillAlpha)
    }

    @Test
    fun fillSegmentStaysStrongerThanBaseTrack() {
        val style = GlassSliderStyle.resolve(
            toneMode = GlassToneMode.NORMAL,
            engaged = false,
            enabled = true
        )

        assertTrue(style.fillTopAlpha > style.trackTopAlpha)
        assertTrue(style.fillBottomAlpha > style.trackBottomAlpha)
    }

    @Test
    fun engagedStateBoostsThumbScaleAndGlow() {
        val idle = GlassSliderStyle.resolve(
            toneMode = GlassToneMode.NORMAL,
            engaged = false,
            enabled = true
        )
        val engaged = GlassSliderStyle.resolve(
            toneMode = GlassToneMode.NORMAL,
            engaged = true,
            enabled = true
        )

        assertTrue(engaged.thumbScale > idle.thumbScale)
        assertTrue(engaged.thumbGlowAlpha > idle.thumbGlowAlpha)
    }
}
