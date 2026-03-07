package com.example.fluxmic.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FluxDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF29A19C),
    onPrimary = Color(0xFF0C141A),
    secondary = Color(0xFFF7B32B),
    background = Color(0xFF101820),
    surface = Color(0xFF172A3A),
    onBackground = Color(0xFFF8F9FA),
    onSurface = Color(0xFFF8F9FA)
)

@Composable
fun FluxMicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FluxDarkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
