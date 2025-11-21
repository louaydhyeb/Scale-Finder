package com.lddev.scalefinder.audio.engine

class Adsr(
    input: Dsp,
    private val attackMs: Float = 10f,
    private val decayMs: Float = 50f,
    private val sustain: Float = 0.7f,
    private val releaseMs: Float = 250f
) : DspEffect(input) {

    private enum class State { IDLE, ATTACK, DECAY, SUSTAIN, RELEASE }
    private var state = State.IDLE
    private var env = 0f

    fun noteOn() {
        state = State.ATTACK
    }

    fun noteOff() {
        state = State.RELEASE
    }

    override fun compute(): Float {
        val stepA = 1f / (attackMs * sampleRate / 1000f)
        val stepD = (1f - sustain) / (decayMs * sampleRate / 1000f)
        val stepR = sustain / (releaseMs * sampleRate / 1000f)

        env = when (state) {
            State.ATTACK -> {
                val v = env + stepA
                if (v >= 1f) { state = State.DECAY; 1f } else v
            }
            State.DECAY -> {
                val v = env - stepD
                if (v <= sustain) { state = State.SUSTAIN; sustain } else v
            }
            State.SUSTAIN -> sustain
            State.RELEASE -> {
                val v = env - stepR
                if (v <= 0f) { state = State.IDLE; 0f } else v
            }
            State.IDLE -> 0f
        }

        return next() * env
    }
}
