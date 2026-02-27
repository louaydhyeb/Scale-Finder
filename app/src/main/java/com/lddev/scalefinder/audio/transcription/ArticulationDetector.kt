package com.lddev.scalefinder.audio.transcription

import com.lddev.scalefinder.model.Articulation
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Analyses the Basic Pitch **contour** output (264 bins, 3 per semitone) to
 * detect guitar articulations: vibrato, bends, slides, hammer-ons, pull-offs.
 */
object ArticulationDetector {
    private const val MIDI_OFFSET = 21
    private const val BINS_PER_SEMITONE = 3
    private const val CONTOUR_BINS = 264
    private const val CENTS_PER_BIN = 100f / BINS_PER_SEMITONE // ≈ 33.3
    private const val CENTROID_RADIUS = 4

    // Vibrato thresholds
    private const val VIBRATO_MIN_DEPTH_CENTS = 15f
    private const val VIBRATO_MIN_RATE_HZ = 3.5f
    private const val VIBRATO_MAX_RATE_HZ = 9f
    private const val VIBRATO_MIN_FRAMES = 12

    // Bend thresholds
    private const val BEND_MIN_CENTS = 40f

    // Slide: rapid pitch change into a note
    private const val SLIDE_MIN_CENTS = 80f
    private const val SLIDE_MAX_FRAMES = 8

    // Legato: onset probability below which we flag hammer/pull
    private const val LEGATO_ONSET_THRESHOLD = 0.2f

    data class NoteArticulations(
        val articulations: Set<Articulation>,
        val bendSemitones: Float = 0f,
        val vibratoRateHz: Float = 0f,
        val vibratoDepthCents: Float = 0f,
    )

    /**
     * Analyse every [NoteEvent][BasicPitchDetector.NoteEvent] against the
     * contour and onset frames to produce articulation metadata.
     *
     * @param notes          detected note events (sorted by time)
     * @param contourFrames  list of 264-float arrays (one per frame)
     * @param onsetFrames    list of 88-float arrays (one per frame)
     * @param framesPerSec   temporal resolution of the output frames
     */
    fun analyse(
        notes: List<BasicPitchDetector.NoteEvent>,
        contourFrames: List<FloatArray>,
        onsetFrames: List<FloatArray>,
        framesPerSec: Double,
    ): List<NoteArticulations> {
        val secPerFrame = (1.0 / framesPerSec).toFloat()

        return notes.mapIndexed { idx, note ->
            val startFrame =
                (note.startTimeSec / secPerFrame).roundToInt()
                    .coerceIn(0, contourFrames.size - 1)
            val endFrame =
                (note.endTimeSec / secPerFrame).roundToInt()
                    .coerceIn(startFrame + 1, contourFrames.size)
            val centerBin = (note.midiPitch - MIDI_OFFSET) * BINS_PER_SEMITONE + 1

            val pitchTrack = extractPitchTrack(contourFrames, startFrame, endFrame, centerBin)
            val arts = mutableSetOf<Articulation>()
            var bendSemi = 0f
            var vibRate = 0f
            var vibDepth = 0f

            // ── Vibrato ──
            val vib = detectVibrato(pitchTrack, framesPerSec)
            if (vib != null) {
                arts.add(Articulation.VIBRATO)
                vibRate = vib.first
                vibDepth = vib.second
            }

            // ── Bend ──
            val bend = detectBend(pitchTrack)
            if (bend != null) {
                if (bend.second) {
                    arts.add(Articulation.BEND_RELEASE)
                } else {
                    arts.add(Articulation.BEND_UP)
                }
                bendSemi = bend.first
            }

            // ── Slide ──
            detectSlide(pitchTrack)?.let { arts.add(it) }

            // ── Hammer-on / Pull-off ──
            detectLegato(note, idx, notes, onsetFrames, secPerFrame)?.let { arts.add(it) }

            NoteArticulations(arts, bendSemi, vibRate, vibDepth)
        }
    }

    // ── Pitch track extraction ──────────────────────────────────────

    /**
     * For each frame, compute the weighted centroid around [centerBin]
     * within ±[CENTROID_RADIUS] bins. Returns cents offset from center.
     */
    private fun extractPitchTrack(
        contour: List<FloatArray>,
        startFrame: Int,
        endFrame: Int,
        centerBin: Int,
    ): FloatArray {
        val length = endFrame - startFrame
        val track = FloatArray(length)
        val lo = (centerBin - CENTROID_RADIUS).coerceAtLeast(0)
        val hi = (centerBin + CENTROID_RADIUS).coerceAtMost(CONTOUR_BINS - 1)

        for (i in 0 until length) {
            val frame = contour[startFrame + i]
            var sumWeight = 0f
            var sumWeightedPos = 0f
            for (b in lo..hi) {
                val w = frame[b]
                sumWeight += w
                sumWeightedPos += w * (b - centerBin)
            }
            track[i] =
                if (sumWeight > 1e-6f) {
                    (sumWeightedPos / sumWeight) * CENTS_PER_BIN
                } else {
                    0f
                }
        }
        return track
    }

    // ── Vibrato ─────────────────────────────────────────────────────

    /** Returns (rateHz, depthCents) or null. */
    private fun detectVibrato(
        pitchTrack: FloatArray,
        framesPerSec: Double,
    ): Pair<Float, Float>? {
        if (pitchTrack.size < VIBRATO_MIN_FRAMES) return null

        val mean = pitchTrack.average().toFloat()
        val detrended = FloatArray(pitchTrack.size) { pitchTrack[it] - mean }

        val peak = detrended.maxOrNull() ?: 0f
        val trough = detrended.minOrNull() ?: 0f
        val depth = peak - trough
        if (depth < VIBRATO_MIN_DEPTH_CENTS) return null

        var crossings = 0
        for (i in 1 until detrended.size) {
            if (detrended[i - 1] <= 0f && detrended[i] > 0f ||
                detrended[i - 1] >= 0f && detrended[i] < 0f
            ) {
                crossings++
            }
        }
        val rateHz = (crossings / 2f) * framesPerSec.toFloat() / detrended.size
        if (rateHz < VIBRATO_MIN_RATE_HZ || rateHz > VIBRATO_MAX_RATE_HZ) return null

        return rateHz to depth
    }

    // ── Bend ────────────────────────────────────────────────────────

    /** Returns (bendSemitones, isRelease) or null. */
    private fun detectBend(pitchTrack: FloatArray): Pair<Float, Boolean>? {
        if (pitchTrack.size < 4) return null

        val quarter = pitchTrack.size / 4
        val startAvg = pitchTrack.take(quarter.coerceAtLeast(1)).average().toFloat()
        val midAvg =
            pitchTrack.drop(quarter)
                .take(pitchTrack.size / 2).average().toFloat()
        val endAvg = pitchTrack.takeLast(quarter.coerceAtLeast(1)).average().toFloat()

        val rise = midAvg - startAvg
        val fall = midAvg - endAvg

        if (rise > BEND_MIN_CENTS && fall > BEND_MIN_CENTS) {
            return (rise / 100f) to true
        }
        val totalRise = endAvg - startAvg
        if (totalRise > BEND_MIN_CENTS) {
            return (totalRise / 100f) to false
        }
        return null
    }

    // ── Slide ───────────────────────────────────────────────────────

    private fun detectSlide(pitchTrack: FloatArray): Articulation? {
        if (pitchTrack.size < 3) return null
        val onset = pitchTrack.take(SLIDE_MAX_FRAMES.coerceAtMost(pitchTrack.size))
        val shift = onset.last() - onset.first()
        if (abs(shift) < SLIDE_MIN_CENTS) return null
        return if (shift > 0) Articulation.SLIDE_UP else Articulation.SLIDE_DOWN
    }

    // ── Hammer-on / Pull-off ────────────────────────────────────────

    private fun detectLegato(
        note: BasicPitchDetector.NoteEvent,
        noteIndex: Int,
        allNotes: List<BasicPitchDetector.NoteEvent>,
        onsetFrames: List<FloatArray>,
        secPerFrame: Float,
    ): Articulation? {
        val frameIdx =
            (note.startTimeSec / secPerFrame).roundToInt()
                .coerceIn(0, onsetFrames.size - 1)
        val pitchIdx = (note.midiPitch - MIDI_OFFSET).coerceIn(0, 87)
        val onsetProb = onsetFrames[frameIdx][pitchIdx]
        if (onsetProb > LEGATO_ONSET_THRESHOLD) return null

        if (noteIndex == 0) return null
        val prev = allNotes[noteIndex - 1]
        val gap = note.startTimeSec - prev.endTimeSec
        if (gap > 0.08f) return null

        return if (note.midiPitch > prev.midiPitch) {
            Articulation.HAMMER_ON
        } else if (note.midiPitch < prev.midiPitch) {
            Articulation.PULL_OFF
        } else {
            null
        }
    }
}
