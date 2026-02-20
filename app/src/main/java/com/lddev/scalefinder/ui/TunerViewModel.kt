package com.lddev.scalefinder.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lddev.scalefinder.audio.NotePlayer
import com.lddev.scalefinder.audio.PitchDetector
import com.lddev.scalefinder.audio.PitchResult
import com.lddev.scalefinder.model.Tuning
import kotlinx.coroutines.launch
import kotlin.math.log2

class TunerViewModel : ViewModel() {
    private val pitchDetector = PitchDetector()
    private val notePlayer = NotePlayer()

    var isListening by mutableStateOf(false)
        private set
    var currentPitch by mutableStateOf<PitchResult?>(null)
        private set
    var selectedTuning by mutableStateOf(Tuning.STANDARD)
        private set
    var targetStringIndex by mutableIntStateOf(-1)
        private set

    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun startListening() {
        if (isListening) return
        pitchDetector.start()
        isListening = true
        viewModelScope.launch {
            pitchDetector.pitchFlow.collect { pitch ->
                currentPitch = pitch
            }
        }
    }

    fun stopListening() {
        pitchDetector.stop()
        isListening = false
        currentPitch = null
    }

    fun selectTuning(tuning: Tuning) {
        selectedTuning = tuning
        targetStringIndex = -1
    }

    fun selectTargetString(index: Int) {
        targetStringIndex = if (targetStringIndex == index) -1 else index
    }

    fun getStringLabel(index: Int): String {
        val midi = selectedTuning.getOpenStringMidi(index)
        return "${noteNames[midi % 12]}${midi / 12 - 1}"
    }

    /**
     * Returns cents deviation relative to the target string, or null if
     * no target is selected. Useful when a specific string is being tuned.
     */
    fun centsFromTarget(): Float? {
        if (targetStringIndex < 0) return null
        val freq = currentPitch?.frequency ?: return null
        val targetFreq = selectedTuning.getFrequency(targetStringIndex, 0)
        return (1200.0 * log2(freq.toDouble() / targetFreq)).toFloat()
    }

    override fun onCleared() {
        super.onCleared()
        pitchDetector.destroy()
        notePlayer.dispose()
    }
}
