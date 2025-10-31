package com.lddev.scalefinder.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import kotlin.math.PI
import kotlin.math.sin
import com.lddev.scalefinder.model.Chord
import java.util.concurrent.atomic.AtomicBoolean

class ChordPlayer {
    private val sampleRate = 44100

    @Volatile
    private var currentTrack: AudioTrack? = null
    @Volatile
    private var writerThread: Thread? = null
    private val stopping = AtomicBoolean(false)

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
}


