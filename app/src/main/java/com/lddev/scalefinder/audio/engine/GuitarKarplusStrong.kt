package com.lddev.scalefinder.audio.engine

import kotlin.random.Random

class GuitarKarplusStrong(
    private val damping: Float = 0.995f
) : DspSource() {

    private var delayLine = FloatArray(1)
    private var delayIndex = 0
    private var active = false

    override fun onNoteOn(freq: Double) {
        val delayLength = (sampleRate / freq).toInt().coerceAtLeast(1)

        delayLine = FloatArray(delayLength) {
            (Random.nextFloat() * 2f - 1f) * 0.8f // pluck noise
        }
        delayIndex = 0
        active = true
    }

    override fun generateSample(): Float {
        if (!active) return 0f

        val sample = delayLine[delayIndex]
        val nextIndex = (delayIndex + 1) % delayLine.size

        val filtered = (sample + delayLine[nextIndex]) * 0.5f
        delayLine[delayIndex] = filtered * damping

        delayIndex = nextIndex

        // Auto-kill when amplitude is tiny
        if (kotlin.math.abs(sample) < 1e-5f) active = false

        return sample
    }

    fun isActive() = active
}
