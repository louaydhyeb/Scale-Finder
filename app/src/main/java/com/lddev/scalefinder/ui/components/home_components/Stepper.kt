package com.lddev.scalefinder.ui.components.home_components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.R
import kotlinx.coroutines.delay

@Composable
fun Stepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    var decrementPressed by remember { mutableStateOf(false) }
    var incrementPressed by remember { mutableStateOf(false) }

    val decrementScale =
        animateFloatAsState(
            targetValue = if (decrementPressed) 0.8f else 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "decrement_scale",
        )

    val incrementScale =
        animateFloatAsState(
            targetValue = if (incrementPressed) 0.8f else 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "increment_scale",
        )

    LaunchedEffect(decrementPressed) {
        if (decrementPressed) {
            delay(150)
            decrementPressed = false
        }
    }

    LaunchedEffect(incrementPressed) {
        if (incrementPressed) {
            delay(150)
            incrementPressed = false
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        IconButton(
            onClick = {
                decrementPressed = true
                onChange(value - 1)
            },
            modifier =
                Modifier
                    .scale(decrementScale.value)
                    .semantics { contentDescription = "$label decrement" },
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.decrease))
        }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(
            onClick = {
                incrementPressed = true
                onChange(value + 1)
            },
            modifier =
                Modifier
                    .scale(incrementScale.value)
                    .semantics { contentDescription = "$label increment" },
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.increase))
        }
    }
}
