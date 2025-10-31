package com.lddev.scalefinder.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lddev.scalefinder.ui.HomeViewModel
import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.ChordQuality
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.Theory
import com.lddev.scalefinder.model.Tuning
import com.lddev.scalefinder.ui.components.GuitarFretboard
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.lddev.scalefinder.audio.NotePlayer

@Composable
fun HomeScreen(modifier: Modifier = Modifier, vm: HomeViewModel = viewModel(), onToggleTheme: () -> Unit = {}, isDark: Boolean = false) {
    val progression = vm.progression
    val selectedIndex = vm.selectedIndex
    val selectedScale = vm.selectedScale
    val selectedTuning = vm.selectedTuning
    val fretStart = vm.fretStart
    val fretCount = vm.fretCount
    val highContrast = vm.highContrast
    val invert = vm.invertFretboard
    val haptics = LocalHapticFeedback.current
    val notePlayer = remember { NotePlayer() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Scale Finder", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = onToggleTheme, modifier = Modifier.semantics { contentDescription = "Toggle theme" }) {
                Text(if (isDark) "Light" else "Dark")
            }
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
            onSelect = { idx -> vm.selectChord(idx); haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
        )

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            DropdownTuning(selectedTuning, vm::setTuning)
            Stepper(label = "Start Fret", value = fretStart, onChange = vm::updateFretStart)
            Stepper(label = "Fret Count", value = fretCount, onChange = vm::updateFretCount)
            OutlinedButton(onClick = vm::toggleHighContrast) { Text(if (highContrast) "High Contrast: On" else "High Contrast: Off") }
            OutlinedButton(onClick = vm::toggleInvertFretboard) { Text(if (invert) "Invert: On" else "Invert: Off") }
        }

        Spacer(Modifier.height(8.dp))

        SuggestionsPanel(
            progression = progression,
            onChooseScale = vm::chooseScale
        )

        Spacer(Modifier.height(8.dp))

        val chordTones = vm.chordTones

        OutlinedCard(Modifier.fillMaxWidth().semantics { contentDescription = "Fretboard card" }) {
            Column(Modifier.padding(8.dp)) {
                Text("Fretboard", style = MaterialTheme.typography.titleMedium)
                GuitarFretboard(
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
                    Text("Tuning: ${selectedTuning.name}")
                    Text("Scale: ${selectedScale?.toString() ?: "None"}")
                }
            }
        }
    }
}

@Composable
private fun PresetsBar(vm: HomeViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Presets", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { vm.applyProgressionPreset("I-V-vi-IV (C)") }) { Text("I–V–vi–IV (C)") }
        OutlinedButton(onClick = { vm.applyProgressionPreset("ii-V-I (G)") }) { Text("ii–V–I (G)") }
        OutlinedButton(onClick = { vm.applyProgressionPreset("Blues (A)") }) { Text("Blues (A)") }
        OutlinedButton(onClick = { vm.setTuning(Tuning.STANDARD) }) { Text("Std Tuning") }
        OutlinedButton(onClick = { vm.setTuning(Tuning.DROP_D) }) { Text("Drop D") }
        OutlinedButton(onClick = { vm.applyFretPreset(0, 12) }) { Text("Frets 0–12") }
        OutlinedButton(onClick = { vm.applyFretPreset(5, 7) }) { Text("Box @5") }
    }
}

@Composable
private fun ProgressionEditor(
    progression: MutableList<Chord>,
    onAdd: (Chord) -> Unit,
    onRemove: (Int) -> Unit,
    onMoveLeft: (Int) -> Unit,
    onMoveRight: (Int) -> Unit,
    onSelect: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Chord Progression", style = MaterialTheme.typography.titleMedium)
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
            itemsIndexed(progression) { idx, chord ->
                OutlinedCard {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(chord.toString(), style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { onMoveLeft(idx) },
                                modifier = Modifier.height(48.dp).semantics { contentDescription = "Move left" }
                            ) { Text("←") }
                            OutlinedButton(
                                onClick = { onSelect(idx) },
                                modifier = Modifier.height(48.dp).semantics { contentDescription = "Select chord" }
                            ) { Text("Select") }
                            OutlinedButton(
                                onClick = { onMoveRight(idx) },
                                modifier = Modifier.height(48.dp).semantics { contentDescription = "Move right" }
                            ) { Text("→") }
                            OutlinedButton(
                                onClick = { onRemove(idx) },
                                modifier = Modifier.height(48.dp).semantics { contentDescription = "Remove chord" }
                            ) { Text("Remove") }
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

    ExposedDropdownMenuBox(expanded = expandedRoot, onExpandedChange = { expandedRoot = !expandedRoot }) {
        OutlinedTextField(
            value = root.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Root") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedRoot) },
            modifier = Modifier.menuAnchor().semantics { contentDescription = "Root selector" }
        )
        DropdownMenu(expanded = expandedRoot, onDismissRequest = { expandedRoot = false }) {
            Note.entries.forEach { n ->
                DropdownMenuItem(text = { Text(n.toString()) }, onClick = { root = n; expandedRoot = false })
            }
        }
    }

    ExposedDropdownMenuBox(expanded = expandedQuality, onExpandedChange = { expandedQuality = !expandedQuality }) {
        OutlinedTextField(
            value = quality.display,
            onValueChange = {},
            readOnly = true,
            label = { Text("Quality") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedQuality) },
            modifier = Modifier.menuAnchor().semantics { contentDescription = "Quality selector" }
        )
        DropdownMenu(expanded = expandedQuality, onDismissRequest = { expandedQuality = false }) {
            ChordQuality.entries.forEach { q ->
                DropdownMenuItem(text = { Text(q.display) }, onClick = { quality = q; expandedQuality = false })
            }
        }
    }

    Button(
        onClick = { onAdd(Chord(root, quality)) },
        modifier = Modifier.height(48.dp).semantics { contentDescription = "Add chord" }
    ) { Text("Add Chord") }
}

@Composable
private fun SuggestionsPanel(
    progression: List<Chord>,
    onChooseScale: (Scale) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Scale Suggestions", style = MaterialTheme.typography.titleMedium)
        if (progression.isEmpty()) {
            Text("Add chords to see suggestions.")
        } else {
            val ranked = Theory.suggestScalesForProgression(progression)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(ranked) { _, s ->
                    Card { Column(Modifier.padding(8.dp)) {
                        Text(s.scale.toString(), style = MaterialTheme.typography.titleSmall)
                        Text(s.rationale, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { onChooseScale(s.scale) },
                            modifier = Modifier.height(48.dp).semantics { contentDescription = "Show scale on neck" }
                        ) { Text("Show on Neck") }
                    } }
                }
            }
        }
    }
}

@Composable
private fun DropdownTuning(current: Tuning, onChange: (Tuning) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = true }, modifier = Modifier.semantics { contentDescription = "Tuning menu" }) { Text("Tuning: ${current.name}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Tuning.all().forEach { t ->
                DropdownMenuItem(text = { Text(t.name) }, onClick = { onChange(t); expanded = false })
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        OutlinedButton(onClick = { onChange(value - 1) }, modifier = Modifier.height(48.dp).semantics { contentDescription = "$label decrement" }) { Text("-") }
        Text(value.toString())
        OutlinedButton(onClick = { onChange(value + 1) }, modifier = Modifier.height(48.dp).semantics { contentDescription = "$label increment" }) { Text("+") }
    }
}


