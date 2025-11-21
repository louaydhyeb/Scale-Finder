package com.lddev.scalefinder.audio.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioEngine(
    private val sampleRate: Int = 44100
) {
    var isStarted = false
        private set

    // Track and scope are created lazily in start() so the engine
    // can be stopped and restarted safely (e.g. across test runs).
    private var track: AudioTrack? = null
    private var scope: CoroutineScope? = null

    private val dsps = mutableListOf<Dsp>() // polyphonic mixing

    fun addDsp(dsp: Dsp) {
        if (dsps.contains(dsp)) return
        dsps.add(dsp)
    }

    fun removeDsp(dsp: Dsp) {
        dsps.remove(dsp)
    }

    fun start() {
        if (isStarted) return

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val newTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = newTrack
        newTrack.play()
        isStarted = true

        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = newScope
        newScope.launch {
            val buf = ShortArray(1024)
            while (isActive) {
                for (i in buf.indices) {
                    var sample = 0f
                    dsps.forEach { sample += it.compute() }
                    sample = sample.coerceIn(-1f, 1f)
                    buf[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                newTrack.write(buf, 0, buf.size)
            }
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        scope?.cancel()
        scope = null
        try { track?.stop() } catch (_: Throwable) { }
        try { track?.release() } catch (_: Throwable) { }
        track = null
    }

    companion object {
        val instance by lazy { AudioEngine() }
    }
}
