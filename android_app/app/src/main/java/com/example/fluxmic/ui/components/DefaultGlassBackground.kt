package com.example.fluxmic.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.fluxmic.R
import com.example.fluxmic.model.DefaultGlassBackgroundSpecResolver
import com.example.fluxmic.model.GlassToneMode

@Composable
fun DefaultGlassBackground(
    toneMode: GlassToneMode,
    hasCustomMedia: Boolean,
    modifier: Modifier = Modifier
) {
    val spec = remember(toneMode, hasCustomMedia) {
        DefaultGlassBackgroundSpecResolver.resolve(
            toneMode = toneMode,
            hasCustomMedia = hasCustomMedia
        )
    }

    Box(modifier = modifier) {
        if (spec.showBundledAtmosphere) {
            Image(
                painter = painterResource(id = R.drawable.default_glass_atmosphere),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        val tint = Color(spec.toneFilter.tintColorArgb)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tint.copy(alpha = spec.toneFilter.scrimAlpha))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = spec.toneFilter.atmosphereVeilAlpha),
                            Color.Transparent,
                            Color.Black.copy(alpha = spec.toneFilter.vignetteAlpha)
                        )
                    )
                )
        )
    }
}
