package com.lddev.scalefinder.audio.transcription

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Polyphonic pitch detector powered by Spotify's Basic Pitch TFLite model.
 *
 * Model contract (nmp.tflite shipped with basic-pitch):
 *   Input  : [1, 43 844, 1]   — ~1.99 s of raw 22 050 Hz mono audio
 *   Output 0: [1, 172, 88]    — onset  posteriors  (MIDI 21–108)
 *   Output 1: [1, 172, 88]    — note   posteriors  (MIDI 21–108)
 *   Output 2: [1, 172, 264]   — contour (3 bins / semitone, used for articulations)
 *
 * Long files are split into non-overlapping windows of 43 844 samples;
 * the last window is zero-padded.
 */
class BasicPitchDetector(private val context: Context) {

    companion object {
        private const val MODEL_FILENAME = "basic_pitch.tflite"
        private const val MIDI_OFFSET = 21
        private const val N_PITCHES = 88
        private const val AUDIO_WINDOW = 43_844
        private const val FRAMES_PER_WINDOW = 172
        private const val SAMPLE_RATE = 22_050
        private const val NOTE_THRESHOLD = 0.5f
        private const val ONSET_THRESHOLD = 0.3f
        private const val MIN_NOTE_DURATION_SEC = 0.05f
        private const val CONTOUR_BINS = 264
        const val FRAMES_PER_SEC: Double =
            FRAMES_PER_WINDOW.toDouble() * SAMPLE_RATE / AUDIO_WINDOW
    }

    data class NoteEvent(
        val midiPitch: Int,
        val startTimeSec: Float,
        val endTimeSec: Float,
        val confidence: Float,
        val articulations: Set<com.lddev.scalefinder.model.Articulation> = emptySet(),
        val bendSemitones: Float = 0f,
        val vibratoRateHz: Float = 0f,
        val vibratoDepthCents: Float = 0f
    )

    data class DetectionResult(
        val notes: List<NoteEvent>,
        val contourFrames: List<FloatArray>,
        val onsetFrames: List<FloatArray>
    )

    private var interpreter: Interpreter? = null

    fun isModelAvailable(): Boolean = try {
        context.assets.openFd(MODEL_FILENAME).close()
        true
    } catch (_: IOException) {
        false
    }

    fun initialize() {
        val model = loadModelFile()
        val options = Interpreter.Options().apply { setNumThreads(4) }
        interpreter = Interpreter(model, options)
    }

    /**
     * Detect notes **with articulations** in the given audio
     * (must be 22 050 Hz mono float).
     */
    fun detect(audio: FloatArray): List<NoteEvent> {
        val raw = detectRaw(audio)
        val articulationInfo = ArticulationDetector.analyse(
            raw.notes, raw.contourFrames, raw.onsetFrames, FRAMES_PER_SEC
        )
        return raw.notes.zip(articulationInfo) { note, art ->
            note.copy(
                articulations = art.articulations,
                bendSemitones = art.bendSemitones,
                vibratoRateHz = art.vibratoRateHz,
                vibratoDepthCents = art.vibratoDepthCents
            )
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    // ── Raw detection (notes + contour + onsets) ────────────────────

    private fun detectRaw(audio: FloatArray): DetectionResult {
        val interp = interpreter
            ?: throw IllegalStateException("Call initialize() before detect()")

        val allNoteFrames = mutableListOf<FloatArray>()
        val allOnsetFrames = mutableListOf<FloatArray>()
        val allContourFrames = mutableListOf<FloatArray>()

        var offset = 0
        while (offset < audio.size) {
            val chunk = FloatArray(AUDIO_WINDOW)
            val toCopy = minOf(AUDIO_WINDOW, audio.size - offset)
            System.arraycopy(audio, offset, chunk, 0, toCopy)

            val windowResult = inferWindow(interp, chunk)
            allNoteFrames.addAll(windowResult.noteFrames)
            allOnsetFrames.addAll(windowResult.onsetFrames)
            allContourFrames.addAll(windowResult.contourFrames)

            offset += AUDIO_WINDOW
        }

        val notes = postProcess(allNoteFrames, allOnsetFrames)
        return DetectionResult(notes, allContourFrames, allOnsetFrames)
    }

    // ── Single-window inference ─────────────────────────────────────

    private data class WindowResult(
        val noteFrames: List<FloatArray>,
        val onsetFrames: List<FloatArray>,
        val contourFrames: List<FloatArray>
    )

    private fun inferWindow(interp: Interpreter, chunk: FloatArray): WindowResult {
        val inputBuffer = ByteBuffer
            .allocateDirect(4 * AUDIO_WINDOW)
            .order(ByteOrder.nativeOrder())
        for (s in chunk) inputBuffer.putFloat(s)
        inputBuffer.rewind()

        val onsetOut = Array(1) { Array(FRAMES_PER_WINDOW) { FloatArray(N_PITCHES) } }
        val noteOut = Array(1) { Array(FRAMES_PER_WINDOW) { FloatArray(N_PITCHES) } }
        val contourOut = Array(1) { Array(FRAMES_PER_WINDOW) { FloatArray(CONTOUR_BINS) } }

        val outputs = mapOf<Int, Any>(
            0 to onsetOut,
            1 to noteOut,
            2 to contourOut
        )

        interp.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
        return WindowResult(
            noteOut[0].toList(),
            onsetOut[0].toList(),
            contourOut[0].toList()
        )
    }

    // ── Post-processing ─────────────────────────────────────────────

    private fun postProcess(
        noteFrames: List<FloatArray>,
        onsetFrames: List<FloatArray>
    ): List<NoteEvent> {
        val events = mutableListOf<NoteEvent>()
        val nFrames = noteFrames.size
        val secPerFrame = (1.0 / FRAMES_PER_SEC).toFloat()

        for (pitch in 0 until N_PITCHES) {
            var active = false
            var startFrame = 0
            var peakConf = 0f

            for (t in 0 until nFrames) {
                val noteProb = noteFrames[t][pitch]
                val onsetProb = onsetFrames[t][pitch]

                if (noteProb > NOTE_THRESHOLD) {
                    if (!active || onsetProb > ONSET_THRESHOLD) {
                        if (active) {
                            emitNote(pitch, startFrame, t, peakConf, secPerFrame, events)
                        }
                        active = true
                        startFrame = t
                        peakConf = noteProb
                    } else {
                        peakConf = maxOf(peakConf, noteProb)
                    }
                } else if (active) {
                    emitNote(pitch, startFrame, t, peakConf, secPerFrame, events)
                    active = false
                }
            }
            if (active) {
                emitNote(pitch, startFrame, nFrames, peakConf, secPerFrame, events)
            }
        }
        return events.sortedBy { it.startTimeSec }
    }

    private fun emitNote(
        pitch: Int,
        startFrame: Int,
        endFrame: Int,
        confidence: Float,
        secPerFrame: Float,
        out: MutableList<NoteEvent>
    ) {
        val startSec = startFrame * secPerFrame
        val endSec = endFrame * secPerFrame
        if (endSec - startSec >= MIN_NOTE_DURATION_SEC) {
            out.add(NoteEvent(pitch + MIDI_OFFSET, startSec, endSec, confidence))
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fd = context.assets.openFd(MODEL_FILENAME)
        FileInputStream(fd.fileDescriptor).use { fis ->
            return fis.channel.map(
                FileChannel.MapMode.READ_ONLY,
                fd.startOffset,
                fd.declaredLength
            )
        }
    }
}
