package com.lddev.scalefinder.ui.components.home_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.ChordQuality
import com.lddev.scalefinder.model.Note
import kotlinx.coroutines.delay

@Composable
fun ProgressionEditor(
    progression: MutableList<Chord>,
    onAdd: (Chord) -> Unit,
    onRemove: (Int) -> Unit,
    onMoveLeft: (Int) -> Unit,
    onMoveRight: (Int) -> Unit,
    onSelect: (Int) -> Unit,
    onPlayArpeggio: (Int) -> Unit,
    isPlaying: Boolean,
    currentPlayingIndex: Int,
    progressionBPM: Int,
    loopEnabled: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onBPMChange: (Int) -> Unit,
    onToggleLoop: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(icon = Icons.Default.Star, title = stringResource(R.string.chord_progression))

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChordPicker(onAdd)
        }
        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible = progression.size >= 2) {
            ProgressionPlaybackBar(
                isPlaying = isPlaying,
                bpm = progressionBPM,
                loopEnabled = loopEnabled,
                onPlay = onPlay,
                onStop = onStop,
                onBPMChange = onBPMChange,
                onToggleLoop = onToggleLoop,
            )
        }

        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(
                items = progression,
                key = { index, chord -> "${chord.root}${chord.quality}$index" },
            ) { idx, chord ->
                ChordCard(
                    chord = chord,
                    index = idx,
                    isCurrentlyPlaying = isPlaying && idx == currentPlayingIndex,
                    onMoveLeft = onMoveLeft,
                    onMoveRight = onMoveRight,
                    onSelect = onSelect,
                    onPlayArpeggio = onPlayArpeggio,
                    onRemove = onRemove,
                )
            }
        }
    }
}

@Composable
private fun ChordCard(
    chord: Chord,
    index: Int,
    isCurrentlyPlaying: Boolean,
    onMoveLeft: (Int) -> Unit,
    onMoveRight: (Int) -> Unit,
    onSelect: (Int) -> Unit,
    onPlayArpeggio: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    var cardScale by remember { mutableFloatStateOf(0.8f) }
    val scaleAnimation =
        animateFloatAsState(
            targetValue = cardScale,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "card_scale",
        )

    LaunchedEffect(chord) {
        delay(50)
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (isCurrentlyPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "glow",
    )

    val moveLeftDesc = stringResource(R.string.move_left)
    val selectChordDesc = stringResource(R.string.select_chord)
    val playArpeggioDesc = stringResource(R.string.play_arpeggio)
    val moveRightDesc = stringResource(R.string.move_right)
    val removeChordDesc = stringResource(R.string.remove_chord)

    val highlightBorder =
        if (glowAlpha > 0f) {
            Modifier.border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                shape = MaterialTheme.shapes.medium,
            )
        } else {
            Modifier
        }

    OutlinedCard(
        modifier =
            Modifier
                .scale(scaleAnimation.value)
                .then(highlightBorder),
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = chord.toString(),
                style = MaterialTheme.typography.titleSmall,
                color =
                    if (isCurrentlyPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onMoveLeft(index) },
                    modifier = Modifier.semantics { contentDescription = moveLeftDesc },
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = moveLeftDesc)
                }
                IconButton(
                    onClick = { onSelect(index) },
                    modifier = Modifier.semantics { contentDescription = selectChordDesc },
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = selectChordDesc, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = { onPlayArpeggio(index) },
                    modifier = Modifier.semantics { contentDescription = playArpeggioDesc },
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = playArpeggioDesc, tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(
                    onClick = { onMoveRight(index) },
                    modifier = Modifier.semantics { contentDescription = moveRightDesc },
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = moveRightDesc)
                }
                IconButton(
                    onClick = { onRemove(index) },
                    modifier = Modifier.semantics { contentDescription = removeChordDesc },
                ) {
                    Icon(Icons.Default.Delete, contentDescription = removeChordDesc, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ProgressionPlaybackBar(
    isPlaying: Boolean,
    bpm: Int,
    loopEnabled: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onBPMChange: (Int) -> Unit,
    onToggleLoop: () -> Unit,
) {
    val playDesc = stringResource(R.string.content_play_progression)
    val stopDesc = stringResource(R.string.content_stop_progression)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { if (isPlaying) onStop() else onPlay() },
                modifier =
                    Modifier.semantics {
                        contentDescription = if (isPlaying) stopDesc else playDesc
                    },
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Clear else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isPlaying) {
                        stringResource(R.string.stop_progression)
                    } else {
                        stringResource(R.string.play_progression)
                    },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onBPMChange(bpm - 5) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.decrease))
                }
                Text(
                    text = "$bpm",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = { onBPMChange(bpm + 5) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.increase))
                }
                Text(
                    stringResource(R.string.bpm),
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(onClick = onToggleLoop) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).rotate(if (loopEnabled) 0f else 0f),
                    tint =
                        if (loopEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        },
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.loop_progression),
                    color =
                        if (loopEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChordPicker(onAdd: (Chord) -> Unit) {
    var expandedRoot by remember { mutableStateOf(false) }
    var expandedQuality by remember { mutableStateOf(false) }
    var root by remember { mutableStateOf(Note.C) }
    var quality by remember { mutableStateOf(ChordQuality.MAJOR) }
    val rootLabel = stringResource(R.string.root)
    val rootSelectorDesc = stringResource(R.string.content_root_selector)

    ExposedDropdownMenuBox(expanded = expandedRoot, onExpandedChange = { expandedRoot = !expandedRoot }) {
        OutlinedTextField(
            value = root.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(rootLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedRoot) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).semantics { contentDescription = rootSelectorDesc },
        )
        DropdownMenu(expanded = expandedRoot, onDismissRequest = { expandedRoot = false }) {
            Note.entries.forEach { n ->
                DropdownMenuItem(text = { Text(n.toString()) }, onClick = {
                    root = n
                    expandedRoot = false
                })
            }
        }
    }

    val qualityLabel = stringResource(R.string.quality)
    val qualitySelectorDesc = stringResource(R.string.content_quality_selector)

    ExposedDropdownMenuBox(expanded = expandedQuality, onExpandedChange = { expandedQuality = !expandedQuality }) {
        OutlinedTextField(
            value = quality.display,
            onValueChange = {},
            readOnly = true,
            label = { Text(qualityLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedQuality) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).semantics { contentDescription = qualitySelectorDesc },
        )
        DropdownMenu(expanded = expandedQuality, onDismissRequest = { expandedQuality = false }) {
            ChordQuality.entries.forEach { q ->
                DropdownMenuItem(text = { Text(q.display) }, onClick = {
                    quality = q
                    expandedQuality = false
                })
            }
        }
    }

    var addButtonPressed by remember { mutableStateOf(false) }
    val addButtonScale =
        animateFloatAsState(
            targetValue = if (addButtonPressed) 0.95f else 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "add_button_scale",
        )

    LaunchedEffect(addButtonPressed) {
        if (addButtonPressed) {
            delay(150)
            addButtonPressed = false
        }
    }

    val addChordDesc = stringResource(R.string.content_add_chord)
    Button(
        onClick = {
            addButtonPressed = true
            onAdd(Chord(root, quality))
        },
        modifier =
            Modifier
                .height(48.dp)
                .scale(addButtonScale.value)
                .semantics { contentDescription = addChordDesc },
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.add_chord))
    }
}
