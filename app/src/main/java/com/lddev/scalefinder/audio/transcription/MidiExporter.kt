package com.lddev.scalefinder.audio.transcription

import com.lddev.scalefinder.model.Articulation
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Writes a Standard MIDI File (SMF Type-0, single track) from detected
 * [BasicPitchDetector.NoteEvent]s, including pitch-bend messages for
 * vibrato and bends.
 */
object MidiExporter {
    private const val TICKS_PER_BEAT = 480
    private const val DEFAULT_BPM = 120
    private const val MICROSEC_PER_BEAT = 60_000_000 / DEFAULT_BPM
    private const val PITCH_BEND_CENTER = 8192
    private const val PITCH_BEND_RANGE_SEMI = 2
    private const val VIBRATO_RESOLUTION_TICKS = 10

    fun export(notes: List<BasicPitchDetector.NoteEvent>): ByteArray {
        val trackBytes = buildTrack(notes)
        val out = ByteArrayOutputStream(14 + 8 + trackBytes.size)

        out.writeAscii("MThd")
        out.writeInt32(6)
        out.writeInt16(0)
        out.writeInt16(1)
        out.writeInt16(TICKS_PER_BEAT)

        out.writeAscii("MTrk")
        out.writeInt32(trackBytes.size)
        out.write(trackBytes)

        return out.toByteArray()
    }

    fun exportTo(
        notes: List<BasicPitchDetector.NoteEvent>,
        outputStream: OutputStream,
    ) {
        outputStream.write(export(notes))
        outputStream.flush()
    }

    // ── Track builder ───────────────────────────────────────────────

    private sealed class MidiEv(val tick: Int) {
        class NoteOn(tick: Int, val pitch: Int, val vel: Int) : MidiEv(tick)

        class NoteOff(tick: Int, val pitch: Int) : MidiEv(tick)

        class PitchBend(tick: Int, val value: Int) : MidiEv(tick)
    }

    private fun buildTrack(notes: List<BasicPitchDetector.NoteEvent>): ByteArray {
        val ticksPerSec = TICKS_PER_BEAT.toDouble() * DEFAULT_BPM / 60.0
        val events = mutableListOf<MidiEv>()

        for (n in notes) {
            val startTick = (n.startTimeSec * ticksPerSec).toInt()
            val endTick = (n.endTimeSec * ticksPerSec).toInt()
            val vel = (n.confidence * 100 + 27).toInt().coerceIn(1, 127)

            events.add(MidiEv.NoteOn(startTick, n.midiPitch, vel))
            events.add(MidiEv.NoteOff(endTick, n.midiPitch))

            if (Articulation.VIBRATO in n.articulations && n.vibratoRateHz > 0f) {
                addVibratoBend(
                    events,
                    startTick,
                    endTick,
                    n.vibratoRateHz,
                    n.vibratoDepthCents,
                    ticksPerSec,
                )
            } else if (Articulation.BEND_UP in n.articulations && n.bendSemitones > 0f) {
                addBendUp(events, startTick, endTick, n.bendSemitones)
            } else if (Articulation.BEND_RELEASE in n.articulations && n.bendSemitones > 0f) {
                addBendRelease(events, startTick, endTick, n.bendSemitones)
            }
        }

        events.sortWith(
            compareBy({ it.tick }, {
                when (it) {
                    is MidiEv.PitchBend -> 0
                    is MidiEv.NoteOff -> 1
                    is MidiEv.NoteOn -> 2
                }
            }),
        )

        val buf = ByteArrayOutputStream()

        buf.writeVarLen(0)
        buf.write(byteArrayOf(0xFF.toByte(), 0x51, 0x03))
        buf.write((MICROSEC_PER_BEAT shr 16) and 0xFF)
        buf.write((MICROSEC_PER_BEAT shr 8) and 0xFF)
        buf.write(MICROSEC_PER_BEAT and 0xFF)

        // Pitch bend range RPN (set to ±2 semitones)
        buf.writeVarLen(0)
        buf.write(0xB0)
        buf.write(0x65)
        buf.write(0x00) // RPN MSB = 0
        buf.writeVarLen(0)
        buf.write(0xB0)
        buf.write(0x64)
        buf.write(0x00) // RPN LSB = 0
        buf.writeVarLen(0)
        buf.write(0xB0)
        buf.write(0x06)
        buf.write(PITCH_BEND_RANGE_SEMI) // Data MSB

        var lastTick = 0
        for (ev in events) {
            val delta = (ev.tick - lastTick).coerceAtLeast(0)
            when (ev) {
                is MidiEv.NoteOn -> {
                    buf.writeVarLen(delta)
                    buf.write(0x90)
                    buf.write(ev.pitch and 0x7F)
                    buf.write(ev.vel and 0x7F)
                }
                is MidiEv.NoteOff -> {
                    buf.writeVarLen(delta)
                    buf.write(0x80)
                    buf.write(ev.pitch and 0x7F)
                    buf.write(0x00)
                }
                is MidiEv.PitchBend -> {
                    buf.writeVarLen(delta)
                    val clamped = ev.value.coerceIn(0, 16383)
                    buf.write(0xE0)
                    buf.write(clamped and 0x7F)
                    buf.write((clamped shr 7) and 0x7F)
                }
            }
            lastTick = ev.tick
        }

        // Reset pitch bend at end
        buf.writeVarLen(0)
        buf.write(0xE0)
        buf.write(PITCH_BEND_CENTER and 0x7F)
        buf.write((PITCH_BEND_CENTER shr 7) and 0x7F)

        buf.writeVarLen(0)
        buf.write(byteArrayOf(0xFF.toByte(), 0x2F, 0x00))

        return buf.toByteArray()
    }

    // ── Articulation pitch-bend generators ───────────────────────────

    private fun addVibratoBend(
        events: MutableList<MidiEv>,
        startTick: Int,
        endTick: Int,
        rateHz: Float,
        depthCents: Float,
        ticksPerSec: Double,
    ) {
        val periodTicks = (ticksPerSec / rateHz).toInt().coerceAtLeast(1)
        val depthSemitones = depthCents / 200f
        val bendRange = (depthSemitones / PITCH_BEND_RANGE_SEMI * PITCH_BEND_CENTER).toInt()
        var tick = startTick
        while (tick < endTick) {
            val phase = ((tick - startTick).toDouble() / periodTicks) * 2.0 * Math.PI
            val offset = (sin(phase) * bendRange).roundToInt()
            events.add(MidiEv.PitchBend(tick, PITCH_BEND_CENTER + offset))
            tick += VIBRATO_RESOLUTION_TICKS
        }
        events.add(MidiEv.PitchBend(endTick, PITCH_BEND_CENTER))
    }

    private fun addBendUp(
        events: MutableList<MidiEv>,
        startTick: Int,
        endTick: Int,
        semitones: Float,
    ) {
        val steps = 16
        val duration = endTick - startTick
        val maxBend =
            (semitones / PITCH_BEND_RANGE_SEMI * PITCH_BEND_CENTER).toInt()
        for (i in 0..steps) {
            val tick = startTick + duration * i / steps
            val bend = PITCH_BEND_CENTER + maxBend * i / steps
            events.add(MidiEv.PitchBend(tick, bend))
        }
    }

    private fun addBendRelease(
        events: MutableList<MidiEv>,
        startTick: Int,
        endTick: Int,
        semitones: Float,
    ) {
        val mid = (startTick + endTick) / 2
        addBendUp(events, startTick, mid, semitones)
        val steps = 16
        val duration = endTick - mid
        val maxBend =
            (semitones / PITCH_BEND_RANGE_SEMI * PITCH_BEND_CENTER).toInt()
        for (i in 0..steps) {
            val tick = mid + duration * i / steps
            val bend = PITCH_BEND_CENTER + maxBend - maxBend * i / steps
            events.add(MidiEv.PitchBend(tick, bend))
        }
    }

    // ── Binary helpers ──────────────────────────────────────────────

    private fun ByteArrayOutputStream.writeAscii(s: String) {
        write(s.toByteArray(Charsets.US_ASCII))
    }

    private fun ByteArrayOutputStream.writeInt32(v: Int) {
        write((v shr 24) and 0xFF)
        write((v shr 16) and 0xFF)
        write((v shr 8) and 0xFF)
        write(v and 0xFF)
    }

    private fun ByteArrayOutputStream.writeInt16(v: Int) {
        write((v shr 8) and 0xFF)
        write(v and 0xFF)
    }

    private fun ByteArrayOutputStream.writeVarLen(value: Int) {
        var v = value.coerceAtLeast(0)
        if (v < 0x80) {
            write(v)
            return
        }
        val bytes = mutableListOf<Int>()
        bytes.add(v and 0x7F)
        v = v shr 7
        while (v > 0) {
            bytes.add((v and 0x7F) or 0x80)
            v = v shr 7
        }
        for (b in bytes.reversed()) write(b)
    }
}
