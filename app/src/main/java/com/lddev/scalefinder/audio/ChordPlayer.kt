package com.lddev.scalefinder.audio

import com.lddev.scalefinder.audio.engine.AudioEngine
import com.lddev.scalefinder.audio.engine.LowPassFilter
import com.lddev.scalefinder.audio.engine.Synth
import com.lddev.scalefinder.model.Chord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

class ChordPlayer {
    private val engine = AudioEngine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    @Volatile
    private var activeSynths = mutableListOf<Synth>()
    @Volatile
    private var activeFilters = mutableListOf<LowPassFilter>()
    private val releaseJobs = mutableListOf<Job>()
    private val stopping = AtomicBoolean(false)
    private var arpeggioPlaying = AtomicBoolean(false)
    private val progressionPlaying = AtomicBoolean(false)

    companion object {
        /** Time reserved at the end of each bar for the release envelope. */
        private const val RELEASE_WINDOW_MS = 150L
    }

    fun playChord(chord: Chord, durationMs: Int = 1200) {
        stop()

        // Calculate MIDI notes for the chord
        val baseMidi = 60 // C4
        val rootOffset = chord.root.semitone
        val rootMidi = baseMidi + rootOffset
        val intervals = chord.quality.intervals
        val midiNotes = intervals.map { interval -> rootMidi + interval }
        val frequencies = midiNotes.map { midiToFreq(it) }

        // Create a Synth for each note in the chord
        val synths = frequencies.map { _ ->
            Synth()
        }
        
        // Create filters for each synth
        val filters = synths.map { synth ->
            LowPassFilter(synth)
        }

        // Add all filters to the engine (synchronized)
        synchronized(activeFilters) {
            filters.forEach { filter ->
                engine.addDsp(filter)
                activeFilters.add(filter)
            }
            synths.forEach { synth ->
                activeSynths.add(synth)
            }
        }

        // Start the engine
        engine.start()

        // Trigger all notes
        synths.forEachIndexed { index, synth ->
            synth.noteOn(frequencies[index])
        }

        // Schedule note releases
        stopping.set(false)
        val releaseJob = scope.launch {
            delay(durationMs.toLong().coerceAtLeast(0L))
            if (!stopping.get()) {
                synths.forEach { synth ->
                    synth.noteOff()
                }
                // Remove filters after release completes
                delay(500) // Wait for release envelope
                synchronized(activeFilters) {
                    filters.forEach { filter ->
                        engine.removeDsp(filter)
                        activeFilters.remove(filter)
                    }
                    synths.forEach { synth ->
                        activeSynths.remove(synth)
                    }
                }
            }
        }
        releaseJobs.add(releaseJob)
    }

    fun stop() {
        stopping.set(true)
        arpeggioPlaying.set(false)
        progressionPlaying.set(false)
        
        // Cancel all release jobs
        releaseJobs.forEach { it.cancel() }
        releaseJobs.clear()
        
        // Stop all active synths and remove filters (synchronized)
        cleanupSynths()
    }

    private fun midiToFreq(midi: Int): Double {
        return 440.0 * 2.0.pow((midi - 69) / 12.0)
    }

    /**
     * Plays a full chord progression in tempo.
     * Each chord sustains for one bar (beatsPerBar beats at the given BPM).
     *
     * @param chords       The chords to play sequentially.
     * @param bpm          Tempo in beats per minute.
     * @param beatsPerBar  How many beats each chord lasts (default 4 = one bar of 4/4).
     * @param loop         Whether to repeat from the beginning when the end is reached.
     * @param onChordStart Called on the main/default dispatcher with the index of the
     *                     chord that just started playing (use to update UI highlights).
     * @param onFinished   Called when the progression finishes (not called if stopped early).
     */
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

                    // Play the chord (non-blocking helper that sets up synths and triggers noteOn)
                    playChordInternal(chord)

                    // Hold for one bar minus a small release window
                    val holdMs = (barDurationMs - RELEASE_WINDOW_MS).coerceAtLeast(100)
                    delay(holdMs)

                    // Release the notes so they fade before the next chord
                    releaseActiveSynths()
                    delay(RELEASE_WINDOW_MS)
                }
            } while (loop && progressionPlaying.get())

            if (progressionPlaying.get()) {
                // Natural end (not user-stopped)
                cleanupSynths()
                progressionPlaying.set(false)
                onFinished()
            }
        }
    }

    /** Stops a playing progression (or any playback). */
    fun stopProgression() {
        progressionPlaying.set(false)
        stop()
    }

    // ── internal helpers for progression ──────────────────────────────

    private fun playChordInternal(chord: Chord) {
        // Tear down previous synths before creating new ones
        cleanupSynths()

        val baseMidi = 60
        val rootMidi = baseMidi + chord.root.semitone
        val frequencies = chord.quality.intervals.map { midiToFreq(rootMidi + it) }

        val synths = frequencies.map { Synth() }
        val filters = synths.map { LowPassFilter(it) }

        synchronized(activeFilters) {
            filters.forEach { engine.addDsp(it); activeFilters.add(it) }
            synths.forEach { activeSynths.add(it) }
        }

        engine.start()
        synths.forEachIndexed { i, s -> s.noteOn(frequencies[i]) }
    }

    private fun releaseActiveSynths() {
        synchronized(activeFilters) {
            activeSynths.toList().forEach { it.noteOff() }
        }
    }

    private fun cleanupSynths() {
        synchronized(activeFilters) {
            activeSynths.toList().forEach { it.noteOff() }
            activeFilters.toList().forEach { engine.removeDsp(it) }
            activeSynths.clear()
            activeFilters.clear()
        }
    }

    /**
     * Plays a chord as an arpeggio (notes played one after another)
     * @param chord The chord to arpeggiate
     * @param noteDurationMs Duration of each note in milliseconds
     * @param gapMs Gap between notes in milliseconds
     * @param direction "up" plays bottom to top, "down" plays top to bottom, "both" plays up then down
     */
    fun playArpeggio(
        chord: Chord,
        noteDurationMs: Int = 400,
        gapMs: Int = 100,
        direction: String = "up"
    ) {
        // Stop any current playback
        stop()
        
        if (arpeggioPlaying.get()) return
        arpeggioPlaying.set(true)

        // Calculate MIDI notes for the chord
        val baseMidi = 60 // C4
        val rootOffset = chord.root.semitone
        val rootMidi = baseMidi + rootOffset
        val intervals = chord.quality.intervals
        val midiNotes = intervals.map { interval -> rootMidi + interval }

        // Determine play order based on direction
        val playOrder = when (direction.lowercase()) {
            "down" -> midiNotes.reversed()
            "both" -> midiNotes + midiNotes.reversed().drop(1) // up then down, skip duplicate root
            else -> midiNotes // "up"
        }

        val frequencies = playOrder.map { midiToFreq(it) }
        
        // Create a single Synth for the arpeggio
        val synth = Synth()
        val filter = LowPassFilter(synth)
        
        synchronized(activeFilters) {
            engine.addDsp(filter)
            activeFilters.add(filter)
            activeSynths.add(synth)
        }
        
        engine.start()

        stopping.set(false)
        
        // Play notes sequentially
        scope.launch {
            for ((index, freq) in frequencies.withIndex()) {
                if (stopping.get()) break
                
                synth.noteOn(freq)
                delay(noteDurationMs.toLong())
                synth.noteOff()
                
                // Add gap between notes (except after last note)
                if (index < frequencies.size - 1) {
                    delay(gapMs.toLong())
                }
            }
            
            // Wait for release envelope to complete
            delay(500)
            
            if (!stopping.get()) {
                synchronized(activeFilters) {
                    engine.removeDsp(filter)
                    activeFilters.remove(filter)
                    activeSynths.remove(synth)
                }
            }
            
            arpeggioPlaying.set(false)
        }
    }

}
