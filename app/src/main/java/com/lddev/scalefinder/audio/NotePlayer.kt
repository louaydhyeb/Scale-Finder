package com.lddev.scalefinder.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class NotePlayer {
    private val sampleRate = 44100

    fun playNote(frequencyHz: Double, durationMs: Int = 500, volume: Float = 0.8f) {
        val sampleRate = 44100
        val length = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(length)
        val phaseIncrement = 2.0 * Math.PI * frequencyHz / sampleRate
        var phase = 0.0

        for (i in buffer.indices) {
            buffer[i] = (sin(phase) * Short.MAX_VALUE * volume).toInt().toShort()
            phase += phaseIncrement
        }

        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            buffer.size * 2,
            AudioTrack.MODE_STATIC
        )

        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep(durationMs.toLong())
        track.stop()
        track.release()
    }

    /**
     * Plays a note with an ADSR envelope (Attack, Decay, Sustain, Release)
     * 
     * @param frequencyHz The frequency of the note in Hz
     * @param durationMs Total duration of the note in milliseconds
     * @param volume Peak volume (0.0 to 1.0)
     * @param attackMs Attack time in milliseconds (time to reach peak)
     * @param decayMs Decay time in milliseconds (time from peak to sustain)
     * @param sustainLevel Sustain level (0.0 to 1.0, as fraction of peak volume)
     * @param releaseMs Release time in milliseconds (time to fade out at end)
     */
    fun playNoteWithEnvelope(
        frequencyHz: Double,
        durationMs: Int = 500,
        volume: Float = 0.8f,
        attackMs: Int = 50,
        decayMs: Int = 100,
        sustainLevel: Float = 0.7f,
        releaseMs: Int = 150
    ) {
        val length = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(length)
        val phaseIncrement = 2.0 * PI * frequencyHz / sampleRate
        var phase = 0.0

        val attackSamples = (sampleRate * (attackMs / 1000.0)).toInt()
        val decaySamples = (sampleRate * (decayMs / 1000.0)).toInt()
        val releaseSamples = (sampleRate * (releaseMs / 1000.0)).toInt()
        val sustainStart = attackSamples + decaySamples
        val releaseStart = (length - releaseSamples).coerceAtLeast(sustainStart)

        for (i in buffer.indices) {
            val envelope = when {
                i < attackSamples -> {
                    // Attack phase: linear ramp from 0 to 1
                    i.toFloat() / attackSamples
                }
                i < sustainStart -> {
                    // Decay phase: linear ramp from 1 to sustainLevel
                    val decayProgress = (i - attackSamples).toFloat() / decaySamples
                    1.0f - (1.0f - sustainLevel) * decayProgress
                }
                i < releaseStart -> {
                    // Sustain phase: hold at sustainLevel
                    sustainLevel
                }
                else -> {
                    // Release phase: linear ramp from sustainLevel to 0
                    val releaseProgress = (i - releaseStart).toFloat() / releaseSamples
                    sustainLevel * (1.0f - releaseProgress)
                }
            }

            buffer[i] = (sin(phase) * Short.MAX_VALUE * volume * envelope).toInt().toShort()
            phase += phaseIncrement
        }

        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            buffer.size * 2,
            AudioTrack.MODE_STATIC
        )

        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep(durationMs.toLong())
        track.stop()
        track.release()
    }

    /**
     * Plays a guitar-like note using the Karplus-Strong algorithm.
     * This produces a realistic plucked string sound by modeling a vibrating string.
     * 
     * @param frequencyHz The frequency of the note in Hz
     * @param durationMs Duration of the note in milliseconds
     * @param volume Volume level (0.0 to 1.0)
     * @param damping Damping factor (0.0 to 1.0) - lower values create brighter sounds with longer decay
     */
    fun playGuitarNote(
        frequencyHz: Double,
        durationMs: Int = 1000,
        volume: Float = 0.8f,
        damping: Float = 0.995f
    ) {
        // Calculate delay line length based on frequency
        val delayLength = (sampleRate / frequencyHz).toInt()
        require(delayLength > 0) { "Frequency too high for sample rate" }
        
        // Initialize delay line with random noise (the "pluck")
        val delayLine = FloatArray(delayLength) {
            (Random.nextFloat() * 2.0f - 1.0f) * volume
        }
        
        val length = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(length)
        var delayIndex = 0
        
        // Karplus-Strong algorithm
        for (i in buffer.indices) {
            // Get current sample from delay line
            val sample = delayLine[delayIndex]
            
            // Low-pass filter: average with next sample (wrapping around)
            val nextIndex = (delayIndex + 1) % delayLength
            val filtered = (sample + delayLine[nextIndex]) * 0.5f
            
            // Apply damping and feed back into delay line
            delayLine[delayIndex] = filtered * damping
            
            // Output the sample
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            
            // Move to next position in delay line
            delayIndex = nextIndex
        }
        
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            buffer.size * 2,
            AudioTrack.MODE_STATIC
        )
        
        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep(durationMs.toLong())
        track.stop()
        track.release()
    }

}


