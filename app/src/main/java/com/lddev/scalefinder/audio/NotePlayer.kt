package com.lddev.scalefinder.audio

import com.lddev.scalefinder.audio.engine.Adsr
import com.lddev.scalefinder.audio.engine.AudioEngine
import com.lddev.scalefinder.audio.engine.GuitarKarplusStrong
import com.lddev.scalefinder.audio.engine.LowPassFilter
import com.lddev.scalefinder.audio.engine.DelayEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NotePlayer {
    val engine = AudioEngine.instance

    /**
     * Plays a guitar-like note using the Karplus-Strong algorithm.
     * This produces a realistic plucked string sound by modeling a vibrating string.
     *
     * @param frequencyHz The frequency of the note in Hz
     * @param durationMs Duration of the note in milliseconds
     * @param volume Volume level (0.0 to 1.0)
     * @param damping Damping factor (0.0 to 1.0) - lower values create brighter sounds with longer decay
     */
    fun playGuitarNote(frequencyHz: Double) {
        engine.start()
        engine.getSourceDsp()?.noteOn(frequencyHz)
        engine.getEnvelope()?.noteOn()
    }
}
