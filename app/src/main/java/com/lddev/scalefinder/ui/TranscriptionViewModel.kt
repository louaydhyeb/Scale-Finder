package com.lddev.scalefinder.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lddev.scalefinder.audio.transcription.AudioDecoder
import com.lddev.scalefinder.audio.transcription.BasicPitchDetector
import com.lddev.scalefinder.audio.transcription.MidiExporter
import com.lddev.scalefinder.audio.transcription.TabMapper
import com.lddev.scalefinder.model.Tablature
import com.lddev.scalefinder.model.Tuning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TranscriptionViewModel(application: Application) : AndroidViewModel(application) {

    enum class State { IDLE, DECODING, ANALYZING, MAPPING, DONE, ERROR }

    var state by mutableStateOf(State.IDLE)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var tablature by mutableStateOf<Tablature?>(null)
        private set
    var selectedTuning by mutableStateOf(Tuning.STANDARD)
        private set
    var detectedNotes by mutableStateOf<List<BasicPitchDetector.NoteEvent>>(emptyList())
        private set

    private val decoder = AudioDecoder(application)
    private val detector = BasicPitchDetector(application)

    val isModelAvailable: Boolean get() = detector.isModelAvailable()

    init {
        if (isModelAvailable) detector.initialize()
    }

    fun transcribe(uri: Uri) {
        viewModelScope.launch {
            try {
                state = State.DECODING
                progress = 0.1f

                val audio = decoder.decode(uri)

                state = State.ANALYZING
                progress = 0.4f

                val notes = withContext(Dispatchers.Default) {
                    detector.detect(audio.samples)
                }
                detectedNotes = notes

                state = State.MAPPING
                progress = 0.8f

                val tab = withContext(Dispatchers.Default) {
                    TabMapper.map(notes, selectedTuning)
                }

                tablature = tab
                progress = 1f
                state = State.DONE
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
                state = State.ERROR
            }
        }
    }

    fun setTuning(tuning: Tuning) {
        selectedTuning = tuning
        val notes = detectedNotes
        if (notes.isNotEmpty()) {
            viewModelScope.launch {
                tablature = withContext(Dispatchers.Default) {
                    TabMapper.map(notes, tuning)
                }
            }
        }
    }

    var midiExported by mutableStateOf(false)
        private set

    fun exportMidi(destinationUri: Uri) {
        val notes = detectedNotes
        if (notes.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver
                    .openOutputStream(destinationUri)?.use { out ->
                        MidiExporter.exportTo(notes, out)
                    }
            }
            midiExported = true
        }
    }

    fun reset() {
        state = State.IDLE
        progress = 0f
        errorMessage = null
        tablature = null
        detectedNotes = emptyList()
        midiExported = false
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}
