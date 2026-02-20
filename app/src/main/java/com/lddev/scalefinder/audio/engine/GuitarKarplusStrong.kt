package com.lddev.scalefinder.audio.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Realistic plucked-string synthesis using extended Karplus-Strong.
 *
 * Improvements over basic KS:
 *  - Pluck-position comb filter on the excitation (removes harmonics at n/pos)
 *  - Tunable one-pole low-pass in feedback (brightness control)
 *  - First-order allpass for fractional-delay tuning accuracy
 *  - Per-sample energy loss derived from a target T60 decay time
 *  - Two biquad peaking-EQ filters modeling guitar-body resonances
 *  - DC blocker to prevent offset accumulation
 *  - ADSR set to sustain=1 so the natural KS decay is unaltered
 */
class GuitarKarplusStrong(
    private val brightness: Float = 0.5f,
    private val pluckPosition: Float = 0.13f,
    private val decaySeconds: Float = 4.0f
) : DspSource(
    attackMs = 1f,
    decayMs = 1f,
    sustain = 1.0f,
    releaseMs = 60f
) {

    @Volatile private var delayLine = FloatArray(1)
    @Volatile private var delayIndex = 0
    @Volatile private var active = false

    private var lpState = 0f

    private var apCoeff = 0f
    private var apPrevIn = 0f
    private var apPrevOut = 0f

    private var loss = 0.999f

    private var dcX1 = 0f
    private var dcY1 = 0f

    private val body1 = Biquad()
    private val body2 = Biquad()

    override fun onNoteOn(freq: Double) {
        val exactDelay = sampleRate.toDouble() / freq
        val n = exactDelay.toInt().coerceAtLeast(2)
        val frac = (exactDelay - n).toFloat()

        apCoeff = if (frac > 0.001f) (1f - frac) / (1f + frac) else 0f
        apPrevIn = 0f
        apPrevOut = 0f

        loss = exp(-6.91 / (decaySeconds * sampleRate)).toFloat()
            .coerceIn(0.990f, 0.99999f)

        lpState = 0f
        dcX1 = 0f
        dcY1 = 0f

        body1.setPeakEq(sampleRate, 110f, 1.5f, 4f)
        body1.reset()
        body2.setPeakEq(sampleRate, 220f, 1.2f, 2f)
        body2.reset()

        delayIndex = 0
        delayLine = shapeExcitation(n)
        active = true
    }

    private fun shapeExcitation(n: Int): FloatArray {
        val buf = FloatArray(n) { Random.nextFloat() * 2f - 1f }

        val pluckSamples = (n * pluckPosition).toInt().coerceIn(1, n - 1)
        for (i in n - 1 downTo pluckSamples) {
            buf[i] -= buf[i - pluckSamples]
        }

        val alpha = (0.2f + brightness * 0.6f).coerceIn(0.1f, 0.8f)
        var prev = 0f
        for (i in buf.indices) {
            buf[i] = prev + alpha * (buf[i] - prev)
            prev = buf[i]
        }

        val peak = buf.maxOfOrNull { abs(it) }?.coerceAtLeast(0.01f) ?: 1f
        val gain = 0.8f / peak
        for (i in buf.indices) buf[i] *= gain

        return buf
    }

    override fun generateSample(): Float {
        if (!active) return 0f

        val buf = delayLine
        val size = buf.size
        val idx = delayIndex % size
        val output = buf[idx]
        val nextIdx = (idx + 1) % size
        val next = buf[nextIdx]

        val blend = 0.5f + (1f - brightness) * 0.35f
        val averaged = output * (1f - blend) + next * blend

        lpState += 0.45f * (averaged - lpState)

        val decayed = lpState * loss

        val tuned: Float
        if (apCoeff != 0f) {
            tuned = apCoeff * decayed + apPrevIn - apCoeff * apPrevOut
            apPrevIn = decayed
            apPrevOut = tuned
        } else {
            tuned = decayed
        }

        buf[idx] = tuned
        delayIndex = nextIdx

        val dc = output - dcX1 + 0.995f * dcY1
        dcX1 = output
        dcY1 = dc

        val bodied = body2.process(body1.process(dc))

        if (abs(output) < 1e-8f && abs(lpState) < 1e-8f) active = false

        return bodied
    }

    private class Biquad {
        private var b0 = 1f; private var b1 = 0f; private var b2 = 0f
        private var a1 = 0f; private var a2 = 0f
        private var x1 = 0f; private var x2 = 0f
        private var y1 = 0f; private var y2 = 0f

        fun setPeakEq(sr: Int, freq: Float, q: Float, gainDb: Float) {
            val a = 10f.pow(gainDb / 40f)
            val w0 = (2.0 * PI * freq / sr).toFloat()
            val sinW = sin(w0)
            val cosW = cos(w0)
            val alpha = sinW / (2f * q)
            val a0 = 1f + alpha / a
            b0 = (1f + alpha * a) / a0
            b1 = (-2f * cosW) / a0
            b2 = (1f - alpha * a) / a0
            a1 = (-2f * cosW) / a0
            a2 = (1f - alpha / a) / a0
        }

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x
            y2 = y1; y1 = if (y.isNaN()) 0f else y
            return y1
        }

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }
    }
}
