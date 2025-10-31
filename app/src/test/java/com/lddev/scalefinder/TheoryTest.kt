package com.lddev.scalefinder

import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.ChordQuality
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.Theory
import org.junit.Assert.assertTrue
import org.junit.Test

class TheoryTest {
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
}



