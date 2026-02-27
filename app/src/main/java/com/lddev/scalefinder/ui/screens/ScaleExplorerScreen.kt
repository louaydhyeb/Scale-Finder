package com.lddev.scalefinder.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.ScaleType
import com.lddev.scalefinder.ui.ScaleExplorerViewModel
import com.lddev.scalefinder.ui.components.home_components.GuitarFretboard
import com.lddev.scalefinder.ui.components.home_components.SectionHeader
import com.lddev.scalefinder.ui.components.home_components.Stepper

@Composable
fun ScaleExplorerScreen(
    modifier: Modifier = Modifier,
    vm: ScaleExplorerViewModel = viewModel(),
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        // Title
        Text(
            text =
                buildAnnotatedString {
                    withStyle(
                        style =
                            SpanStyle(
                                brush =
                                    Brush.horizontalGradient(
                                        colors =
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary,
                                            ),
                                    ),
                                fontWeight = FontWeight.Bold,
                            ),
                    ) { append(stringResource(R.string.scale_explorer_title)) }
                },
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        )

        Spacer(Modifier.height(12.dp))

        // Root + Scale Type pickers
        ScalePickerRow(vm)

        Spacer(Modifier.height(12.dp))

        // Scale Info Card
        ScaleInfoCard(vm)

        Spacer(Modifier.height(12.dp))

        // Diatonic Chords
        DiatonicChordsSection(vm)

        Spacer(Modifier.height(12.dp))

        // Fret controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
        ) {
            Stepper(label = stringResource(R.string.start_fret), value = vm.fretStart, onChange = vm::updateFretStart)
            Stepper(label = stringResource(R.string.fret_count), value = vm.fretCount, onChange = vm::updateFretCount)
        }

        Spacer(Modifier.height(8.dp))

        // Fretboard
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.content_fretboard_options),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(stringResource(R.string.scale_explorer_fretboard), style = MaterialTheme.typography.titleMedium)
                }
                GuitarFretboard(
                    showNoteNames = vm.showNoteNames,
                    tuning = vm.selectedTuning,
                    scale = vm.scale,
                    fretStart = vm.fretStart,
                    fretCount = vm.fretCount,
                    highContrast = vm.highContrast,
                    invertStrings = vm.invertFretboard,
                    onNoteTapped = { stringIdx, fret, _ ->
                        val freq = vm.selectedTuning.getFrequency(stringIdx, fret)
                        vm.playFrequency(freq)
                    },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.tuning_label, vm.selectedTuning.name))
                    Text(stringResource(R.string.scale_label, vm.scale.toString()))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScalePickerRow(vm: ScaleExplorerViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Root picker
        var rootExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = rootExpanded, onExpandedChange = { rootExpanded = !rootExpanded }) {
            OutlinedTextField(
                value = vm.selectedRoot.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_root)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rootExpanded) },
                modifier =
                    Modifier
                        .width(120.dp)
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            DropdownMenu(expanded = rootExpanded, onDismissRequest = { rootExpanded = false }) {
                Note.entries.forEach { note ->
                    DropdownMenuItem(
                        text = { Text(note.toString(), fontWeight = if (note == vm.selectedRoot) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            vm.setRoot(note)
                            rootExpanded = false
                        },
                    )
                }
            }
        }

        // Scale type picker
        var typeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
            OutlinedTextField(
                value = vm.selectedScaleType.display,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_scale_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                modifier =
                    Modifier
                        .width(240.dp)
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                ScaleType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                type.display,
                                fontWeight = if (type == vm.selectedScaleType) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            vm.setScaleType(type)
                            typeExpanded = false
                        },
                    )
                }
            }
        }

        if (vm.isPlayingScale) {
            OutlinedButton(onClick = { vm.stopScale() }) {
                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.content_stop_scale), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.stop_progression))
            }
        } else {
            OutlinedButton(onClick = { vm.playScaleAscending() }) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.content_play_ascending),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.play_scale_ascending))
            }
            OutlinedButton(onClick = { vm.playScaleDescending() }) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.content_play_descending),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.play_scale_descending))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScaleInfoCard(vm: ScaleExplorerViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Scale notes
            SectionHeader(icon = Icons.Default.Star, title = stringResource(R.string.scale_notes))
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                vm.scaleNotes.forEachIndexed { index, note ->
                    NoteChip(
                        note = note.toString(),
                        isRoot = index == 0,
                        isPlaying = vm.currentPlayingNoteIndex == index,
                        onClick = { vm.playNoteAtIndex(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteChip(
    note: String,
    isRoot: Boolean,
    isPlaying: Boolean = false,
    onClick: () -> Unit = {},
) {
    val bgColor =
        when {
            isPlaying -> MaterialTheme.colorScheme.tertiary
            isRoot -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    val textColor =
        when {
            isPlaying -> MaterialTheme.colorScheme.onTertiary
            isRoot -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        }
    val animatedScale =
        animateFloatAsState(
            targetValue = if (isPlaying) 1.2f else 1f,
            animationSpec = tween(150),
            label = "note_chip_scale",
        )

    Box(
        modifier =
            Modifier
                .scale(animatedScale.value)
                .clickable { onClick() }
                .background(color = bgColor, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = note,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isRoot || isPlaying) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
    }
}

@Composable
private fun DiatonicChordsSection(vm: ScaleExplorerViewModel) {
    val chords = vm.diatonicChords

    Column(Modifier.fillMaxWidth()) {
        SectionHeader(icon = Icons.Default.Star, title = stringResource(R.string.diatonic_chords))
        Spacer(Modifier.height(8.dp))

        if (chords.isEmpty()) {
            Text(
                stringResource(R.string.no_diatonic_chords),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(chords) { _, dc ->
                    DiatonicChordCard(
                        degree = dc.degree,
                        chordName = dc.chord.toString(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DiatonicChordCard(
    degree: String,
    chordName: String,
) {
    OutlinedCard {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = degree,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = chordName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
