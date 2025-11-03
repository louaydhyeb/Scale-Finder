package com.lddev.scalefinder.ui.theme

import androidx.compose.ui.graphics.Color

// Modern, coherent color palette for music app
// Light mode - warm, inviting tones
val WoodLightPrimary = Color(0xFF6366F1) // Indigo - modern, musical
val WoodLightSecondary = Color(0xFF8B5CF6) // Purple - creative accent
val WoodLightTertiary = Color(0xFFEC4899) // Pink - vibrant accent
val WoodLightBackground = Color(0xFFFEFEFE) // Pure white with slight warmth
val WoodLightSurface = Color(0xFFF8F9FA) // Light gray surface
val WoodLightOnPrimary = Color(0xFFFFFFFF)
val WoodLightOnSecondary = Color(0xFFFFFFFF)
val WoodLightOnTertiary = Color(0xFFFFFFFF)
val WoodLightOnBackground = Color(0xFF1F2937) // Dark gray text
val WoodLightOnSurface = Color(0xFF1F2937) // Dark gray text

// Dark mode - sophisticated, easy on eyes
val WoodDarkPrimary = Color(0xFF818CF8) // Lighter indigo for dark mode
val WoodDarkSecondary = Color(0xFFA78BFA) // Lighter purple
val WoodDarkTertiary = Color(0xFFF472B6) // Lighter pink
val WoodDarkBackground = Color(0xFF0F172A) // Deep slate blue
val WoodDarkSurface = Color(0xFF1E293B) // Slate surface
val WoodDarkOnPrimary = Color(0xFF0F172A) // Dark text on light primary
val WoodDarkOnSecondary = Color(0xFF0F172A)
val WoodDarkOnTertiary = Color(0xFF0F172A)
val WoodDarkOnBackground = Color(0xFFF1F5F9) // Light text
val WoodDarkOnSurface = Color(0xFFF1F5F9) // Light text

// Keep legacy names to satisfy Theme.kt references
val Purple80 = WoodLightPrimary
val PurpleGrey80 = WoodLightSecondary
val Pink80 = WoodLightTertiary

val Purple40 = WoodDarkPrimary
val PurpleGrey40 = WoodDarkSecondary
val Pink40 = WoodDarkTertiary