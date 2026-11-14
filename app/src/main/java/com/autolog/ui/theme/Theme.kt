package com.autolog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0F1419)
val Surface = Color(0xFF1A2332)
val SurfaceVariant = Color(0xFF243044)
val Primary = Color(0xFF4FC3F7)
val Secondary = Color(0xFF80CBC4)
val Warning = Color(0xFFFFA726)
val Success = Color(0xFF66BB6A)
val Danger = Color(0xFFEF5350)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0BEC5)

private val DarkColors = darkColorScheme(
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    primary = Primary,
    secondary = Secondary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onPrimary = Color(0xFF0D1117),
    onSecondary = Color(0xFF0D1117),
)

@Composable
fun AutoLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
