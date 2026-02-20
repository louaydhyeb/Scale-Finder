package com.lddev.scalefinder.ui.components.home_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Tuning
import com.lddev.scalefinder.ui.HomeViewModel
import com.lddev.scalefinder.ui.ProgressionPreset
import kotlinx.coroutines.delay

@Composable
fun PresetsBar(vm: HomeViewModel) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            icon = Icons.Default.Star,
            title = stringResource(R.string.quick_presets),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val presets = listOf(
            Triple(stringResource(R.string.preset_pop_progression), stringResource(R.string.preset_pop_description)) { vm.applyProgressionPreset(ProgressionPreset.POP) },
            Triple(stringResource(R.string.preset_jazz_progression), stringResource(R.string.preset_jazz_description)) { vm.applyProgressionPreset(ProgressionPreset.JAZZ) },
            Triple(stringResource(R.string.preset_blues), stringResource(R.string.preset_blues_description)) { vm.applyProgressionPreset(ProgressionPreset.BLUES) },
            Triple(stringResource(R.string.preset_standard), stringResource(R.string.preset_standard_description)) { vm.setTuning(Tuning.STANDARD) },
            Triple(stringResource(R.string.preset_drop_d), stringResource(R.string.preset_drop_d_description)) { vm.setTuning(Tuning.DROP_D) },
            Triple(stringResource(R.string.preset_frets_0_12), stringResource(R.string.preset_frets_0_12_description)) { vm.applyFretPreset(0, 12) },
            Triple(stringResource(R.string.preset_box_5), stringResource(R.string.preset_box_5_description)) { vm.applyFretPreset(5, 7) }
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(presets) { index, preset ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(
                            durationMillis = 300,
                            delayMillis = index * 50,
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 300,
                            delayMillis = index * 50
                        )
                    )
                ) {
                    PresetChip(
                        label = preset.first,
                        description = preset.second,
                        onClick = preset.third
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "preset_scale"
    )

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(150)
            pressed = false
        }
    }

    Card(
        onClick = {
            pressed = true
            onClick()
        },
        modifier = Modifier
            .height(70.dp)
            .width(110.dp)
            .scale(scale.value),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
