package com.codx.controller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    secondary = AccentCyan,
    tertiary = AccentGreen,
    background = ControllerBackground,
    surface = ControllerSurface,
    surfaceVariant = ControllerSurfaceVariant,
    onPrimary = ControllerBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun CodXControllerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
