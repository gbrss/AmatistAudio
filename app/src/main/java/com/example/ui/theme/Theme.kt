package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmatistColorScheme = darkColorScheme(
    primary = AmethystPrimary,
    onPrimary = Color.Black,
    primaryContainer = AmethystPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = AmethystSecondary,
    onSecondary = Color.Black,
    tertiary = AmethystTertiary,
    background = AmethystDarkBg,
    onBackground = TextPrimary,
    surface = AmethystDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = AmethystDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AmatistColorScheme,
        typography = Typography,
        content = content
    )
}
