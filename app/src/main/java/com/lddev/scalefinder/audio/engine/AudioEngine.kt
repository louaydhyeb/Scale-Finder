package com.lddev.scalefinder.audio.engine

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.lazy.LazyRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AudioEngine(
    private val sampleRate: Int = 44100,
    bufferSize: Int = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
) {
    var isStarted = false

    private val track = AudioTrack(
        AudioManager.STREAM_MUSIC,
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize,
        AudioTrack.MODE_STREAM
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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

        track.play()
        isStarted = true

        scope.launch {
            val buf = ShortArray(1024)
            while (isActive) {
                for (i in buf.indices) {
                    var sample = 0f
                    dsps.forEach { sample += it.compute() }
                    sample = sample.coerceIn(-1f, 1f)
                    buf[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
            }
        }
    }

    fun stop() {
        scope.cancel()
        track.stop()
        track.release()
    }

    companion object {
        val instance by lazy { AudioEngine() }
    }
}
