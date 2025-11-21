package com.lddev.scalefinder.audio

import com.lddev.scalefinder.audio.engine.AudioEngine
import com.lddev.scalefinder.audio.engine.GuitarKarplusStrong
import com.lddev.scalefinder.audio.engine.ReverbEffect
import kotlinx.coroutines.*

class NotePlayer {
    private val engine = AudioEngine.instance
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val guitar = GuitarKarplusStrong()
    private val reverb = ReverbEffect(guitar)

    private var releaseJob: Job? = null

    init {
        engine.addDsp(reverb)
    }

    /**
     * Plays a guitar-like note using the Karplus-Strong algorithm.
     * The DSP chain lives inside this player, so calling this method handles
     * starting the engine, triggering the envelope, and scheduling the release automatically.
     *
     * @param frequencyHz The frequency of the note in Hz.
     * @param durationMs How long before the note releases (controls ADSR release trigger).
     */
    fun playGuitarNote(
        frequencyHz: Double,
        durationMs: Int = 1000
    ) {
        engine.start()
        releaseJob?.cancel()
        guitar.noteOn(frequencyHz)
        releaseJob = scope.launch {
            delay(durationMs.toLong().coerceAtLeast(0L))
            guitar.noteOff()
        }
    }

    fun dispose() {
        releaseJob?.cancel()
        scope.cancel()
        engine.removeDsp(reverb)
    }
}
