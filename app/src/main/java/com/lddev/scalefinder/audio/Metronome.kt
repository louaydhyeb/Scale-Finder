package com.lddev.scalefinder.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class Metronome {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 44100
    
    // Frequencies for click sounds
    private val clickFrequency = 800.0 // Hz for regular clicks
    private val accentFrequency = 1200.0 // Hz for downbeat
    
    private var isPlaying = false
    private var beatsPerMinute = 120
    private var beatsPerMeasure = 4
    
    // Flow to track current beat for visual metronome
    private val _currentBeat = MutableStateFlow(0)
    val currentBeat: StateFlow<Int> = _currentBeat
    
    private val clickDuration = 50 // ms
    private val clickVolume = 0.3f
    
    /**
     * Generate a click sound buffer
     */
    private fun generateClick(frequency: Double, volume: Float): ShortArray {
        val samples = (sampleRate * (clickDuration / 1000.0)).toInt()
        val buffer = ShortArray(samples)
        val phaseIncrement = 2.0 * PI * frequency / sampleRate
        var phase = 0.0
        
        // Generate click with quick attack and decay envelope
        for (i in buffer.indices) {
            val envelope = when {
                i < samples / 8 -> i.toFloat() / (samples / 8f) // Quick attack
                i > samples * 0.7 -> 1.0f - ((i - samples * 0.7).toFloat() / (samples * 0.3f)) // Decay
                else -> 1.0f // Sustain
            }
            
            val sample = sin(phase).toFloat() * envelope
            buffer[i] = (sample * Short.MAX_VALUE * volume).toInt().toShort()
            phase += phaseIncrement
        }
        
        return buffer
    }
    
    /**
     * Play a single click
     */
    private fun playClick(isAccent: Boolean = false) {
        val frequency = if (isAccent) accentFrequency else clickFrequency
        val buffer = generateClick(frequency, clickVolume)
        
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
        
        // AudioTrack will play the buffer and release itself when done
        // We don't need to wait for completion in the metronome loop
    }
    
    /**
     * Start the metronome
     */
    fun start(initialBeat: Int = 1) {
        if (isPlaying) return
        isPlaying = true
        
        scope.launch {
            var currentBeat = initialBeat
            val beatIntervalMs = 60000L / beatsPerMinute
            
            _currentBeat.value = currentBeat
            
            while (isActive && isPlaying) {
                val isAccent = (currentBeat == 1)
                playClick(isAccent)
                _currentBeat.value = currentBeat
                
                currentBeat = if (currentBeat >= beatsPerMeasure) 1 else currentBeat + 1
                delay(beatIntervalMs)
            }
            
            _currentBeat.value = 0
        }
    }
    
    /**
     * Stop the metronome
     */
    fun stop() {
        isPlaying = false
    }
    
    /**
     * Update the tempo (beats per minute)
     */
    fun setBPM(bpm: Int) {
        beatsPerMinute = bpm.coerceIn(30, 200)
    }
    
    /**
     * Update the time signature (beats per measure)
     */
    fun setTimeSignature(beats: Int) {
        beatsPerMeasure = beats.coerceIn(2, 8)
    }
    
    /**
     * Get current BPM
     */
    fun getBPM(): Int = beatsPerMinute
    
    /**
     * Get current time signature
     */
    fun getTimeSignature(): Int = beatsPerMeasure
    
    /**
     * Check if metronome is currently playing
     */
    fun isRunning(): Boolean = isPlaying
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stop()
        scope.cancel()
    }
}