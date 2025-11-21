package com.lddev.scalefinder.audio.engine

abstract class Dsp(
    protected val sampleRate: Int = 44100
) {
    abstract fun compute(): Float
}

abstract class DspSource(
    private val attackMs: Float = 2f,
    private val decayMs: Float = 50f,
    private val sustain: Float = 0.7f,
    private val releaseMs: Float = 250f
) : Dsp() {

    private val generator = object : Dsp() {
        override fun compute(): Float = generateSample()
    }

    private val envelope = Adsr(
        input = generator,
        attackMs = attackMs,
        decayMs = decayMs,
        sustain = sustain,
        releaseMs = releaseMs
    )

    final override fun compute(): Float = envelope.compute()

    fun noteOn(freq: Double) {
        onNoteOn(freq)
        envelope.noteOn()
    }

    fun noteOff() {
        onNoteOff()
        envelope.noteOff()
    }

    protected open fun onNoteOn(freq: Double) {}
    protected open fun onNoteOff() {}

    protected abstract fun generateSample(): Float
}

abstract class DspEffect(private val input: Dsp) : Dsp() {
    protected fun next() = input.compute()
}
