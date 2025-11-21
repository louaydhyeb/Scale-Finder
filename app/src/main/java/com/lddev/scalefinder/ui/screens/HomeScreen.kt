package com.lddev.scalefinder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lddev.scalefinder.R
import com.lddev.scalefinder.audio.NotePlayer
import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.ChordQuality
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.Theory
import com.lddev.scalefinder.model.Tuning
import com.lddev.scalefinder.ui.HomeViewModel
import com.lddev.scalefinder.ui.components.GuitarFretboard
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(modifier: Modifier = Modifier, vm: HomeViewModel = viewModel(), onToggleTheme: () -> Unit = {}, isDark: Boolean = false) {
    val progression = vm.progression
    val selectedScale = vm.selectedScale
    val selectedTuning = vm.selectedTuning
    val fretStart = vm.fretStart
    val fretCount = vm.fretCount
    val highContrast = vm.highContrast
    val invert = vm.invertFretboard
    val showNoteNames = vm.showNoteNames
    val metronomeBPM = vm.metronomeBPM
    val metronomeTimeSignature = vm.metronomeTimeSignature
    val isMetronomeRunning = vm.isMetronomeRunning
    val metronomeCurrentBeat by vm.metronomeCurrentBeat.collectAsState()
    val haptics = LocalHapticFeedback.current
    val notePlayer = remember { NotePlayer() }
    DisposableEffect(Unit) {
        onDispose { notePlayer.dispose() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            // Modern gradient title
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(stringResource(R.string.app_title_scale))
                    }
                    append(" ")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(stringResource(R.string.app_title_finder))
                    }
                },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
            // Settings menu button
            SettingsMenu(
                isDark = isDark,
                onToggleTheme = onToggleTheme,
                selectedTuning = selectedTuning,
                onTuningChanged = vm::setTuning,
                highContrast = highContrast,
                onToggleHighContrast = vm::toggleHighContrast,
                invert = invert,
                onToggleInvert = vm::toggleInvertFretboard,
                showNoteNames = showNoteNames,
                onToggleShowNoteNames = vm::toggleShowNoteNames
            )
        }
        Spacer(Modifier.height(8.dp))

        PresetsBar(vm)
        Spacer(Modifier.height(8.dp))

        ProgressionEditor(
            progression = progression,
            onAdd = { chord ->
                vm.addChord(chord)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onRemove = vm::removeChord,
            onMoveLeft = vm::moveLeft,
            onMoveRight = vm::moveRight,
            onSelect = { idx -> vm.selectChord(idx); haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
            onPlayArpeggio = { idx -> 
                vm.playChordArpeggio(idx)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        )

        Spacer(Modifier.height(8.dp))

        SuggestionsPanel(
            progression = progression,
            onChooseScale = vm::chooseScale
        )

        Spacer(Modifier.height(8.dp))

        MetronomeControls(
            bpm = metronomeBPM,
            timeSignature = metronomeTimeSignature,
            currentBeat = metronomeCurrentBeat,
            isRunning = isMetronomeRunning,
            onBPMChanged = vm::updateMetronomeBPM,
            onTimeSignatureChanged = vm::updateMetronomeTimeSignature,
            onToggle = vm::toggleMetronome
        )

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Stepper(label = stringResource(R.string.start_fret), value = fretStart, onChange = vm::updateFretStart)
            Stepper(label = stringResource(R.string.fret_count), value = fretCount, onChange = vm::updateFretCount)
        }

        Spacer(Modifier.height(8.dp))

        val chordTones = vm.chordTones
        val fretboardCardDesc = stringResource(R.string.content_fretboard_card)

        OutlinedCard(Modifier.fillMaxWidth().semantics { contentDescription = fretboardCardDesc }) {
            Column(Modifier.padding(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.fretboard), style = MaterialTheme.typography.titleMedium)
                }
                GuitarFretboard(
                    showNoteNames = showNoteNames,
                    tuning = selectedTuning,
                    scale = selectedScale,
                    fretStart = fretStart,
                    fretCount = fretCount,
                    chordTones = chordTones,
                    highContrast = highContrast,
                    invertStrings = invert,
                    onNoteTapped = { stringIdx, fret, note ->
                        val freq = selectedTuning.getFrequency(stringIdx, fret)
                        notePlayer.playGuitarNote(freq, durationMs = 1000)
                    }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.tuning_label, selectedTuning.name))
                    Text(stringResource(R.string.scale_label, selectedScale?.toString() ?: stringResource(R.string.scale_none)))
                }
            }
        }
    }
}

@Composable
private fun SettingsMenu(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    selectedTuning: Tuning,
    onTuningChanged: (Tuning) -> Unit,
    highContrast: Boolean,
    onToggleHighContrast: () -> Unit,
    invert: Boolean,
    onToggleInvert: () -> Unit,
    showNoteNames: Boolean,
    onToggleShowNoteNames: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var tuningMenuExpanded by remember { mutableStateOf(false) }
    var themeRotation by remember { mutableStateOf(0f) }
    val rotation = animateFloatAsState(
        targetValue = themeRotation,
        animationSpec = tween(durationMillis = 300),
        label = "theme_rotation"
    )
    
    val settingsMenuDesc = stringResource(R.string.content_settings_menu)
    val settingsDesc = stringResource(R.string.settings)
    
    Box {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.semantics { contentDescription = settingsMenuDesc }
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = settingsDesc,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Theme toggle
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotation.value),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (isDark) stringResource(R.string.theme_light) else stringResource(R.string.theme_dark),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                onClick = {
                    themeRotation = if (themeRotation == 0f) 180f else 0f
                    onToggleTheme()
                    // Don't close menu - let user close it manually
                }
            )
            
            // Tuning selector with submenu
            DropdownMenuItem(
                text = {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                stringResource(R.string.tuning),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            selectedTuning.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                onClick = { tuningMenuExpanded = true }
            )
            
            // High Contrast toggle
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (highContrast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            if (highContrast) stringResource(R.string.contrast_high) else stringResource(R.string.contrast_normal),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (highContrast) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                },
                onClick = {
                    onToggleHighContrast()
                    // Don't close menu - let user close it manually
                }
            )
            
            // Invert toggle
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (invert) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            if (invert) stringResource(R.string.fretboard_inverted) else stringResource(R.string.fretboard_normal),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (invert) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                },
                onClick = {
                    onToggleInvert()
                    // Don't close menu - let user close it manually
                }
            )
            
            // Show Note Names toggle
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (showNoteNames) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            if (showNoteNames) stringResource(R.string.note_names_hide) else stringResource(R.string.note_names_show),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (showNoteNames) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                },
                onClick = {
                    onToggleShowNoteNames()
                    // Don't close menu - let user close it manually
                }
            )
        }
        
        // Tuning submenu
        DropdownMenu(
            expanded = tuningMenuExpanded,
            onDismissRequest = { tuningMenuExpanded = false }
        ) {
            Tuning.all().forEach { tuning ->
                DropdownMenuItem(
                    text = {
                        Text(
                            tuning.name,
                            fontWeight = if (tuning == selectedTuning) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onTuningChanged(tuning)
                        tuningMenuExpanded = false
                        // Don't close main menu - let user close it manually
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetsBar(vm: HomeViewModel) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.quick_presets),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        val presets = listOf(
            Triple(stringResource(R.string.preset_pop_progression), stringResource(R.string.preset_pop_description)) { vm.applyProgressionPreset("I-V-vi-IV (C)") },
            Triple(stringResource(R.string.preset_jazz_progression), stringResource(R.string.preset_jazz_description)) { vm.applyProgressionPreset("ii-V-I (G)") },
            Triple(stringResource(R.string.preset_blues), stringResource(R.string.preset_blues_description)) { vm.applyProgressionPreset("Blues (A)") },
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
        colors = androidx.compose.material3.CardDefaults.cardColors(
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

@Composable
private fun ProgressionEditor(
    progression: MutableList<Chord>,
    onAdd: (Chord) -> Unit,
    onRemove: (Int) -> Unit,
    onMoveLeft: (Int) -> Unit,
    onMoveRight: (Int) -> Unit,
    onSelect: (Int) -> Unit,
    onPlayArpeggio: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.chord_progression), style = MaterialTheme.typography.titleMedium)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChordPicker(onAdd)
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(
                items = progression,
                key = { index, chord -> "${chord.root}${chord.quality}${index}" }
            ) { idx, chord ->
                var cardScale by remember { mutableStateOf(0.8f) }
                val scaleAnimation = animateFloatAsState(
                    targetValue = cardScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "card_scale"
                )
                
                LaunchedEffect(chord) {
                    cardScale = 0.8f
                    delay(50)
                    cardScale = 1f
                }
                
                val moveLeftDesc = stringResource(R.string.move_left)
                val selectChordDesc = stringResource(R.string.select_chord)
                val playArpeggioDesc = stringResource(R.string.play_arpeggio)
                val moveRightDesc = stringResource(R.string.move_right)
                val removeChordDesc = stringResource(R.string.remove_chord)
                
                OutlinedCard(
                    modifier = Modifier.scale(scaleAnimation.value)
                ) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(chord.toString(), style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onMoveLeft(idx) },
                                modifier = Modifier.semantics { contentDescription = moveLeftDesc }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = moveLeftDesc)
                            }
                            IconButton(
                                onClick = { onSelect(idx) },
                                modifier = Modifier.semantics { contentDescription = selectChordDesc }
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = selectChordDesc, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { onPlayArpeggio(idx) },
                                modifier = Modifier.semantics { contentDescription = playArpeggioDesc }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = playArpeggioDesc, tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(
                                onClick = { onMoveRight(idx) },
                                modifier = Modifier.semantics { contentDescription = moveRightDesc }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = moveRightDesc)
                            }
                            IconButton(
                                onClick = { onRemove(idx) },
                                modifier = Modifier.semantics { contentDescription = removeChordDesc }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = removeChordDesc, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
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
                modifier = Modifier.menuAnchor().semantics { contentDescription = rootSelectorDesc }
        )
        DropdownMenu(expanded = expandedRoot, onDismissRequest = { expandedRoot = false }) {
            Note.entries.forEach { n ->
                DropdownMenuItem(text = { Text(n.toString()) }, onClick = { root = n; expandedRoot = false })
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
                modifier = Modifier.menuAnchor().semantics { contentDescription = qualitySelectorDesc }
        )
        DropdownMenu(expanded = expandedQuality, onDismissRequest = { expandedQuality = false }) {
            ChordQuality.entries.forEach { q ->
                DropdownMenuItem(text = { Text(q.display) }, onClick = { quality = q; expandedQuality = false })
            }
        }
    }

    var addButtonPressed by remember { mutableStateOf(false) }
    val addButtonScale = animateFloatAsState(
        targetValue = if (addButtonPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "add_button_scale"
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
        modifier = Modifier
            .height(48.dp)
            .scale(addButtonScale.value)
            .semantics { contentDescription = addChordDesc }
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.add_chord))
    }
}

@Composable
private fun SuggestionsPanel(
    progression: List<Chord>,
    onChooseScale: (Scale) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.scale_suggestions), style = MaterialTheme.typography.titleMedium)
        }
        if (progression.isEmpty()) {
            Text(stringResource(R.string.add_chords_to_see_suggestions))
        } else {
            val ranked = Theory.suggestScalesForProgression(progression)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(ranked) { index, s ->
                    val isTop = index == 0
                    var pulseScale by remember { mutableFloatStateOf(1f) }
                    val pulseAnimation = animateFloatAsState(
                        targetValue = pulseScale,
                        animationSpec = if (isTop) {
                            tween(durationMillis = 1000, delayMillis = index * 100)
                        } else {
                            tween(durationMillis = 300, delayMillis = index * 50)
                        },
                        label = "pulse_scale"
                    )
                    
                    LaunchedEffect(isTop) {
                        if (isTop) {
                            while (true) {
                                pulseScale = 1.05f
                                delay(1500)
                                pulseScale = 1f
                                delay(1500)
                            }
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(
                                durationMillis = 400,
                                delayMillis = index * 80,
                                easing = FastOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 400,
                                delayMillis = index * 80
                            )
                        ),
                        modifier = Modifier.scale(if (isTop) pulseAnimation.value else 1f)
                    ) {
                        Card {
                            Column(Modifier.padding(8.dp)) {
                                Text(s.scale.toString(), style = MaterialTheme.typography.titleSmall)
                                Text(s.rationale, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp))
                                val showScaleDesc = stringResource(R.string.content_show_scale_on_neck)
                                OutlinedButton(
                                    onClick = { onChooseScale(s.scale) },
                                    modifier = Modifier.height(48.dp).semantics { contentDescription = showScaleDesc }
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text(stringResource(R.string.show_on_neck))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownTuning(current: Tuning, onChange: (Tuning) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val tuningMenuDesc = stringResource(R.string.content_tuning_menu)
    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = tuningMenuDesc }
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.tuning_label, current.name))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Tuning.all().forEach { t ->
                DropdownMenuItem(text = { Text(t.name) }, onClick = { onChange(t); expanded = false })
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    var decrementPressed by remember { mutableStateOf(false) }
    var incrementPressed by remember { mutableStateOf(false) }
    
    val decrementScale = animateFloatAsState(
        targetValue = if (decrementPressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "decrement_scale"
    )
    
    val incrementScale = animateFloatAsState(
        targetValue = if (incrementPressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "increment_scale"
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
            modifier = Modifier
                .scale(decrementScale.value)
                .semantics { contentDescription = "$label decrement" }
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.decrease))
        }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(
            onClick = {
                incrementPressed = true
                onChange(value + 1)
            },
            modifier = Modifier
                .scale(incrementScale.value)
                .semantics { contentDescription = "$label increment" }
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.increase))
        }
    }
}

@Composable
private fun MetronomeControls(
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
            // Header with play/pause and expand buttons
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
                        modifier = Modifier.semantics { contentDescription = if (isRunning) metronomePauseDesc else metronomeStartDesc }
                    ) {
                        Icon(
                            if (isRunning) Icons.Default.Clear else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) metronomePauseLabel else metronomePlayLabel,
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
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.semantics { contentDescription = if (isExpanded) metronomeCollapseDesc else metronomeExpandDesc }
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) metronomeCollapseLabel else metronomeExpandLabel,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Expanded content
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
                    
                    // Visual beat indicators
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
                
                Spacer(Modifier.height(20.dp))
                
                // Controls
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

