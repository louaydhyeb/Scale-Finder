package com.lddev.scalefinder.audio.engine

/**
 * Simple feedback delay effect with adjustable time, feedback, and wet/dry mix.
 *
 * @param input The source DSP that feeds the effect.
 * @param delayMs Initial delay time in milliseconds.
 * @param feedback Initial feedback amount (0 - 0.95). Higher values create longer tails.
 * @param mix Initial wet mix amount (0 - 1). 0 = dry signal only, 1 = fully wet.
 */
class DelayEffect(
    input: Dsp,
    delayMs: Int = 400,
    feedback: Float = 0.35f,
    mix: Float = 0.3f
) : DspEffect(input) {

    private var delaySamples = msToSamples(delayMs)
    private var buffer = FloatArray(delaySamples)
    private var writeIndex = 0

    private var feedbackAmount = clampFeedback(feedback)
    private var mixAmount = clampMix(mix)

    /**
     * Updates the delay time. Smaller values behave like a slap-back delay,
     * larger ones create echo-like repeats.
     */
    fun setDelayMs(ms: Int) {
        val samples = msToSamples(ms)
        if (samples == delaySamples) return

        delaySamples = samples
        buffer = FloatArray(delaySamples)
        writeIndex = 0
    }

    /** Adjusts the feedback amount (0 - 0.95). */
    fun setFeedback(value: Float) {
        feedbackAmount = clampFeedback(value)
    }

    /** Adjusts the wet mix amount (0 - 1). */
    fun setMix(value: Float) {
        mixAmount = clampMix(value)
    }

    override fun compute(): Float {
        val dry = next()
        val delayed = buffer[writeIndex]

        buffer[writeIndex] = dry + delayed * feedbackAmount
        writeIndex++
        if (writeIndex >= delaySamples) {
            writeIndex = 0
        }

        return dry * (1f - mixAmount) + delayed * mixAmount
    }

    private fun msToSamples(ms: Int): Int {
        val clampedMs = ms.coerceAtLeast(1)
        val samples = ((sampleRate.toLong() * clampedMs) / 1000L).toInt()
        return samples.coerceAtLeast(1)
    }

    private fun clampFeedback(value: Float) = value.coerceIn(0f, 0.95f)
    private fun clampMix(value: Float) = value.coerceIn(0f, 1f)
}

