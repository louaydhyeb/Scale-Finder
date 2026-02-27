package com.lddev.scalefinder.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.roundToInt

data class PitchResult(
    val frequency: Float,
    val noteName: String,
    val octave: Int,
    val cents: Float,
)

class PitchDetector(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 4096,
    private val yinThreshold: Float = 0.15f,
) {
    private var audioRecord: AudioRecord? = null
    private var detectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _pitchFlow = MutableSharedFlow<PitchResult?>(replay = 1)
    val pitchFlow: SharedFlow<PitchResult?> = _pitchFlow

    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    @SuppressLint("MissingPermission")
    fun start() {
        if (audioRecord != null) return

        val minBufSize =
            AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
            )
        val actualBufSize = maxOf(bufferSize * Float.SIZE_BYTES, minBufSize)

        audioRecord =
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                actualBufSize,
            ).also { it.startRecording() }

        detectionJob =
            scope.launch {
                val buffer = FloatArray(bufferSize)
                while (isActive) {
                    val read =
                        audioRecord?.read(
                            buffer, 0, bufferSize, AudioRecord.READ_BLOCKING,
                        ) ?: -1
                    if (read > 0) {
                        val freq = detectPitch(buffer)
                        _pitchFlow.emit(if (freq > 0) frequencyToNote(freq) else null)
                    }
                }
            }
    }

    fun stop() {
        detectionJob?.cancel()
        detectionJob = null
        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
        }
        audioRecord?.release()
        audioRecord = null
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    private fun frequencyToNote(freq: Float): PitchResult {
        val midiNote = 12.0 * log2(freq.toDouble() / 440.0) + 69.0
        val roundedMidi = midiNote.roundToInt()
        val cents = ((midiNote - roundedMidi) * 100.0).toFloat()
        val noteIndex = ((roundedMidi % 12) + 12) % 12
        val octave = (roundedMidi / 12) - 1
        return PitchResult(freq, noteNames[noteIndex], octave, cents)
    }

    /**
     * YIN pitch detection algorithm.
     * Returns detected frequency in Hz, or -1 if no clear pitch found.
     */
    private fun detectPitch(buffer: FloatArray): Float {
        val halfBuffer = buffer.size / 2
        val yinBuffer = FloatArray(halfBuffer)

        // Step 1: Difference function
        for (tau in 0 until halfBuffer) {
            var sum = 0f
            for (i in 0 until halfBuffer) {
                val diff = buffer[i] - buffer[i + tau]
                sum += diff * diff
            }
            yinBuffer[tau] = sum
        }

        // Step 2: Cumulative mean normalized difference
        yinBuffer[0] = 1f
        var runningSum = 0f
        for (tau in 1 until halfBuffer) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] =
                if (runningSum == 0f) {
                    1f
                } else {
                    yinBuffer[tau] * tau / runningSum
                }
        }

        // Step 3: Absolute threshold — find the first dip below threshold
        var tauEstimate = -1
        var tau = 2
        while (tau < halfBuffer) {
            if (yinBuffer[tau] < yinThreshold) {
                while (tau + 1 < halfBuffer && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++
                }
                tauEstimate = tau
                break
            }
            tau++
        }

        if (tauEstimate == -1) return -1f

        // Step 4: Parabolic interpolation for sub-sample accuracy
        val betterTau: Float =
            if (tauEstimate in 1 until halfBuffer - 1) {
                val s0 = yinBuffer[tauEstimate - 1]
                val s1 = yinBuffer[tauEstimate]
                val s2 = yinBuffer[tauEstimate + 1]
                val denom = 2f * (2f * s1 - s2 - s0)
                if (denom != 0f) {
                    tauEstimate + (s2 - s0) / denom
                } else {
                    tauEstimate.toFloat()
                }
            } else {
                tauEstimate.toFloat()
            }

        val freq = sampleRate.toFloat() / betterTau
        return if (freq in 60f..1200f) freq else -1f
    }
}
