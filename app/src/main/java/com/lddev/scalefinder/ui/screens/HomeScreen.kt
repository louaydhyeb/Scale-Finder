package com.lddev.scalefinder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lddev.scalefinder.R
import com.lddev.scalefinder.audio.NotePlayer
import com.lddev.scalefinder.ui.HomeViewModel
import com.lddev.scalefinder.ui.components.home_components.ChordVoicingSection
import com.lddev.scalefinder.ui.components.home_components.FretHighlight
import com.lddev.scalefinder.ui.components.home_components.GuitarFretboard
import com.lddev.scalefinder.ui.components.home_components.MetronomeControls
import com.lddev.scalefinder.ui.components.home_components.PresetsBar
import com.lddev.scalefinder.ui.components.home_components.ProgressionEditor
import com.lddev.scalefinder.ui.components.home_components.Stepper
import com.lddev.scalefinder.ui.components.home_components.SuggestionsPanel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel()
) {
    val haptics = LocalHapticFeedback.current
    val notePlayer = remember { NotePlayer() }
    DisposableEffect(Unit) { onDispose { notePlayer.dispose() } }

    val metronomeCurrentBeat by vm.metronomeCurrentBeat.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TitleBar()

        Spacer(Modifier.height(8.dp))
        PresetsBar(vm)
        Spacer(Modifier.height(8.dp))

        ProgressionEditor(
            progression = vm.progression,
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
            },
            isPlaying = vm.isPlayingProgression,
            currentPlayingIndex = vm.currentPlayingChordIndex,
            progressionBPM = vm.progressionBPM,
            loopEnabled = vm.loopProgression,
            onPlay = {
                vm.playProgression()
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onStop = { vm.stopProgression() },
            onBPMChange = vm::updateProgressionBPM,
            onToggleLoop = vm::toggleLoopProgression
        )

        VoicingPanel(vm = vm, haptics = haptics)

        Spacer(Modifier.height(8.dp))
        SuggestionsPanel(progression = vm.progression, onChooseScale = vm::chooseScale)

        Spacer(Modifier.height(8.dp))
        MetronomeControls(
            bpm = vm.metronomeBPM,
            timeSignature = vm.metronomeTimeSignature,
            currentBeat = metronomeCurrentBeat,
            isRunning = vm.isMetronomeRunning,
            onBPMChanged = vm::updateMetronomeBPM,
            onTimeSignatureChanged = vm::updateMetronomeTimeSignature,
            onToggle = vm::toggleMetronome
        )

        Spacer(Modifier.height(8.dp))
        FretSteppers(vm)

        Spacer(Modifier.height(8.dp))
        FretboardCard(vm = vm, notePlayer = notePlayer)
    }
}

@Composable
private fun TitleBar() {
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
            ) { append(stringResource(R.string.app_title_scale)) }
            append(" ")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            ) { append(stringResource(R.string.app_title_finder)) }
        },
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun VoicingPanel(
    vm: HomeViewModel,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val selectedChord = vm.selectedIndex?.let { vm.progression.getOrNull(it) }
    AnimatedVisibility(
        visible = selectedChord != null,
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
        if (selectedChord != null) {
            Column {
                Spacer(Modifier.height(8.dp))
                ChordVoicingSection(
                    chord = selectedChord,
                    voicings = vm.selectedChordVoicings,
                    selectedVoicing = vm.selectedVoicing,
                    onShowOnNeck = { voicing ->
                        vm.showVoicingOnNeck(voicing)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
        }
    }
}

@Composable
private fun FretSteppers(vm: HomeViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Stepper(label = stringResource(R.string.start_fret), value = vm.fretStart, onChange = vm::updateFretStart)
        Stepper(label = stringResource(R.string.fret_count), value = vm.fretCount, onChange = vm::updateFretCount)
    }
}

@Composable
private fun FretboardCard(vm: HomeViewModel, notePlayer: NotePlayer) {
    val fretboardCardDesc = stringResource(R.string.content_fretboard_card)
    val voicingHighlightColor = MaterialTheme.colorScheme.tertiary
    val voicingHighlights = vm.selectedVoicing?.frets?.mapIndexedNotNull { stringIdx, fret ->
        if (fret >= 0) FretHighlight(stringIdx, fret, voicingHighlightColor)
        else null
    } ?: emptyList()

    OutlinedCard(Modifier.fillMaxWidth().semantics { contentDescription = fretboardCardDesc }) {
        Column(Modifier.padding(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.content_fretboard_icon), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.fretboard), style = MaterialTheme.typography.titleMedium)
            }
            GuitarFretboard(
                showNoteNames = vm.showNoteNames,
                tuning = vm.selectedTuning,
                scale = vm.selectedScale,
                fretStart = vm.fretStart,
                fretCount = vm.fretCount,
                chordTones = vm.chordTones,
                highContrast = vm.highContrast,
                invertStrings = vm.invertFretboard,
                theme = vm.fretboardTheme,
                highlights = voicingHighlights,
                onNoteTapped = { stringIdx, fret, _ ->
                    val freq = vm.selectedTuning.getFrequency(stringIdx, fret)
                    notePlayer.playGuitarNote(freq, durationMs = 1000)
                }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.tuning_label, vm.selectedTuning.name))
                Text(stringResource(R.string.scale_label, vm.selectedScale?.toString() ?: stringResource(R.string.scale_none)))
            }
        }
    }
}
