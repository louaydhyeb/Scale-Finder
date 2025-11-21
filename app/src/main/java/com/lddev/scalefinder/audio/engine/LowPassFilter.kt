package com.lddev.scalefinder.audio.engine

class LowPassFilter(
    input: Dsp,
    private var cutoffHz: Float = 1200f
) : DspEffect(input) {

    private var prev = 0f

    fun setCutoff(hz: Float) {
        cutoffHz = hz
    }

    override fun compute(): Float {
        val x = next()
        val rc = 1f / (2f * Math.PI.toFloat() * cutoffHz)
        val alpha = (1f / sampleRate) / (rc + 1f / sampleRate)
        prev += alpha * (x - prev)
        return prev
    }
}
