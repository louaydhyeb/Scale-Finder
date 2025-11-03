package com.lddev.scalefinder.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import kotlin.math.PI
import kotlin.math.sin
import com.lddev.scalefinder.model.Chord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ChordPlayer {
    private val sampleRate = 44100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var currentTrack: AudioTrack? = null
    @Volatile
    private var writerThread: Thread? = null
    private val stopping = AtomicBoolean(false)
    private var arpeggioPlaying = AtomicBoolean(false)

    fun playChord(chord: Chord, durationMs: Int = 1200, volume: Float = 0.6f) {
        stop()

        val samples = generateChordPcm(chord, durationMs)
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            minBuffer,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        currentTrack = track

        track.setVolume(volume.coerceIn(0f, 1f))
        track.play()

        stopping.set(false)
        val writer = Thread {
            var offset = 0
            val chunk = minBuffer / 2 // 16-bit mono
            while (!stopping.get() && offset < samples.size) {
                val toWrite = minOf(chunk, samples.size - offset)
                val wrote = track.write(samples, offset, toWrite)
                if (wrote <= 0) break
                offset += wrote
            }
            try {
                track.stop()
            } catch (_: Throwable) {}
            track.release()
        }
        writer.isDaemon = true
        writer.start()
        writerThread = writer
    }

    fun stop() {
        stopping.set(true)
        arpeggioPlaying.set(false)
        writerThread?.interrupt()
        writerThread = null
        currentTrack?.let { t ->
            try { t.stop() } catch (_: Throwable) {}
            try { t.release() } catch (_: Throwable) {}
        }
        currentTrack = null
    }

    private fun generateChordPcm(chord: Chord, durationMs: Int): ShortArray {
        val length = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(length)

        // Choose a pleasant register: base octave around C4=60
        val baseMidi = 60 // C4
        val rootOffset = chord.root.semitone // C=0 .. B=11
        val rootMidi = baseMidi + rootOffset

        val intervals = chord.quality.intervals
        val midiNotes = intervals.map { interval -> rootMidi + interval }

        val twoPi = 2.0 * PI
        val amplitudes = midiNotes.map { 1.0 / midiNotes.size } // equal mix
        val freqs = midiNotes.map { midiToFreq(it) }

        // 20ms attack/release envelope
        val attack = (0.02 * sampleRate).toInt().coerceAtLeast(1)
        val release = (0.04 * sampleRate).toInt().coerceAtLeast(1)

        for (i in 0 until length) {
            var sample = 0.0
            val t = i / sampleRate.toDouble()
            for (v in freqs.indices) {
                sample += amplitudes[v] * sin(twoPi * freqs[v] * t)
            }
            // Apply simple envelope
            val amp = when {
                i < attack -> i / attack.toDouble()
                i > length - release -> (length - i) / release.toDouble()
                else -> 1.0
            }
            val s = (sample * amp * Short.MAX_VALUE).toInt()
            buffer[i] = s.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return buffer
    }

    private fun midiToFreq(midi: Int): Double {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
    }

    /**
     * Plays a chord as an arpeggio (notes played one after another)
     * @param chord The chord to arpeggiate
     * @param noteDurationMs Duration of each note in milliseconds
     * @param gapMs Gap between notes in milliseconds
     * @param direction "up" plays bottom to top, "down" plays top to bottom, "both" plays up then down
     * @param volume Volume level (0.0 to 1.0)
     */
    fun playArpeggio(
        chord: Chord,
        noteDurationMs: Int = 400,
        gapMs: Int = 100,
        direction: String = "up",
        volume: Float = 0.6f
    ) {
        // Stop any current playback
        stop()
        
        if (arpeggioPlaying.get()) return
        arpeggioPlaying.set(true)

        // Calculate MIDI notes for the chord
        val baseMidi = 60 // C4
        val rootOffset = chord.root.semitone
        val rootMidi = baseMidi + rootOffset
        val intervals = chord.quality.intervals
        val midiNotes = intervals.map { interval -> rootMidi + interval }

        // Determine play order based on direction
        val playOrder = when (direction.lowercase()) {
            "down" -> midiNotes.reversed()
            "both" -> midiNotes + midiNotes.reversed().drop(1) // up then down, skip duplicate root
            else -> midiNotes // "up"
        }

        // Generate entire arpeggio as one continuous sample to avoid clicks
        val samples = generateArpeggioPcm(playOrder, noteDurationMs, gapMs)
        
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            minBuffer,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        
        currentTrack = track
        track.setVolume(volume.coerceIn(0f, 1f))
        track.play()

        stopping.set(false)
        val writer = Thread {
            var offset = 0
            val chunk = minBuffer / 2
            while (!stopping.get() && offset < samples.size) {
                val toWrite = minOf(chunk, samples.size - offset)
                val wrote = track.write(samples, offset, toWrite)
                if (wrote <= 0) break
                offset += wrote
            }
            try {
                track.stop()
            } catch (_: Throwable) {}
            try {
                track.release()
            } catch (_: Throwable) {}
            currentTrack = null
        }
        writer.isDaemon = true
        writer.start()
        writerThread = writer
        
        // Reset arpeggio flag when done
        scope.launch {
            val totalDuration = (playOrder.size * noteDurationMs) + ((playOrder.size - 1) * gapMs)
            delay(totalDuration.toLong())
            arpeggioPlaying.set(false)
        }
    }

    private fun generateArpeggioPcm(midiNotes: List<Int>, noteDurationMs: Int, gapMs: Int): ShortArray {
        val noteLength = (sampleRate * (noteDurationMs / 1000.0)).toInt()
        val gapLength = (sampleRate * (gapMs / 1000.0)).toInt()
        val totalLength = (midiNotes.size * noteLength) + ((midiNotes.size - 1) * gapLength)
        val buffer = ShortArray(totalLength)
        val twoPi = 2.0 * PI

        // Use the same envelope as chord player for smooth transitions
        val attack = (0.02 * sampleRate).toInt().coerceAtLeast(1)
        val release = (0.04 * sampleRate).toInt().coerceAtLeast(1)

        var bufferIndex = 0
        
        for ((noteIndex, midiNote) in midiNotes.withIndex()) {
            val frequency = midiToFreq(midiNote)
            
            // Generate note samples
            for (i in 0 until noteLength) {
                val globalIndex = bufferIndex + i
                val t = globalIndex / sampleRate.toDouble()
                val sample = sin(twoPi * frequency * t)
                
                // Apply smooth envelope - same as chord player
                val amp = when {
                    i < attack -> i / attack.toDouble()
                    i > noteLength - release -> (noteLength - i) / release.toDouble()
                    else -> 1.0
                }
                
                val s = (sample * amp * Short.MAX_VALUE).toInt()
                buffer[globalIndex] = s.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            
            bufferIndex += noteLength
            
            // Add gap (silence) between notes, except after last note
            // Gap is already zero-filled, so no clicks
            if (noteIndex < midiNotes.size - 1) {
                bufferIndex += gapLength
            }
        }

        return buffer
    }
}
