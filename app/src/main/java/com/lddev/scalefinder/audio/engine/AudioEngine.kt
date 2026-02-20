package com.lddev.scalefinder.audio.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class AudioEngine(
    private val sampleRate: Int = 44100
) {
    var isStarted = false
        private set

    private var track: AudioTrack? = null
    private var scope: CoroutineScope? = null

    private val dsps = CopyOnWriteArrayList<Dsp>()

    fun addDsp(dsp: Dsp) {
        if (dsps.contains(dsp)) return
        dsps.add(dsp)
    }

    fun removeDsp(dsp: Dsp) {
        dsps.remove(dsp)
    }

    fun start() {
        if (isStarted) return

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        }

        val newTrack = builder.build()
        track = newTrack
        newTrack.play()
        isStarted = true

        val audioThread = Executors.newSingleThreadExecutor { r ->
            Thread(r, "AudioEngine").apply {
                priority = Thread.MAX_PRIORITY
            }
        }
        val newScope = CoroutineScope(SupervisorJob() + audioThread.asCoroutineDispatcher())
        scope = newScope
        newScope.launch {
            val buf = FloatArray(RENDER_FRAMES)
            while (isActive) {
                for (i in buf.indices) {
                    var sample = 0f
                    for (dsp in dsps) { sample += dsp.compute() }
                    buf[i] = sample.coerceIn(-1f, 1f)
                }
                newTrack.write(buf, 0, buf.size, AudioTrack.WRITE_BLOCKING)
            }
            audioThread.shutdown()
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
        /** 128 frames ≈ 2.9 ms @ 44 100 Hz. */
        private const val RENDER_FRAMES = 128

        val instance by lazy { AudioEngine() }
    }
}
