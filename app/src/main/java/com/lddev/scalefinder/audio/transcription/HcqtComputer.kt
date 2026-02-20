package com.lddev.scalefinder.audio.transcription

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes the Harmonic Constant-Q Transform (HCQT) used as input features for
 * the Basic Pitch neural network. Uses a pseudo-CQT approach: one FFT per frame,
 * then interpolates FFT bins to log-frequency CQT bins for each harmonic.
 */
class HcqtComputer(
    private val sampleRate: Int = 22050,
    private val hopLength: Int = 256,
    private val fftSize: Int = 8192,
    private val binsPerOctave: Int = 36,
    private val nBins: Int = 264,
    private val fMin: Double = 32.7,
    private val harmonics: DoubleArray = doubleArrayOf(0.5, 1.0, 2.0, 3.0, 4.0, 5.0)
) {
    private val nyquist = sampleRate / 2.0
    private val cqtFreqs = DoubleArray(nBins) { k -> fMin * 2.0.pow(k.toDouble() / binsPerOctave) }
    private val window = FloatArray(fftSize) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / fftSize))).toFloat()
    }

    val framesPerSecond: Double get() = sampleRate.toDouble() / hopLength

    fun frameCount(audioLength: Int): Int =
        maxOf(0, (audioLength - fftSize) / hopLength + 1)

    /**
     * Compute HCQT features from raw audio samples (expected at [sampleRate] Hz).
     * @return [nHarmonics][nFrames][nBins] array, log-amplitude scaled.
     */
    fun compute(audio: FloatArray): Array<Array<FloatArray>> {
        val nFrames = frameCount(audio.size)
        if (nFrames == 0) return Array(harmonics.size) { emptyArray() }

        val result = Array(harmonics.size) { Array(nFrames) { FloatArray(nBins) } }
        val re = FloatArray(fftSize)
        val im = FloatArray(fftSize)
        val magnitudes = FloatArray(fftSize / 2)

        for (frame in 0 until nFrames) {
            val start = frame * hopLength

            for (i in 0 until fftSize) {
                re[i] = if (start + i < audio.size) audio[start + i] * window[i] else 0f
                im[i] = 0f
            }
            fft(re, im)

            for (i in magnitudes.indices) {
                magnitudes[i] = sqrt(re[i] * re[i] + im[i] * im[i])
            }

            for (h in harmonics.indices) {
                val harmonic = harmonics[h]
                for (k in 0 until nBins) {
                    val freq = cqtFreqs[k] * harmonic
                    if (freq >= nyquist) {
                        result[h][frame][k] = 0f
                    } else {
                        val fftBin = freq * fftSize / sampleRate
                        val binLow = fftBin.toInt()
                        val binHigh = binLow + 1
                        val frac = (fftBin - binLow).toFloat()

                        val magLow =
                            if (binLow in magnitudes.indices) magnitudes[binLow] else 0f
                        val magHigh =
                            if (binHigh in magnitudes.indices) magnitudes[binHigh] else 0f

                        val mag = magLow * (1f - frac) + magHigh * frac
                        result[h][frame][k] = kotlin.math.ln(1f + mag)
                    }
                }
            }
        }
        return result
    }

    /** In-place radix-2 Cooley-Tukey FFT. Arrays must be power-of-2 length. */
    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var tmp = re[i]; re[i] = re[j]; re[j] = tmp
                tmp = im[i]; im[i] = im[j]; im[j] = tmp
            }
        }

        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang).toFloat()
            val wIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f
                var curIm = 0f
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val halfIdx = i + k + len / 2
                    val vRe = re[halfIdx] * curRe - im[halfIdx] * curIm
                    val vIm = re[halfIdx] * curIm + im[halfIdx] * curRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[halfIdx] = uRe - vRe
                    im[halfIdx] = uIm - vIm
                    val newRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = newRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}
