package com.lddev.scalefinder.audio.transcription

import com.lddev.scalefinder.model.TabEvent
import com.lddev.scalefinder.model.TabNote
import com.lddev.scalefinder.model.Tablature
import com.lddev.scalefinder.model.Tuning
import kotlin.math.abs

/**
 * Maps detected [BasicPitchDetector.NoteEvent]s to guitar tablature positions,
 * minimising hand movement across consecutive events and carrying articulation
 * metadata through to [TabNote].
 */
object TabMapper {

    private const val MAX_FRET = 22
    private const val ONSET_TOLERANCE_SEC = 0.035f

    fun map(
        noteEvents: List<BasicPitchDetector.NoteEvent>,
        tuning: Tuning
    ): Tablature {
        if (noteEvents.isEmpty()) return Tablature(tuning, emptyList(), 0L)

        val groups = groupSimultaneous(noteEvents)
        val tabEvents = mutableListOf<TabEvent>()
        val lastFrets = IntArray(tuning.openNotes.size) { -1 }

        for (group in groups) {
            val tabNotes = mutableListOf<TabNote>()
            val usedStrings = mutableSetOf<Int>()

            val sorted = group.sortedBy { it.midiPitch }
            for (note in sorted) {
                val pos = bestPosition(note.midiPitch, tuning, lastFrets, usedStrings)
                    ?: continue
                tabNotes.add(
                    TabNote(
                        string = pos.first,
                        fret = pos.second,
                        articulations = note.articulations,
                        bendSemitones = note.bendSemitones
                    )
                )
                usedStrings.add(pos.first)
                lastFrets[pos.first] = pos.second
            }

            if (tabNotes.isNotEmpty()) {
                val startMs = (group.minOf { it.startTimeSec } * 1000).toLong()
                val endMs = (group.maxOf { it.endTimeSec } * 1000).toLong()
                tabEvents.add(TabEvent(startMs, (endMs - startMs).coerceAtLeast(1), tabNotes))
            }
        }

        val totalMs = if (noteEvents.isNotEmpty())
            (noteEvents.maxOf { it.endTimeSec } * 1000).toLong() else 0L

        return Tablature(tuning, tabEvents, totalMs)
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun bestPosition(
        midiPitch: Int,
        tuning: Tuning,
        lastFrets: IntArray,
        usedStrings: Set<Int>
    ): Pair<Int, Int>? {
        var bestString = -1
        var bestFret = -1
        var bestCost = Int.MAX_VALUE

        for (s in tuning.openNotes.indices) {
            if (s in usedStrings) continue
            val fret = midiPitch - tuning.getOpenStringMidi(s)
            if (fret !in 0..MAX_FRET) continue

            val moveCost = if (lastFrets[s] >= 0) abs(fret - lastFrets[s]) else 0
            val fretCost = fret / 4
            val cost = moveCost + fretCost
            if (cost < bestCost) {
                bestCost = cost
                bestString = s
                bestFret = fret
            }
        }
        return if (bestString >= 0) bestString to bestFret else null
    }

    private fun groupSimultaneous(
        events: List<BasicPitchDetector.NoteEvent>
    ): List<List<BasicPitchDetector.NoteEvent>> {
        val sorted = events.sortedBy { it.startTimeSec }
        val groups = mutableListOf<MutableList<BasicPitchDetector.NoteEvent>>()
        var current = mutableListOf(sorted.first())

        for (i in 1 until sorted.size) {
            if (sorted[i].startTimeSec - current.first().startTimeSec <= ONSET_TOLERANCE_SEC) {
                current.add(sorted[i])
            } else {
                groups.add(current)
                current = mutableListOf(sorted[i])
            }
        }
        groups.add(current)
        return groups
    }
}
