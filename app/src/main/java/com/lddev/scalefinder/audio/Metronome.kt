package com.lddev.scalefinder.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
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

    private val clickFrequency = 800.0
    private val accentFrequency = 1200.0

    private var isPlaying = false
    private var beatsPerMinute = 120
    private var beatsPerMeasure = 4

    private val _currentBeat = MutableStateFlow(0)
    val currentBeat: StateFlow<Int> = _currentBeat

    private val clickDuration = 50
    private val clickVolume = 0.3f

    private val normalClickBuffer = generateClick(clickFrequency, clickVolume)
    private val accentClickBuffer = generateClick(accentFrequency, clickVolume)

    private val normalTrack = buildClickTrack(normalClickBuffer)
    private val accentTrack = buildClickTrack(accentClickBuffer)

    private fun generateClick(frequency: Double, volume: Float): ShortArray {
        val samples = (sampleRate * (clickDuration / 1000.0)).toInt()
        val buffer = ShortArray(samples)
        val phaseIncrement = 2.0 * PI * frequency / sampleRate
        var phase = 0.0

        for (i in buffer.indices) {
            val envelope = when {
                i < samples / 8 -> i.toFloat() / (samples / 8f)
                i > samples * 0.7 -> 1.0f - ((i - samples * 0.7).toFloat() / (samples * 0.3f))
                else -> 1.0f
            }

            val sample = sin(phase).toFloat() * envelope
            buffer[i] = (sample * Short.MAX_VALUE * volume).toInt().toShort()
            phase += phaseIncrement
        }

        return buffer
    }

    private fun buildClickTrack(buffer: ShortArray): AudioTrack {
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        }

        val track = builder.build()
        track.write(buffer, 0, buffer.size)
        return track
    }

    private fun playClick(isAccent: Boolean = false) {
        val track = if (isAccent) accentTrack else normalTrack
        try { track.stop() } catch (_: IllegalStateException) { }
        track.setPlaybackHeadPosition(0)
        track.play()
    }

    fun start(initialBeat: Int = 1) {
        if (isPlaying) return
        isPlaying = true

        scope.launch {
            var currentBeat = initialBeat

            _currentBeat.value = currentBeat

            while (isActive && isPlaying) {
                val beatIntervalMs = 60000L / beatsPerMinute
                val isAccent = (currentBeat == 1)
                playClick(isAccent)
                _currentBeat.value = currentBeat

                currentBeat = if (currentBeat >= beatsPerMeasure) 1 else currentBeat + 1
                delay(beatIntervalMs)
            }

            _currentBeat.value = 0
        }
    }

    fun stop() {
        isPlaying = false
    }

    fun setBPM(bpm: Int) {
        beatsPerMinute = bpm.coerceIn(30, 200)
    }

    fun setTimeSignature(beats: Int) {
        beatsPerMeasure = beats.coerceIn(2, 8)
    }

    fun cleanup() {
        stop()
        scope.cancel()
        try { normalTrack.release() } catch (_: Throwable) { }
        try { accentTrack.release() } catch (_: Throwable) { }
    }
}
