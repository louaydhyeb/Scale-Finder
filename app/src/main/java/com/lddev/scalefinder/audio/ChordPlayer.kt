package com.lddev.scalefinder.audio

import com.lddev.scalefinder.audio.engine.AudioEngine
import com.lddev.scalefinder.audio.engine.GuitarKarplusStrong
import com.lddev.scalefinder.model.Chord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

class ChordPlayer {
    private val engine = AudioEngine.instance
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var activeVoices = mutableListOf<GuitarKarplusStrong>()
    private val releaseJobs = mutableListOf<Job>()
    private val stopping = AtomicBoolean(false)
    private var arpeggioPlaying = AtomicBoolean(false)
    private val progressionPlaying = AtomicBoolean(false)

    companion object {
        private const val RELEASE_WINDOW_MS = 150L
        private const val STRUM_DELAY_MS = 25L
        private const val CHORD_BRIGHTNESS = 0.45f
        private const val ARPEGGIO_BRIGHTNESS = 0.5f
        private const val TAIL_MS = 500L
        private const val BASE_MIDI = 60
    }

    fun playChord(chord: Chord, durationMs: Int = 1200) {
        stop()

        val frequencies = chordFrequencies(chord)
        val voices = frequencies.map { GuitarKarplusStrong(brightness = CHORD_BRIGHTNESS) }

        synchronized(activeVoices) {
            voices.forEach { voice ->
                engine.addDsp(voice)
                activeVoices.add(voice)
            }
        }

        engine.start()

        stopping.set(false)
        val releaseJob = scope.launch {
            voices.forEachIndexed { index, voice ->
                if (stopping.get()) return@launch
                voice.noteOn(frequencies[index])
                if (index < voices.size - 1) delay(STRUM_DELAY_MS)
            }

            delay(durationMs.toLong().coerceAtLeast(0L))
            if (!stopping.get()) {
                voices.forEach { it.noteOff() }
                delay(TAIL_MS)
                synchronized(activeVoices) {
                    voices.forEach { voice ->
                        engine.removeDsp(voice)
                        activeVoices.remove(voice)
                    }
                }
            }
        }
        synchronized(releaseJobs) {
            releaseJobs.add(releaseJob)
        }
        releaseJob.invokeOnCompletion {
            synchronized(releaseJobs) {
                releaseJobs.remove(releaseJob)
            }
        }
    }

    fun stop() {
        stopping.set(true)
        arpeggioPlaying.set(false)
        progressionPlaying.set(false)

        synchronized(releaseJobs) {
            releaseJobs.forEach { it.cancel() }
            releaseJobs.clear()
        }

        cleanupVoices()
    }

    fun dispose() {
        stop()
        scope.cancel()
    }

    private fun midiToFreq(midi: Int): Double {
        return 440.0 * 2.0.pow((midi - 69) / 12.0)
    }

    private fun chordFrequencies(chord: Chord): List<Double> {
        val rootMidi = BASE_MIDI + chord.root.semitone
        return chord.quality.intervals.map { midiToFreq(rootMidi + it) }
    }

    fun playProgression(
        chords: List<Chord>,
        bpm: Int = 120,
        beatsPerBar: Int = 4,
        loop: Boolean = false,
        onChordStart: (Int) -> Unit = {},
        onFinished: () -> Unit = {}
    ) {
        stop()
        if (chords.isEmpty()) return

        progressionPlaying.set(true)

        scope.launch {
            val barDurationMs = (60_000L * beatsPerBar) / bpm

            do {
                for ((index, chord) in chords.withIndex()) {
                    if (!progressionPlaying.get()) break

                    onChordStart(index)

                    playChordInternal(chord)

                    val holdMs = (barDurationMs - RELEASE_WINDOW_MS).coerceAtLeast(100)
                    delay(holdMs)

                    releaseActiveVoices()
                    delay(RELEASE_WINDOW_MS)
                }
            } while (loop && progressionPlaying.get())

            if (progressionPlaying.get()) {
                cleanupVoices()
                progressionPlaying.set(false)
                onFinished()
            }
        }
    }

    fun stopProgression() {
        progressionPlaying.set(false)
        stop()
    }

    private suspend fun playChordInternal(chord: Chord) {
        cleanupVoices()

        val frequencies = chordFrequencies(chord)
        val voices = frequencies.map { GuitarKarplusStrong(brightness = CHORD_BRIGHTNESS) }

        synchronized(activeVoices) {
            voices.forEach { engine.addDsp(it); activeVoices.add(it) }
        }

        engine.start()
        voices.forEachIndexed { i, v ->
            v.noteOn(frequencies[i])
            if (i < voices.size - 1) delay(STRUM_DELAY_MS)
        }
    }

    private fun releaseActiveVoices() {
        synchronized(activeVoices) {
            activeVoices.toList().forEach { it.noteOff() }
        }
    }

    private fun cleanupVoices() {
        synchronized(activeVoices) {
            activeVoices.toList().forEach { it.noteOff() }
            activeVoices.toList().forEach { engine.removeDsp(it) }
            activeVoices.clear()
        }
    }

    fun playArpeggio(
        chord: Chord,
        noteDurationMs: Int = 400,
        gapMs: Int = 100,
        direction: String = "up"
    ) {
        stop()

        arpeggioPlaying.set(true)

        val rootMidi = BASE_MIDI + chord.root.semitone
        val midiNotes = chord.quality.intervals.map { rootMidi + it }

        val playOrder = when (direction.lowercase()) {
            "down" -> midiNotes.reversed()
            "both" -> midiNotes + midiNotes.reversed().drop(1)
            else -> midiNotes
        }

        val frequencies = playOrder.map { midiToFreq(it) }

        val voices = frequencies.map { GuitarKarplusStrong(brightness = ARPEGGIO_BRIGHTNESS) }

        synchronized(activeVoices) {
            voices.forEach { voice ->
                engine.addDsp(voice)
                activeVoices.add(voice)
            }
        }

        engine.start()

        stopping.set(false)

        scope.launch {
            for ((index, freq) in frequencies.withIndex()) {
                if (stopping.get()) break

                voices[index].noteOn(freq)
                delay(noteDurationMs.toLong())
                voices[index].noteOff()

                if (index < frequencies.size - 1) {
                    delay(gapMs.toLong())
                }
            }

            delay(TAIL_MS)

            if (!stopping.get()) {
                synchronized(activeVoices) {
                    voices.forEach { voice ->
                        engine.removeDsp(voice)
                        activeVoices.remove(voice)
                    }
                }
            }

            arpeggioPlaying.set(false)
        }
    }
}
