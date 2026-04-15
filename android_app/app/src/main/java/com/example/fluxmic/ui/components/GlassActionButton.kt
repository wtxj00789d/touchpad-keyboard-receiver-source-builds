package com.example.fluxmic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fluxmic.model.GlassActionButtonStyle
import com.example.fluxmic.model.GlassControlTextTreatment
import com.example.fluxmic.model.GlassThemeVariant
import com.example.fluxmic.model.GlassToneMode

@Composable
fun GlassActionButton(
    text: String,
    onClick: () -> Unit,
    toneMode: GlassToneMode,
    modifier: Modifier = Modifier,
    themeVariant: GlassThemeVariant = GlassThemeVariant.GLASS,
    active: Boolean = false,
    enabled: Boolean = true,
    editing: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    cornerRadius: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val style = remember(toneMode, themeVariant, active, pressed, enabled, editing) {
        GlassActionButtonStyle.resolve(
            toneMode = toneMode,
            themeVariant = themeVariant,
            active = active,
            pressed = pressed,
            enabled = enabled,
            editing = editing
        )
    }

    val scale by animateFloatAsState(
        targetValue = style.scale,
        animationSpec = tween(durationMillis = 90),
        label = "glass-action-scale"
    )
    val fillTopAlpha by animateFloatAsState(
        targetValue = style.fillTopAlpha,
        animationSpec = tween(durationMillis = 120),
        label = "glass-action-fill-top"
    )
    val fillBottomAlpha by animateFloatAsState(
        targetValue = style.fillBottomAlpha,
        animationSpec = tween(durationMillis = 120),
        label = "glass-action-fill-bottom"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = style.borderAlpha,
        animationSpec = tween(durationMillis = 100),
        label = "glass-action-border"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = style.glowAlpha,
        animationSpec = tween(durationMillis = 110),
        label = "glass-action-glow"
    )
    val shape = RoundedCornerShape(cornerRadius)
    val cornerRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { cornerRadius.toPx() }

    val textColor = when (style.textTreatment) {
        GlassControlTextTreatment.DARK -> Color(0xFF1A2534).copy(alpha = style.contentAlpha)
        GlassControlTextTreatment.LIGHT_WITH_SHADOW -> Color.White.copy(alpha = 0.96f * style.contentAlpha)
        GlassControlTextTreatment.LIGHT_PLAIN -> Color.White.copy(alpha = 0.90f * style.contentAlpha)
    }
    val textShadow = when (style.textTreatment) {
        GlassControlTextTreatment.LIGHT_WITH_SHADOW -> Shadow(
            color = Color.Black.copy(alpha = 0.76f * style.contentAlpha),
            offset = Offset(0f, 1.2f),
            blurRadius = 2.8f
        )
        else -> null
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (glowAlpha > 0.001f) {
                    val glowStroke = 2.dp.toPx()
                    drawRoundRect(
                        color = Color.White.copy(alpha = glowAlpha * 0.34f),
                        topLeft = Offset(-glowStroke * 0.5f, -glowStroke * 0.5f),
                        size = Size(width = size.width + glowStroke, height = size.height + glowStroke),
                        cornerRadius = CornerRadius(cornerRadiusPx + glowStroke, cornerRadiusPx + glowStroke),
                        style = Stroke(width = glowStroke)
                    )
                }
            }
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = fillTopAlpha),
                        Color.White.copy(alpha = fillBottomAlpha)
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = borderAlpha),
                shape = shape
            )
            .defaultMinSize(minHeight = 40.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = textStyle.copy(color = textColor, shadow = textShadow)
        )
        if (style.showIndicatorDot) {
            IndicatorDot(
                on = true,
                toneMode = toneMode,
                themeVariant = themeVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
            )
        }
    }
}
