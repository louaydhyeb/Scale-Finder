package com.lddev.scalefinder.audio.engine

import kotlin.math.roundToInt

/**
 * Lightweight Schroeder-style reverb built from a pre-delay, multiple comb filters
 * and two all-pass diffusion stages. Designed for mono sources.
 *
 * @param input The DSP feeding the effect.
 * @param preDelayMs Pre-delay time before reflections start (ms).
 * @param decay Decay/feedback amount (0 - 0.95).
 * @param mix Wet mix amount (0 - 1).
 */
class ReverbEffect(
    input: Dsp,
    preDelayMs: Int = 20,
    decay: Float = 0.55f,
    mix: Float = 0.25f
) : DspEffect(input) {

    private val combDelayTimesMs = floatArrayOf(29.7f, 37.1f, 41.1f, 43.7f)
    private val allpassDelayTimesMs = floatArrayOf(5.0f, 1.7f)
    private val allpassFeedback = 0.5f

    private var preDelayBuffer = createBuffer(preDelayMs.toFloat())
    private var preDelayIndex = 0

    private val combBuffers = combDelayTimesMs.map { createBuffer(it) }.toTypedArray()
    private val combIndices = IntArray(combBuffers.size)

    private val allpassBuffers = allpassDelayTimesMs.map { createBuffer(it) }.toTypedArray()
    private val allpassIndices = IntArray(allpassBuffers.size)

    private var decayAmount = clampDecay(decay)
    private var mixAmount = clampMix(mix)

    /** Sets the pre-delay time in milliseconds. */
    fun setPreDelay(ms: Int) {
        preDelayBuffer = createBuffer(ms.toFloat())
        preDelayIndex = 0
    }

    /** Sets the decay/feedback amount (0 - 0.95). */
    fun setDecay(value: Float) {
        decayAmount = clampDecay(value)
    }

    /** Sets the wet mix amount (0 - 1). */
    fun setMix(value: Float) {
        mixAmount = clampMix(value)
    }

    override fun compute(): Float {
        val dry = next()
        val predelayed = processPreDelay(dry)
        val combOut = processCombs(predelayed)
        val wet = processAllpass(combOut)

        return dry * (1f - mixAmount) + wet * mixAmount
    }

    private fun processPreDelay(input: Float): Float {
        if (preDelayBuffer.isEmpty()) return input
        val output = preDelayBuffer[preDelayIndex]
        preDelayBuffer[preDelayIndex] = input
        preDelayIndex = (preDelayIndex + 1) % preDelayBuffer.size
        return output
    }

    private fun processCombs(input: Float): Float {
        var sum = 0f
        for (i in combBuffers.indices) {
            val buffer = combBuffers[i]
            if (buffer.isEmpty()) continue
            val idx = combIndices[i]
            val delayed = buffer[idx]
            buffer[idx] = input + delayed * decayAmount
            combIndices[i] = (idx + 1) % buffer.size
            sum += delayed
        }
        val activeBuffers = combBuffers.count { it.isNotEmpty() }.coerceAtLeast(1)
        return sum / activeBuffers
    }

    private fun processAllpass(input: Float): Float {
        var sample = input
        for (i in allpassBuffers.indices) {
            val buffer = allpassBuffers[i]
            if (buffer.isEmpty()) continue
            val idx = allpassIndices[i]
            val bufOut = buffer[idx]
            val newVal = sample + bufOut * allpassFeedback
            buffer[idx] = newVal
            sample = bufOut - newVal * allpassFeedback
            allpassIndices[i] = (idx + 1) % buffer.size
        }
        return sample
    }

    private fun createBuffer(ms: Float): FloatArray {
        val samples = msToSamples(ms)
        return FloatArray(samples)
    }

    private fun msToSamples(ms: Float): Int {
        val clampedMs = ms.coerceAtLeast(0.2f)
        val samples = ((sampleRate * clampedMs) / 1000f).roundToInt()
        return samples.coerceAtLeast(1)
    }

    private fun clampDecay(value: Float) = value.coerceIn(0f, 0.95f)
    private fun clampMix(value: Float) = value.coerceIn(0f, 1f)
}

