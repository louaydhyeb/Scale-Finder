package com.lddev.scalefinder

import com.lddev.scalefinder.model.Tuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningTest {
    @Test
    fun standard_getOpenStringMidi_knownValues() {
        // E2=40, A2=45, D3=50, G3=55, B3=59, E4=64
        assertEquals(40, Tuning.STANDARD.getOpenStringMidi(0))
        assertEquals(45, Tuning.STANDARD.getOpenStringMidi(1))
        assertEquals(50, Tuning.STANDARD.getOpenStringMidi(2))
        assertEquals(55, Tuning.STANDARD.getOpenStringMidi(3))
        assertEquals(59, Tuning.STANDARD.getOpenStringMidi(4))
        assertEquals(64, Tuning.STANDARD.getOpenStringMidi(5))
    }

    @Test
    fun dropD_lowString_oneSemitoneLower() {
        // Drop D: low E down to D -> 40 - 2 = 38 (D2)
        assertEquals(38, Tuning.DROP_D.getOpenStringMidi(0))
        assertEquals(45, Tuning.DROP_D.getOpenStringMidi(1))
    }

    @Test
    fun standard_getFrequency_a4_440Hz() {
        // String 5 (high E), fret 5 is A4 = 440 Hz (MIDI 69)
        // Open string 5 = E4 = 64, +5 frets = 69
        val freq = Tuning.STANDARD.getFrequency(5, 5)
        assertEquals(440.0, freq, 0.5)
    }

    @Test
    fun standard_getFrequency_e2_openLowE() {
        // Open low E = 40 -> 82.4 Hz (E2)
        val freq = Tuning.STANDARD.getFrequency(0, 0)
        assertEquals(82.41, freq, 0.1)
    }

    @Test
    fun getFrequency_eachFret_raisesBySemitone() {
        val f0 = Tuning.STANDARD.getFrequency(0, 0)
        val f1 = Tuning.STANDARD.getFrequency(0, 1)
        val ratio = f1 / f0
        // 2^(1/12) ≈ 1.0595
        assertEquals(1.0595, ratio, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getOpenStringMidi_negativeIndex_throws() {
        Tuning.STANDARD.getOpenStringMidi(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getOpenStringMidi_indexOutOfRange_throws() {
        Tuning.STANDARD.getOpenStringMidi(6)
    }

    @Test
    fun tuning_all_containsStandardAndDropD() {
        val all = Tuning.all()
        assertEquals(3, all.size)
        assertTrue(all.any { it.name == "E Standard" })
        assertTrue(all.any { it.name == "Drop D" })
        assertTrue(all.any { it.name == "DADGAD" })
    }
}
