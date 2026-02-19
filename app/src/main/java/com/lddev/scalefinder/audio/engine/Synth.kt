package com.lddev.scalefinder.audio.engine

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

enum class OscillatorType {
    SAW,
    SQUARE,
    SINE,
    TRIANGLE
}

class Oscillator(
    private val sampleRate: Int,
    var type: OscillatorType = OscillatorType.SINE,
    var detune: Float = 0f // in cents (100 cents = 1 semitone)
) {
    private var phase = 0.0
    
    fun generateSample(frequency: Double): Float {
        // Apply detune: convert cents to frequency multiplier
        val detuneMultiplier = 2.0.pow(detune / 1200.0).toFloat()
        val detunedFreq = frequency * detuneMultiplier
        
        val sample = when (type) {
            OscillatorType.SINE -> sin(phase * 2.0 * PI).toFloat()
            OscillatorType.SAW -> {
                // Sawtooth: phase goes from 0 to 1, output from -1 to 1
                ((phase * 2.0) - 1.0).toFloat()
            }
            OscillatorType.SQUARE -> {
                // Square: 1 if phase < 0.5, -1 otherwise
                if (phase < 0.5) 1f else -1f
            }
            OscillatorType.TRIANGLE -> {
                // Triangle: linear ramp up then down
                if (phase < 0.5) {
                    (phase * 4.0 - 1.0).toFloat()
                } else {
                    (3.0 - phase * 4.0).toFloat()
                }
            }
        }
        
        // Advance phase
        phase += detunedFreq / sampleRate
        if (phase >= 1.0) phase -= 1.0
        
        return sample
    }
    
    fun reset() {
        phase = 0.0
    }
}

class Synth(
    oscillator1Type: OscillatorType = OscillatorType.SAW,
    oscillator1Detune: Float = 0f,
    oscillator2Type: OscillatorType = OscillatorType.SAW,
    oscillator2Detune: Float = 0f
) : DspSource() {
    
    private var frequency = 0.0
    private var oscillator1: Oscillator = Oscillator(sampleRate, oscillator1Type, oscillator1Detune)
    private var oscillator2: Oscillator = Oscillator(sampleRate, oscillator2Type, oscillator2Detune)

    override fun onNoteOn(freq: Double) {
        frequency = freq
        oscillator1.reset()
        oscillator2.reset()
    }
    
    override fun generateSample(): Float {
        val sample1 = oscillator1.generateSample(frequency)
        val sample2 = oscillator2.generateSample(frequency)
        
        // Mix the two oscillators (average them)
        return (sample1 + sample2) * 0.5f
    }
}

