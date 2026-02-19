package com.lddev.scalefinder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun MetronomeControls(
    bpm: Int,
    timeSignature: Int,
    currentBeat: Int,
    isRunning: Boolean,
    onBPMChanged: (Int) -> Unit,
    onTimeSignatureChanged: (Int) -> Unit,
    onToggle: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val metronomePauseDesc = stringResource(R.string.metronome_pause)
    val metronomeStartDesc = stringResource(R.string.metronome_start)
    val metronomePauseLabel = stringResource(R.string.metronome_pause_label)
    val metronomePlayLabel = stringResource(R.string.metronome_play_label)
    val metronomeCollapseDesc = stringResource(R.string.metronome_collapse)
    val metronomeExpandDesc = stringResource(R.string.metronome_expand)
    val metronomeCollapseLabel = stringResource(R.string.metronome_collapse_label)
    val metronomeExpandLabel = stringResource(R.string.metronome_expand_label)

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            MetronomeHeader(
                bpm = bpm,
                timeSignature = timeSignature,
                isRunning = isRunning,
                isExpanded = isExpanded,
                onToggle = onToggle,
                onExpandToggle = { isExpanded = !isExpanded },
                pauseDesc = metronomePauseDesc,
                startDesc = metronomeStartDesc,
                pauseLabel = metronomePauseLabel,
                playLabel = metronomePlayLabel,
                collapseDesc = metronomeCollapseDesc,
                expandDesc = metronomeExpandDesc,
                collapseLabel = metronomeCollapseLabel,
                expandLabel = metronomeExpandLabel
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))

                    BeatIndicators(
                        timeSignature = timeSignature,
                        currentBeat = currentBeat,
                        isRunning = isRunning
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.bpm), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Stepper(label = "", value = bpm, onChange = onBPMChanged)
                        }

                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.time_signature), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Stepper(label = "", value = timeSignature, onChange = onTimeSignatureChanged)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetronomeHeader(
    bpm: Int,
    timeSignature: Int,
    isRunning: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onExpandToggle: () -> Unit,
    pauseDesc: String,
    startDesc: String,
    pauseLabel: String,
    playLabel: String,
    collapseDesc: String,
    expandDesc: String,
    collapseLabel: String,
    expandLabel: String
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.semantics { contentDescription = if (isRunning) pauseDesc else startDesc }
            ) {
                Icon(
                    if (isRunning) Icons.Default.Clear else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) pauseLabel else playLabel,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    "$bpm BPM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$timeSignature/4",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        IconButton(
            onClick = onExpandToggle,
            modifier = Modifier.semantics { contentDescription = if (isExpanded) collapseDesc else expandDesc }
        ) {
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) collapseLabel else expandLabel,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BeatIndicators(
    timeSignature: Int,
    currentBeat: Int,
    isRunning: Boolean
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..timeSignature).forEach { beat ->
            val isActive = currentBeat == beat && isRunning
            val scale = animateFloatAsState(
                targetValue = if (isActive) 1.3f else 1.0f,
                animationSpec = tween(durationMillis = 100),
                label = "beat_scale"
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .scale(scale.value),
                contentAlignment = Alignment.Center
            ) {
                val color = if (beat == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                val backgroundColor = if (isActive) color else color.copy(alpha = 0.2f)

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(backgroundColor, CircleShape)
                        .border(
                            2.dp,
                            if (isActive) color else color.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = beat.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimary else color
                            )
                        )
                    }
                }
            }
            if (beat < timeSignature) Spacer(Modifier.size(8.dp))
        }
    }
}
