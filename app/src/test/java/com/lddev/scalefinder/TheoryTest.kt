package com.lddev.scalefinder

import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.ChordQuality
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.ScaleType
import com.lddev.scalefinder.model.Theory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TheoryTest {

    // --- Note ---
    @Test
    fun note_fromSemitone_wrapsCorrectly() {
        assertEquals(Note.C, Note.fromSemitone(0))
        assertEquals(Note.C, Note.fromSemitone(12))
        assertEquals(Note.C, Note.fromSemitone(-12))
        assertEquals(Note.G, Note.fromSemitone(7))
        assertEquals(Note.G, Note.fromSemitone(19))
        assertEquals(Note.F_SHARP, Note.fromSemitone(6))
    }

    @Test
    fun note_positiveMod_handlesNegative() {
        assertEquals(1, Note.positiveMod(-11, 12))
        assertEquals(0, Note.positiveMod(12, 12))
        assertEquals(11, Note.positiveMod(-1, 12))
    }

    @Test
    fun note_toString_returnsLabel() {
        assertEquals("C", Note.C.toString())
        assertEquals("C#", Note.C_SHARP.toString())
        assertEquals("B", Note.B.toString())
    }

    // --- Chord ---
    @Test
    fun chord_cMajor_hasCorrectTones() {
        val chord = Chord(Note.C, ChordQuality.MAJOR)
        assertEquals(setOf(0, 4, 7), chord.tones)
    }

    @Test
    fun chord_aMinor_hasCorrectTones() {
        val chord = Chord(Note.A, ChordQuality.MINOR)
        assertEquals(setOf(9, 0, 4), chord.tones) // A=9, +0,3,7 -> 9,0,4
    }

    @Test
    fun chord_g7_hasFourTones() {
        val chord = Chord(Note.G, ChordQuality.DOMINANT7)
        assertEquals(4, chord.tones.size)
        assertTrue(chord.tones.contains(7))  // G
        assertTrue(chord.tones.contains(11)) // B
        assertTrue(chord.tones.contains(2))  // D
        assertTrue(chord.tones.contains(5))  // F
    }

    @Test
    fun chord_toString_format() {
        assertEquals("CMaj", Chord(Note.C, ChordQuality.MAJOR).toString())
        assertEquals("Am7", Chord(Note.A, ChordQuality.MINOR7).toString())
    }

    // --- Scale ---
    @Test
    fun scale_cMajor_hasCorrectTones() {
        val scale = Scale(Note.C, ScaleType.MAJOR)
        assertEquals(setOf(0, 2, 4, 5, 7, 9, 11), scale.tones)
    }

    @Test
    fun scale_aMinorPentatonic_hasFiveTones() {
        val scale = Scale(Note.A, ScaleType.MINOR_PENTATONIC)
        assertEquals(5, scale.tones.size)
        assertTrue(scale.tones.contains(9)) // A
        assertTrue(scale.tones.contains(0)) // C
    }

    @Test
    fun scale_toString_format() {
        assertTrue(Scale(Note.G, ScaleType.MIXOLYDIAN).toString().contains("Mixolydian"))
    }

    // --- Theory.allChords / allScales ---
    @Test
    fun theory_allChords_count() {
        val chords = Theory.allChords()
        val qualities = 7
        val roots = 12
        assertEquals(roots * qualities, chords.size)
    }

    @Test
    fun theory_allScales_count() {
        val scales = Theory.allScales()
        val scaleTypes = ScaleType.entries.size
        val roots = Note.entries.size
        assertEquals(roots * scaleTypes, scales.size)
    }

    // --- suggestScalesForChord ---
    @Test
    fun suggestScalesForChord_cMajor_includesCMajorScale() {
        val chord = Chord(Note.C, ChordQuality.MAJOR)
        val suggestions = Theory.suggestScalesForChord(chord)
        val cMajor = suggestions.find { it.scale.root == Note.C && it.scale.type == ScaleType.MAJOR }
        assertNotNull(cMajor)
        assertTrue(cMajor!!.rationale.isNotEmpty())
    }

    @Test
    fun suggestScalesForChord_g7_includesMixolydian() {
        val chord = Chord(Note.G, ChordQuality.DOMINANT7)
        val suggestions = Theory.suggestScalesForChord(chord)
        val mixo = suggestions.find { it.scale.type == ScaleType.MIXOLYDIAN && it.scale.root == Note.G }
        assertNotNull(mixo)
    }

    @Test
    fun suggestScalesForChord_onlyReturnsScalesContainingAllChordTones() {
        val chord = Chord(Note.D, ChordQuality.MINOR7)
        val suggestions = Theory.suggestScalesForChord(chord)
        suggestions.forEach { suggestion ->
            chord.tones.forEach { tone ->
                assertTrue("Scale ${suggestion.scale} should contain chord tone $tone", tone in suggestion.scale.tones)
            }
        }
    }

    // --- suggestScalesForProgression ---
    @Test
    fun suggestScalesForProgression_empty_returnsEmpty() {
        val result = Theory.suggestScalesForProgression(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun c_major_prefers_c_major_scale() {
        val chord = Chord(Note.C, ChordQuality.MAJOR)
        val suggestions = Theory.suggestScalesForProgression(listOf(chord))
        val top = suggestions.firstOrNull()?.scale
        assertTrue(top != null && top.root == Note.C)
    }

    @Test
    fun g7_suggests_mixolydian() {
        val chord = Chord(Note.G, ChordQuality.DOMINANT7)
        val suggestions = Theory.suggestScalesForProgression(listOf(chord))
        val names = suggestions.map { it.scale.type.display }
        assertTrue(names.any { it.contains("Mixolydian", ignoreCase = true) })
    }

    @Test
    fun suggestScalesForProgression_returnsAtMost12() {
        val prog = listOf(
            Chord(Note.C, ChordQuality.MAJOR),
            Chord(Note.G, ChordQuality.DOMINANT7),
            Chord(Note.A, ChordQuality.MINOR),
            Chord(Note.F, ChordQuality.MAJOR)
        )
        val suggestions = Theory.suggestScalesForProgression(prog)
        assertTrue(suggestions.size <= 12)
    }

    @Test
    fun suggestScalesForProgression_sortedByScoreDescending() {
        val chord = Chord(Note.C, ChordQuality.MAJOR)
        val suggestions = Theory.suggestScalesForProgression(listOf(chord))
        for (i in 0 until suggestions.size - 1) {
            assertTrue(suggestions[i].score >= suggestions[i + 1].score)
        }
    }
}



