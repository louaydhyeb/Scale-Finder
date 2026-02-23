package com.lddev.scalefinder.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lddev.scalefinder.audio.ChordPlayer
import com.lddev.scalefinder.audio.Metronome
import com.lddev.scalefinder.audio.engine.AudioEngine
import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.ChordQuality
import com.lddev.scalefinder.model.ChordVoicing
import com.lddev.scalefinder.model.ChordVoicings
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.FretboardTheme
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.Tuning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    companion object {
        const val DEFAULT_BPM = 120
        const val DEFAULT_TIME_SIGNATURE = 4
        const val MIN_FRET_COUNT = 5
        const val MAX_FRET = 18
        const val MIN_METRONOME_BPM = 30
        const val MAX_METRONOME_BPM = 200
        const val MIN_BEATS = 2
        const val MAX_BEATS = 8
        const val MIN_PROGRESSION_BPM = 40
        const val MAX_PROGRESSION_BPM = 240
    }
    // Audio engine init
    val engine = AudioEngine.instance

    // UI State
    val progression = mutableStateListOf<Chord>()
    var selectedIndex by mutableStateOf<Int?>(null)
        private set
    var selectedScale by mutableStateOf<Scale?>(null)
        private set
    var selectedTuning by mutableStateOf(Tuning.STANDARD)
        private set
    var fretStart by mutableIntStateOf(0)
        private set
    var fretCount by mutableIntStateOf(12)
        private set
    var highContrast by mutableStateOf(false)
        private set
    var invertFretboard by mutableStateOf(true)
        private set
    var showNoteNames by mutableStateOf(false)
        private set
    var fretboardTheme by mutableStateOf(FretboardTheme.ROSEWOOD)
        private set
    var metronomeBPM by mutableIntStateOf(DEFAULT_BPM)
        private set
    var metronomeTimeSignature by mutableIntStateOf(DEFAULT_TIME_SIGNATURE)
        private set
    var isMetronomeRunning by mutableStateOf(false)
        private set
    var selectedVoicing by mutableStateOf<ChordVoicing?>(null)
        private set

    // Progression playback state
    var isPlayingProgression by mutableStateOf(false)
        private set
    var currentPlayingChordIndex by mutableIntStateOf(-1)
        private set
    var progressionBPM by mutableIntStateOf(DEFAULT_BPM)
        private set
    var loopProgression by mutableStateOf(false)
        private set

    private val chordPlayer = ChordPlayer()
    private val metronome = Metronome()

    val chordTones: Set<Int>
        get() = selectedIndex?.let { idx -> progression.getOrNull(idx)?.tones } ?: emptySet()

    /** Voicings available for the currently selected chord */
    val selectedChordVoicings: List<ChordVoicing>
        get() = selectedIndex?.let { idx ->
            progression.getOrNull(idx)?.let { ChordVoicings.getVoicings(it) }
        } ?: emptyList()
    
    val metronomeCurrentBeat = metronome.currentBeat

    fun addChord(chord: Chord) {
        progression.add(chord)
        chordPlayer.playChord(chord)
    }

    fun removeChord(index: Int) {
        if (isPlayingProgression) stopProgression()
        if (index !in progression.indices) return
        progression.removeAt(index)
        selectedIndex = when {
            selectedIndex == index -> { selectedVoicing = null; null }
            selectedIndex != null && selectedIndex!! > index -> selectedIndex!! - 1
            else -> selectedIndex
        }
    }

    fun moveLeft(index: Int) {
        if (index <= 0) return
        val c = progression.removeAt(index)
        progression.add(index - 1, c)
        selectedIndex = when (selectedIndex) {
            index -> index - 1
            index - 1 -> index
            else -> selectedIndex
        }
    }

    fun moveRight(index: Int) {
        if (index >= progression.lastIndex) return
        val c = progression.removeAt(index)
        progression.add(index + 1, c)
        selectedIndex = when (selectedIndex) {
            index -> index + 1
            index + 1 -> index
            else -> selectedIndex
        }
    }

    fun selectChord(index: Int) {
        selectedIndex = index
        selectedVoicing = null // reset voicing when switching chords
        progression.getOrNull(index)?.let { chordPlayer.playChord(it) }
    }

    fun playChordArpeggio(index: Int) {
        progression.getOrNull(index)?.let { chord ->
            chordPlayer.playArpeggio(chord, noteDurationMs = 400, gapMs = 100, direction = "up")
        }
    }

    fun chooseScale(scale: Scale) {
        selectedScale = scale
    }

    /** Show a specific voicing on the fretboard, auto-adjusting the visible range. */
    fun showVoicingOnNeck(voicing: ChordVoicing) {
        selectedVoicing = voicing
        val positiveFrets = voicing.frets.filter { it > 0 }
        if (positiveFrets.isNotEmpty()) {
            val minFret = positiveFrets.min()
            val maxFret = positiveFrets.max()
            val hasOpen = voicing.frets.any { it == 0 }
            val newStart = if (hasOpen) 0 else maxOf(0, minFret - 1)
            val newCount = maxOf(MIN_FRET_COUNT, maxFret - newStart + 3).coerceAtMost(MAX_FRET)
            fretStart = newStart
            fretCount = newCount
        } else {
            fretStart = 0
            fretCount = 5
        }
    }

    fun setTuning(tuning: Tuning) { selectedTuning = tuning }
    fun updateFretStart(value: Int) { fretStart = value.coerceIn(0, MAX_FRET) }
    fun updateFretCount(value: Int) { fretCount = value.coerceIn(MIN_FRET_COUNT, MAX_FRET) }
    fun toggleHighContrast() { highContrast = !highContrast }
    fun toggleInvertFretboard() { invertFretboard = !invertFretboard }
    fun toggleShowNoteNames() { showNoteNames = !showNoteNames }
    fun updateFretboardTheme(theme: FretboardTheme) { fretboardTheme = theme }
    
    // Metronome controls
    fun updateMetronomeBPM(bpm: Int) { 
        metronomeBPM = bpm.coerceIn(MIN_METRONOME_BPM, MAX_METRONOME_BPM)
        metronome.setBPM(metronomeBPM)
    }
    
    fun updateMetronomeTimeSignature(beats: Int) { 
        metronomeTimeSignature = beats.coerceIn(MIN_BEATS, MAX_BEATS)
        metronome.setTimeSignature(metronomeTimeSignature)
    }
    
    fun toggleMetronome() {
        isMetronomeRunning = !isMetronomeRunning
        if (isMetronomeRunning) {
            metronome.setBPM(metronomeBPM)
            metronome.setTimeSignature(metronomeTimeSignature)
            metronome.start()
        } else {
            metronome.stop()
        }
    }

    // ── Progression playback controls ──────────────────────────────

    fun updateProgressionBPM(bpm: Int) {
        progressionBPM = bpm.coerceIn(MIN_PROGRESSION_BPM, MAX_PROGRESSION_BPM)
    }

    fun toggleLoopProgression() {
        loopProgression = !loopProgression
    }

    fun playProgression() {
        if (progression.isEmpty()) return
        // Take a snapshot so mutations during playback won't crash
        val chords = progression.toList()
        isPlayingProgression = true
        currentPlayingChordIndex = 0
        chordPlayer.playProgression(
            chords = chords,
            bpm = progressionBPM,
            beatsPerBar = DEFAULT_TIME_SIGNATURE,
            loop = loopProgression,
            onChordStart = { index ->
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    currentPlayingChordIndex = index
                    selectedIndex = index
                }
            },
            onFinished = {
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    isPlayingProgression = false
                    currentPlayingChordIndex = -1
                }
            }
        )
    }

    fun stopProgression() {
        chordPlayer.stopProgression()
        isPlayingProgression = false
        currentPlayingChordIndex = -1
    }

    fun applyProgressionPreset(preset: ProgressionPreset) {
        if (isPlayingProgression) stopProgression()
        progression.clear()
        selectedIndex = null
        selectedVoicing = null
        progression.addAll(preset.chords)
    }

    fun applyFretPreset(start: Int, count: Int) {
        updateFretStart(start)
        updateFretCount(count)
    }

    override fun onCleared() {
        super.onCleared()
        stopProgression()
        chordPlayer.dispose()
        metronome.cleanup()
        engine.stop()
    }
}

enum class ProgressionPreset(val chords: List<Chord>) {
    POP(listOf(Chord(Note.C, ChordQuality.MAJOR), Chord(Note.G, ChordQuality.MAJOR), Chord(Note.A, ChordQuality.MINOR), Chord(Note.F, ChordQuality.MAJOR))),
    JAZZ(listOf(Chord(Note.A, ChordQuality.MINOR7), Chord(Note.D, ChordQuality.DOMINANT7), Chord(Note.G, ChordQuality.MAJOR7))),
    BLUES(listOf(Chord(Note.A, ChordQuality.DOMINANT7), Chord(Note.D, ChordQuality.DOMINANT7), Chord(Note.E, ChordQuality.DOMINANT7)))
}
