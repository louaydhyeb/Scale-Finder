package com.lddev.scalefinder.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.lddev.scalefinder.ui.theme.DarkPrimary
import com.lddev.scalefinder.ui.theme.DarkTertiary
import com.lddev.scalefinder.ui.theme.LightPrimary
import com.lddev.scalefinder.ui.theme.LightTertiary

@Composable
fun SplashScreen(
    darkTheme: Boolean = false,
    onSplashComplete: () -> Unit,
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim =
        animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec =
                tween(
                    durationMillis = 1000,
                    delayMillis = 300,
                ),
            label = "splash_alpha",
        )

    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    LaunchedEffect(alphaAnim.value) {
        if (alphaAnim.value == 1f) {
            // Wait a bit before transitioning
            kotlinx.coroutines.delay(1500)
            onSplashComplete()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.horizontalGradient(
                            colors =
                                if (darkTheme) {
                                    listOf(
                                        MaterialTheme.colorScheme.background,
                                        MaterialTheme.colorScheme.surface,
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.background,
                                        MaterialTheme.colorScheme.surface,
                                    )
                                },
                        ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text =
                buildAnnotatedString {
                    withStyle(
                        style =
                            SpanStyle(
                                brush =
                                    Brush.horizontalGradient(
                                        colors =
                                            if (darkTheme) {
                                                listOf(DarkPrimary, DarkTertiary)
                                            } else {
                                                listOf(LightPrimary, LightTertiary)
                                            },
                                    ),
                                fontWeight = FontWeight.Bold,
                            ),
                    ) {
                        append("Scale")
                    }
                    append(" ")
                    withStyle(
                        style =
                            SpanStyle(
                                color =
                                    if (darkTheme) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                fontWeight = FontWeight.Bold,
                            ),
                    ) {
                        append("Finder")
                    }
                },
            style =
                TextStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                ),
            modifier = Modifier.alpha(alphaAnim.value),
        )
    }
}
