package com.example.fluxmic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.fluxmic.model.GlassSliderStyle
import com.example.fluxmic.model.GlassToneMode
import kotlin.math.max

@Composable
fun GlassSlider(
    value: Float,
    toneMode: GlassToneMode,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    var engaged by remember { mutableStateOf(false) }

    val style = remember(toneMode, engaged, enabled) {
        GlassSliderStyle.resolve(
            toneMode = toneMode,
            engaged = engaged,
            enabled = enabled
        )
    }
    val trackTopAlpha by animateFloatAsState(
        targetValue = style.trackTopAlpha,
        animationSpec = tween(durationMillis = 140),
        label = "glass-slider-track-top"
    )
    val trackBottomAlpha by animateFloatAsState(
        targetValue = style.trackBottomAlpha,
        animationSpec = tween(durationMillis = 140),
        label = "glass-slider-track-bottom"
    )
    val trackBorderAlpha by animateFloatAsState(
        targetValue = style.trackBorderAlpha,
        animationSpec = tween(durationMillis = 140),
        label = "glass-slider-track-border"
    )
    val fillTopAlpha by animateFloatAsState(
        targetValue = style.fillTopAlpha,
        animationSpec = tween(durationMillis = 110),
        label = "glass-slider-fill-top"
    )
    val fillBottomAlpha by animateFloatAsState(
        targetValue = style.fillBottomAlpha,
        animationSpec = tween(durationMillis = 110),
        label = "glass-slider-fill-bottom"
    )
    val fillGlowAlpha by animateFloatAsState(
        targetValue = style.fillGlowAlpha,
        animationSpec = tween(durationMillis = 110),
        label = "glass-slider-fill-glow"
    )
    val thumbFillAlpha by animateFloatAsState(
        targetValue = style.thumbFillAlpha,
        animationSpec = tween(durationMillis = 110),
        label = "glass-slider-thumb-fill"
    )
    val thumbBorderAlpha by animateFloatAsState(
        targetValue = style.thumbBorderAlpha,
        animationSpec = tween(durationMillis = 110),
        label = "glass-slider-thumb-border"
    )
    val thumbGlowAlpha by animateFloatAsState(
        targetValue = style.thumbGlowAlpha,
        animationSpec = tween(durationMillis = 110),
        label = "glass-slider-thumb-glow"
    )
    val thumbScale by animateFloatAsState(
        targetValue = style.thumbScale,
        animationSpec = tween(durationMillis = 90),
        label = "glass-slider-thumb-scale"
    )
    val strokeWidthPx = with(LocalDensity.current) { 1.dp.toPx() }
    val clampedValue = value.coerceIn(0f, 1f)

    fun updateValueFromX(x: Float) {
        if (!enabled) return
        val nextValue = (x / widthPx).coerceIn(0f, 1f)
        onValueChange(nextValue)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .onGloballyPositioned { coordinates ->
                widthPx = max(coordinates.size.width.toFloat(), 1f)
            }
            .pointerInput(enabled, widthPx) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        engaged = true
                        updateValueFromX(offset.x)
                        tryAwaitRelease()
                        engaged = false
                    }
                )
            }
            .pointerInput(enabled, widthPx) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        engaged = true
                        updateValueFromX(offset.x)
                    },
                    onDragEnd = {
                        engaged = false
                    },
                    onDragCancel = {
                        engaged = false
                    },
                    onDrag = { change, _ ->
                        updateValueFromX(change.position.x)
                        change.consume()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = size.height * 0.34f
            val trackTop = (size.height - trackHeight) / 2f
            val trackCorner = trackHeight / 2f
            val thumbRadius = size.height * 0.21f * thumbScale
            val thumbTravel = (size.width - thumbRadius * 2f).coerceAtLeast(0f)
            val thumbCenterX = thumbRadius + (thumbTravel * clampedValue)
            val fillWidth = thumbCenterX.coerceAtLeast(trackHeight)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = trackTopAlpha),
                        Color.White.copy(alpha = trackBottomAlpha)
                    )
                ),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackCorner, trackCorner)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = trackBorderAlpha),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackCorner, trackCorner),
                style = Stroke(width = strokeWidthPx)
            )

            if (fillGlowAlpha > 0.001f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = fillGlowAlpha * 0.26f),
                    topLeft = Offset(0f, trackTop - strokeWidthPx * 0.5f),
                    size = Size(fillWidth, trackHeight + strokeWidthPx),
                    cornerRadius = CornerRadius(trackCorner + strokeWidthPx, trackCorner + strokeWidthPx)
                )
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = fillTopAlpha),
                        Color(0xFFDDEBFF).copy(alpha = fillBottomAlpha)
                    )
                ),
                topLeft = Offset(0f, trackTop),
                size = Size(fillWidth, trackHeight),
                cornerRadius = CornerRadius(trackCorner, trackCorner)
            )

            if (thumbGlowAlpha > 0.001f) {
                drawCircle(
                    color = Color.White.copy(alpha = thumbGlowAlpha * 0.22f),
                    radius = thumbRadius + strokeWidthPx * 2.2f,
                    center = Offset(thumbCenterX, size.height / 2f)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = thumbFillAlpha),
                        Color(0xFFDDEBFF).copy(alpha = thumbFillAlpha * 0.72f)
                    ),
                    center = Offset(thumbCenterX, size.height / 2f),
                    radius = thumbRadius * 1.15f
                ),
                radius = thumbRadius,
                center = Offset(thumbCenterX, size.height / 2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = thumbBorderAlpha),
                radius = thumbRadius,
                center = Offset(thumbCenterX, size.height / 2f),
                style = Stroke(width = strokeWidthPx)
            )
        }
    }
}
