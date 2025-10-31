package com.lddev.scalefinder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = WoodDarkBackground,
    surface = WoodDarkSurface,
    onPrimary = WoodDarkOnPrimary,
    onSecondary = WoodDarkOnSecondary,
    onTertiary = WoodDarkOnTertiary,
    onBackground = WoodDarkOnBackground,
    onSurface = WoodDarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = WoodLightBackground,
    surface = WoodLightSurface,
    onPrimary = WoodLightOnPrimary,
    onSecondary = WoodLightOnSecondary,
    onTertiary = WoodLightOnTertiary,
    onBackground = WoodLightOnBackground,
    onSurface = WoodLightOnSurface
)

@Composable
fun ScaleFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Use consistent wood palette across devices
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}