package com.ngongole.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F1C5),
    onPrimaryContainer = Color(0xFF00210D),
    secondary = Color(0xFFB5551D),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD5AA),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF005228),
    onPrimaryContainer = Color(0xFFB7F1C5),
    secondary = Color(0xFFFFB68F),
    error = Color(0xFFFFB4AB)
)

@Composable
fun NgongoleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
