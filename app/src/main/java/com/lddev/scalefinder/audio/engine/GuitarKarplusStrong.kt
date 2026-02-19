package com.lddev.scalefinder.audio.engine

import kotlin.random.Random

class GuitarKarplusStrong(
    private val damping: Float = 0.995f
) : DspSource() {

    // @Volatile ensures the audio thread sees the latest reference immediately
    // after the UI thread swaps the buffer in onNoteOn().
    @Volatile
    private var delayLine = FloatArray(1)

    @Volatile
    private var delayIndex = 0

    @Volatile
    private var active = false

    override fun onNoteOn(freq: Double) {
        val delayLength = (sampleRate / freq).toInt().coerceAtLeast(2)

        // Build the new buffer first, then swap both fields.
        // The audio thread may still be mid-read, so generateSample()
        // uses a local snapshot + modular index to stay in bounds.
        val newLine = FloatArray(delayLength) {
            (Random.nextFloat() * 2f - 1f) * 0.8f // pluck noise
        }
        delayIndex = 0
        delayLine = newLine   // publish after index is reset
        active = true
    }

    override fun generateSample(): Float {
        if (!active) return 0f

        // Local snapshot – even if onNoteOn() swaps the array on another
        // thread mid-call, we keep working with a consistent buffer.
        val buffer = delayLine
        val size = buffer.size

        // Modular clamp – if the index was left over from a longer buffer,
        // wrap it so it is always valid for the current array.
        val idx = delayIndex % size
        val sample = buffer[idx]
        val nextIdx = (idx + 1) % size

        val filtered = (sample + buffer[nextIdx]) * 0.5f
        buffer[idx] = filtered * damping

        delayIndex = nextIdx

        // Auto-kill when amplitude is tiny
        if (kotlin.math.abs(sample) < 1e-10f) active = false

        return sample
    }
}
