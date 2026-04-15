package com.example.fluxmic.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fluxmic.model.BorderlessThemeTokens
import com.example.fluxmic.model.GlassThemeVariant
import com.example.fluxmic.model.GlassToneMode

@Composable
fun IndicatorDot(
    on: Boolean,
    toneMode: GlassToneMode,
    themeVariant: GlassThemeVariant,
    modifier: Modifier = Modifier
) {
    val dotStyle = remember(on, toneMode, themeVariant) {
        when (themeVariant) {
            GlassThemeVariant.GLASS -> {
                if (on) {
                    BorderlessThemeTokens.indicatorDot(toneMode, on = true)
                } else {
                    BorderlessThemeTokens.indicatorDot(toneMode, on = false)
                }
            }
            GlassThemeVariant.BORDERLESS -> BorderlessThemeTokens.indicatorDot(toneMode, on = on)
        }
    }

    val fillAlpha = animateFloatAsState(
        targetValue = dotStyle.fillAlpha,
        animationSpec = tween(durationMillis = 140, easing = LinearOutSlowInEasing),
        label = "indicator-dot-fill"
    )
    val borderAlpha = animateFloatAsState(
        targetValue = dotStyle.borderAlpha,
        animationSpec = tween(durationMillis = 140, easing = LinearOutSlowInEasing),
        label = "indicator-dot-border"
    )

    if (fillAlpha.value <= 0.001f && borderAlpha.value <= 0.001f) {
        return
    }

    Box(
        modifier = modifier
            .size(7.dp)
            .background(
                color = Color.White.copy(alpha = fillAlpha.value),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = borderAlpha.value),
                shape = CircleShape
            )
    )
}
