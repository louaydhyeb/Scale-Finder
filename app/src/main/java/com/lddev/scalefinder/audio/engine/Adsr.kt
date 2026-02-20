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

    private var stepA = 0f
    private var stepD = 0f
    private var stepR = 0f

    init {
        stepA = computeStep(attackMs, 1f)
        stepD = computeStep(decayMs, 1f - sustain)
        stepR = computeStep(releaseMs, sustain)
    }

    private fun computeStep(timeMs: Float, range: Float): Float {
        val samples = timeMs.coerceAtLeast(0.01f) * sampleRate / 1000f
        return range / samples
    }

    fun noteOn() {
        stepA = computeStep(attackMs, 1f)
        stepD = computeStep(decayMs, 1f - sustain)
        state = State.ATTACK
    }

    fun noteOff() {
        stepR = computeStep(releaseMs, env.coerceAtLeast(0f))
        state = State.RELEASE
    }

    override fun compute(): Float {
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
