package com.lddev.scalefinder.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lddev.scalefinder.audio.NotePlayer
import com.lddev.scalefinder.model.DiatonicChord
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.ScaleFormulas
import com.lddev.scalefinder.model.ScaleType
import com.lddev.scalefinder.model.Tuning
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

class ScaleExplorerViewModel : ViewModel() {
    private val notePlayer = NotePlayer()
    private var playJob: Job? = null

    var selectedRoot by mutableStateOf(Note.C)
        private set
    var selectedScaleType by mutableStateOf(ScaleType.MAJOR)
        private set
    var selectedTuning by mutableStateOf(Tuning.STANDARD)
        private set
    var fretStart by mutableIntStateOf(0)
        private set
    var fretCount by mutableIntStateOf(12)
        private set
    var showNoteNames by mutableStateOf(true)
        private set
    var highContrast by mutableStateOf(false)
        private set
    var invertFretboard by mutableStateOf(true)
        private set
    var isPlayingScale by mutableStateOf(false)
        private set
    var currentPlayingNoteIndex by mutableIntStateOf(-1)
        private set

    val scale: Scale get() = Scale(selectedRoot, selectedScaleType)
    val scaleNotes: List<Note> get() = ScaleFormulas.scaleNotes(scale)
    val diatonicChords: List<DiatonicChord> get() = ScaleFormulas.diatonicChords(scale)

    fun setRoot(note: Note) {
        selectedRoot = note
    }

    fun setScaleType(type: ScaleType) {
        selectedScaleType = type
    }

    fun updateFretStart(value: Int) {
        fretStart = value.coerceIn(0, 18)
    }

    fun updateFretCount(value: Int) {
        fretCount = value.coerceIn(5, 18)
    }

    private fun midiToFreq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    fun playFrequency(freq: Double) {
        notePlayer.playGuitarNote(freq, durationMs = 1000)
    }

    fun playNoteAtIndex(index: Int) {
        val notes = scaleNotes
        if (index !in notes.indices) return
        val rootMidi = selectedTuning.getOpenStringMidi(0) + 12
        val midi = noteToMidi(notes[index], rootMidi)
        notePlayer.playGuitarNote(midiToFreq(midi), durationMs = 800)
    }

    private fun noteToMidi(
        note: Note,
        octaveBase: Int,
    ): Int {
        val offset = Note.positiveMod(note.semitone - selectedRoot.semitone, 12)
        return octaveBase + offset
    }

    fun playScaleAscending() {
        playJob?.cancel()
        isPlayingScale = true
        playJob =
            viewModelScope.launch {
                val notes = scaleNotes
                val rootMidi = selectedTuning.getOpenStringMidi(0) + 12
                for ((idx, note) in notes.withIndex()) {
                    currentPlayingNoteIndex = idx
                    val midi = noteToMidi(note, rootMidi)
                    notePlayer.playGuitarNote(midiToFreq(midi), durationMs = 500)
                    delay(420)
                }
                currentPlayingNoteIndex = notes.size
                val octaveMidi = rootMidi + 12
                notePlayer.playGuitarNote(midiToFreq(octaveMidi), durationMs = 800)
                delay(700)
                isPlayingScale = false
                currentPlayingNoteIndex = -1
            }
    }

    fun playScaleDescending() {
        playJob?.cancel()
        isPlayingScale = true
        playJob =
            viewModelScope.launch {
                val notes = scaleNotes
                val rootMidi = selectedTuning.getOpenStringMidi(0) + 12

                currentPlayingNoteIndex = notes.size
                val octaveMidi = rootMidi + 12
                notePlayer.playGuitarNote(midiToFreq(octaveMidi), durationMs = 500)
                delay(420)

                for (idx in notes.indices.reversed()) {
                    currentPlayingNoteIndex = idx
                    val midi = noteToMidi(notes[idx], rootMidi)
                    notePlayer.playGuitarNote(midiToFreq(midi), durationMs = 500)
                    delay(420)
                }
                delay(300)
                isPlayingScale = false
                currentPlayingNoteIndex = -1
            }
    }

    fun stopScale() {
        playJob?.cancel()
        isPlayingScale = false
        currentPlayingNoteIndex = -1
    }

    override fun onCleared() {
        super.onCleared()
        playJob?.cancel()
        notePlayer.dispose()
    }
}
