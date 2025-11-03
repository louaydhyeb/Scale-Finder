package com.lddev.scalefinder.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class NotePlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 44100

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
        
        scope.launch {
            track.write(buffer, 0, buffer.size)
            track.play()
            delay(durationMs.toLong())
            track.stop()
            track.release()
        }
    }
}
