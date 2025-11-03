package com.lddev.scalefinder.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.lddev.scalefinder.audio.ChordPlayer
import com.lddev.scalefinder.audio.Metronome
import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.Tuning
import com.lddev.scalefinder.model.Theory

class HomeViewModel : ViewModel() {
    // UI State
    val progression = mutableStateListOf<Chord>()
    var selectedIndex by mutableStateOf<Int?>(null)
        private set
    var selectedScale by mutableStateOf<Scale?>(null)
        private set
    var selectedTuning by mutableStateOf(Tuning.STANDARD)
        private set
    var fretStart by mutableStateOf(0)
        private set
    var fretCount by mutableStateOf(12)
        private set
    var highContrast by mutableStateOf(false)
        private set
    var invertFretboard by mutableStateOf(true)
        private set
    var showNoteNames by mutableStateOf(false)
        private set
    var metronomeBPM by mutableStateOf(120)
        private set
    var metronomeTimeSignature by mutableStateOf(4)
        private set
    var isMetronomeRunning by mutableStateOf(false)
        private set

    private val chordPlayer = ChordPlayer()
    private val metronome = Metronome()

    val suggestions: List<com.lddev.scalefinder.model.ScaleSuggestionRanked>
        get() = Theory.suggestScalesForProgression(progression)

    val chordTones: Set<Int>
        get() = selectedIndex?.let { idx -> progression.getOrNull(idx)?.tones } ?: emptySet()
    
    val metronomeCurrentBeat = metronome.currentBeat

    fun addChord(chord: Chord) {
        progression.add(chord)
        chordPlayer.playChord(chord)
    }

    fun removeChord(index: Int) {
        if (index in progression.indices) progression.removeAt(index)
        if (selectedIndex == index) selectedIndex = null
    }

    fun moveLeft(index: Int) {
        if (index > 0) {
            val c = progression.removeAt(index)
            progression.add(index - 1, c)
        }
    }

    fun moveRight(index: Int) {
        if (index < progression.lastIndex) {
            val c = progression.removeAt(index)
            progression.add(index + 1, c)
        }
    }

    fun selectChord(index: Int) {
        selectedIndex = index
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

    fun setTuning(tuning: Tuning) { selectedTuning = tuning }
    fun updateFretStart(value: Int) { fretStart = value.coerceIn(0, 18) }
    fun updateFretCount(value: Int) { fretCount = value.coerceIn(5, 18) }
    fun toggleHighContrast() { highContrast = !highContrast }
    fun toggleInvertFretboard() { invertFretboard = !invertFretboard }
    fun toggleShowNoteNames() { showNoteNames = !showNoteNames }
    
    // Metronome controls
    fun updateMetronomeBPM(bpm: Int) { 
        metronomeBPM = bpm.coerceIn(30, 200)
        metronome.setBPM(metronomeBPM)
    }
    
    fun updateMetronomeTimeSignature(beats: Int) { 
        metronomeTimeSignature = beats.coerceIn(2, 8)
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

    fun applyProgressionPreset(preset: String) {
        progression.clear()
        when (preset) {
            "I-V-vi-IV (C)" -> {
                progression.addAll(listOf(
                    Chord(com.lddev.scalefinder.model.Note.C, com.lddev.scalefinder.model.ChordQuality.MAJOR),
                    Chord(com.lddev.scalefinder.model.Note.G, com.lddev.scalefinder.model.ChordQuality.MAJOR),
                    Chord(com.lddev.scalefinder.model.Note.A, com.lddev.scalefinder.model.ChordQuality.MINOR),
                    Chord(com.lddev.scalefinder.model.Note.F, com.lddev.scalefinder.model.ChordQuality.MAJOR)
                ))
            }
            "ii-V-I (G)" -> {
                progression.addAll(listOf(
                    Chord(com.lddev.scalefinder.model.Note.A, com.lddev.scalefinder.model.ChordQuality.MINOR7),
                    Chord(com.lddev.scalefinder.model.Note.D, com.lddev.scalefinder.model.ChordQuality.DOMINANT7),
                    Chord(com.lddev.scalefinder.model.Note.G, com.lddev.scalefinder.model.ChordQuality.MAJOR7)
                ))
            }
            "Blues (A)" -> {
                progression.addAll(listOf(
                    Chord(com.lddev.scalefinder.model.Note.A, com.lddev.scalefinder.model.ChordQuality.DOMINANT7),
                    Chord(com.lddev.scalefinder.model.Note.D, com.lddev.scalefinder.model.ChordQuality.DOMINANT7),
                    Chord(com.lddev.scalefinder.model.Note.E, com.lddev.scalefinder.model.ChordQuality.DOMINANT7)
                ))
            }
        }
    }

    fun applyFretPreset(start: Int, count: Int) {
        updateFretStart(start)
        updateFretCount(count)
    }

    override fun onCleared() {
        super.onCleared()
        chordPlayer.stop()
        metronome.cleanup()
    }
}
