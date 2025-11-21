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
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

class ChordPlayer {
    private val engine = AudioEngine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val activeSynths = mutableListOf<Synth>()
    private val releaseJobs = mutableListOf<Job>()
    private val stopping = AtomicBoolean(false)
    private var arpeggioPlaying = AtomicBoolean(false)

    fun playChord(chord: Chord, durationMs: Int = 1200, volume: Float = 0.6f) {
        stop()

        // Calculate MIDI notes for the chord
        val baseMidi = 60 // C4
        val rootOffset = chord.root.semitone
        val rootMidi = baseMidi + rootOffset
        val intervals = chord.quality.intervals
        val midiNotes = intervals.map { interval -> rootMidi + interval }
        val frequencies = midiNotes.map { midiToFreq(it) }

        // Create a Synth for each note in the chord
        val synths = frequencies.map { freq ->
            Synth()
        }

        // Add all synths to the engine
        synths.forEach { synth ->
            engine.addDsp(LowPassFilter(synth))
            activeSynths.add(synth)
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
                // Remove synths after release completes
                delay(500) // Wait for release envelope
                synths.forEach { synth ->
                    engine.removeDsp(synth)
                    activeSynths.remove(synth)
                }
            }
        }
        releaseJobs.add(releaseJob)
    }

    fun stop() {
        stopping.set(true)
        arpeggioPlaying.set(false)
        
        // Cancel all release jobs
        releaseJobs.forEach { it.cancel() }
        releaseJobs.clear()
        
        // Stop all active synths
        activeSynths.forEach { synth ->
            synth.noteOff()
            engine.removeDsp(synth)
        }
        activeSynths.clear()
    }
    
    fun dispose() {
        stop()
        engine.stop()
        scope.cancel()
    }

    private fun midiToFreq(midi: Int): Double {
        return 440.0 * 2.0.pow((midi - 69) / 12.0)
    }

    /**
     * Plays a chord as an arpeggio (notes played one after another)
     * @param chord The chord to arpeggiate
     * @param noteDurationMs Duration of each note in milliseconds
     * @param gapMs Gap between notes in milliseconds
     * @param direction "up" plays bottom to top, "down" plays top to bottom, "both" plays up then down
     * @param volume Volume level (0.0 to 1.0)
     */
    fun playArpeggio(
        chord: Chord,
        noteDurationMs: Int = 400,
        gapMs: Int = 100,
        direction: String = "up",
        volume: Float = 0.6f
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
        engine.addDsp(LowPassFilter(synth))
        activeSynths.add(synth)
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
                engine.removeDsp(synth)
                activeSynths.remove(synth)
            }
            
            arpeggioPlaying.set(false)
        }
    }

}
