package com.lddev.scalefinder.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
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

    companion object {
        private const val TAG = "Metronome"
        const val SAMPLE_RATE = 44100
        const val CLICK_FREQ = 800.0
        const val ACCENT_FREQ = 1200.0
        const val CLICK_DURATION_MS = 50
        const val CLICK_VOLUME = 0.3f
        const val DEFAULT_BPM = 120
        const val DEFAULT_BEATS = 4
        const val MIN_BPM = 30
        const val MAX_BPM = 200
        const val MIN_BEATS = 2
        const val MAX_BEATS = 8
    }

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var disposed = false

    private var isPlaying = false
    private var beatsPerMinute = DEFAULT_BPM
    private var beatsPerMeasure = DEFAULT_BEATS

    private val _currentBeat = MutableStateFlow(0)
    val currentBeat: StateFlow<Int> = _currentBeat

    private val normalClickBuffer = generateClick(CLICK_FREQ, CLICK_VOLUME)
    private val accentClickBuffer = generateClick(ACCENT_FREQ, CLICK_VOLUME)

    private val normalTrack: AudioTrack? = buildClickTrack(normalClickBuffer)
    private val accentTrack: AudioTrack? = buildClickTrack(accentClickBuffer)

    private fun generateClick(frequency: Double, volume: Float): ShortArray {
        val samples = (SAMPLE_RATE * (CLICK_DURATION_MS / 1000.0)).toInt()
        val buffer = ShortArray(samples)
        val phaseIncrement = 2.0 * PI * frequency / SAMPLE_RATE
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

    private fun buildClickTrack(buffer: ShortArray): AudioTrack? {
        return try {
            val builder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
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
            track
        } catch (e: Exception) {
            Log.w(TAG, "buildClickTrack failed", e)
            null
        }
    }

    private fun playClick(isAccent: Boolean = false) {
        val track = (if (isAccent) accentTrack else normalTrack) ?: return
        try {
            track.stop()
            track.setPlaybackHeadPosition(0)
            track.play()
        } catch (_: Exception) { }
    }

    fun start(initialBeat: Int = 1) {
        if (disposed) return
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
        beatsPerMinute = bpm.coerceIn(MIN_BPM, MAX_BPM)
    }

    fun setTimeSignature(beats: Int) {
        beatsPerMeasure = beats.coerceIn(MIN_BEATS, MAX_BEATS)
    }

    fun cleanup() {
        stop()
        disposed = true
        scope.cancel()
        try { normalTrack?.release() } catch (_: Throwable) { }
        try { accentTrack?.release() } catch (_: Throwable) { }
    }
}
