package com.lddev.scalefinder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary, secondary = DarkSecondary, tertiary = DarkTertiary,
        background = DarkBackground, surface = DarkSurface, surfaceVariant = DarkSurfaceVariant,
        onPrimary = DarkOnPrimary, onSecondary = DarkOnSecondary, onTertiary = DarkOnTertiary,
        onBackground = DarkOnBackground, onSurface = DarkOnSurface, onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline, error = DarkError, onError = DarkOnError,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = LightPrimary, secondary = LightSecondary, tertiary = LightTertiary,
        background = LightBackground, surface = LightSurface, surfaceVariant = LightSurfaceVariant,
        onPrimary = LightOnPrimary, onSecondary = LightOnSecondary, onTertiary = LightOnTertiary,
        onBackground = LightOnBackground, onSurface = LightOnSurface, onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline, error = LightError, onError = LightOnError,
    )

@Composable
fun ScaleFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme, typography = Typography, content = content)
}
