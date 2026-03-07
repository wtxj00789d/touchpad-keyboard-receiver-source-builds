package com.example.fluxmic.touchpad.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TouchpadColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF56D6C0),
    onPrimary = Color(0xFF072018),
    secondary = Color(0xFF94C9FF),
    background = Color(0xFF0E1825),
    surface = Color(0xFF1A2737),
    onBackground = Color(0xFFEDF3FA),
    onSurface = Color(0xFFEDF3FA)
)

@Composable
fun TouchpadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TouchpadColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
