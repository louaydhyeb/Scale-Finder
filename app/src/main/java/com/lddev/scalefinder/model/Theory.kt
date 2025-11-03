package com.lddev.scalefinder.model

import kotlin.math.floor

enum class Note(val semitone: Int, val label: String) {
    C(0, "C"),
    C_SHARP(1, "C#"),
    D(2, "D"),
    D_SHARP(3, "D#"),
    E(4, "E"),
    F(5, "F"),
    F_SHARP(6, "F#"),
    G(7, "G"),
    G_SHARP(8, "G#"),
    A(9, "A"),
    A_SHARP(10, "A#"),
    B(11, "B");

    override fun toString(): String = label

    companion object {
        private val bySemitone = entries.associateBy { it.semitone }
        fun fromSemitone(semitone: Int): Note = bySemitone[positiveMod(semitone, 12)]!!
        fun positiveMod(a: Int, b: Int): Int = ((a % b) + b) % b
    }
}

enum class ChordQuality(val display: String, val intervals: List<Int>) {
    MAJOR("Maj", listOf(0, 4, 7)),
    MINOR("min", listOf(0, 3, 7)),
    DOMINANT7("7", listOf(0, 4, 7, 10)),
    MAJOR7("Maj7", listOf(0, 4, 7, 11)),
    MINOR7("m7", listOf(0, 3, 7, 10)),
    DIMINISHED("dim", listOf(0, 3, 6)),
    HALF_DIMINISHED("m7b5", listOf(0, 3, 6, 10));
}

data class Chord(val root: Note, val quality: ChordQuality) {
    val tones: Set<Int> = quality.intervals.map { (root.semitone + it) % 12 }.toSet()
    override fun toString(): String = "${root}${quality.display}"
}

enum class ScaleType(val display: String, val intervals: List<Int>) {
    MAJOR("Major (Ionian)", listOf(0, 2, 4, 5, 7, 9, 11)),
    DORIAN("Dorian", listOf(0, 2, 3, 5, 7, 9, 10)),
    PHRYGIAN("Phrygian", listOf(0, 1, 3, 5, 7, 8, 10)),
    LYDIAN("Lydian", listOf(0, 2, 4, 6, 7, 9, 11)),
    MIXOLYDIAN("Mixolydian", listOf(0, 2, 4, 5, 7, 9, 10)),
    AEOLIAN("Natural Minor (Aeolian)", listOf(0, 2, 3, 5, 7, 8, 10)),
    LOCRIAN("Locrian", listOf(0, 1, 3, 5, 6, 8, 10)),
    MAJOR_PENTATONIC("Major Pentatonic", listOf(0, 2, 4, 7, 9)),
    MINOR_PENTATONIC("Minor Pentatonic", listOf(0, 3, 5, 7, 10)),
    BLUES("Blues", listOf(0, 3, 5, 6, 7, 10));
}

data class Scale(val root: Note, val type: ScaleType) {
    val tones: Set<Int> = type.intervals.map { (root.semitone + it) % 12 }.toSet()
    override fun toString(): String = "${root} ${type.display}"
}

object Theory {
    fun allChords(): List<Chord> {
        val roots = Note.entries
        val qualities = listOf(
            ChordQuality.MAJOR,
            ChordQuality.MINOR,
            ChordQuality.DOMINANT7,
            ChordQuality.MAJOR7,
            ChordQuality.MINOR7,
            ChordQuality.DIMINISHED,
            ChordQuality.HALF_DIMINISHED
        )
        return roots.flatMap { r -> qualities.map { q -> Chord(r, q) } }
    }

    fun allScales(): List<Scale> = Note.entries.flatMap { r -> ScaleType.entries.map { t -> Scale(r, t) } }

    fun suggestScalesForChord(chord: Chord): List<ScaleSuggestion> {
        val candidates = allScales().filter { scale -> chord.tones.all { it in scale.tones } }
        return candidates.map { scale ->
            val rationale = when (scale.type) {
                ScaleType.MAJOR -> if (chord.quality in listOf(ChordQuality.MAJOR, ChordQuality.MAJOR7)) "Shares chord tones of major chord." else "Modal interchange may work contextually."
                ScaleType.MIXOLYDIAN -> if (chord.quality == ChordQuality.DOMINANT7) "Dominant chord natural fit (b7 present)." else "Works over dominant functions."
                ScaleType.AEOLIAN -> if (chord.quality in listOf(ChordQuality.MINOR, ChordQuality.MINOR7)) "Minor chord natural fit (b3 present)." else "Minor color over non-functional harmony."
                ScaleType.DORIAN -> if (chord.quality in listOf(ChordQuality.MINOR, ChordQuality.MINOR7)) "Great minor mode (natural 6)." else "Dorian color option."
                ScaleType.PHRYGIAN -> if (chord.quality in listOf(ChordQuality.MINOR, ChordQuality.MINOR7)) "Minor with b2 flavor." else "Exotic color."
                ScaleType.LYDIAN -> if (chord.quality in listOf(ChordQuality.MAJOR, ChordQuality.MAJOR7)) "Major with #4 (avoid on chord tones)." else "Bright color over major contexts."
                ScaleType.LOCRIAN -> if (chord.quality in listOf(ChordQuality.DIMINISHED, ChordQuality.HALF_DIMINISHED)) "Fits diminished/half-diminished." else "Rarely used broadly."
                ScaleType.MAJOR_PENTATONIC -> if (chord.quality in listOf(ChordQuality.MAJOR, ChordQuality.MAJOR7, ChordQuality.DOMINANT7)) "Safe, consonant choice." else "Pentatonic color option."
                ScaleType.MINOR_PENTATONIC -> if (chord.quality in listOf(ChordQuality.MINOR, ChordQuality.MINOR7)) "Safe, blues/rock minor choice." else "Bluesy color option."
                ScaleType.BLUES -> "Adds blue note tension; stylistic choice."
            }
            ScaleSuggestion(scale, rationale)
        }
    }

    fun suggestScalesForProgression(prog: List<Chord>): List<ScaleSuggestionRanked> {
        if (prog.isEmpty()) return emptyList()
        val suggestions = allScales().map { scale ->
            val fitScores = prog.map { chord ->
                val chordToneCount = chord.tones.count { it in scale.tones }
                chordToneCount.toDouble() / chord.tones.size
            }
            val avgFit = if (fitScores.isEmpty()) 0.0 else fitScores.sum() / fitScores.size
            val penalty = voiceLeadingPenalty(scale, prog)
            val score = avgFit - penalty
            val why = buildString {
                append("Fits ")
                append(floor(avgFit * 100).toInt())
                append("% of chord tones on average.")
            }
            ScaleSuggestionRanked(scale, score, why)
        }
        return suggestions
            .sortedByDescending { it.score }
            .take(12)
    }

    private fun voiceLeadingPenalty(scale: Scale, prog: List<Chord>): Double {
        // Simple heuristic: prefer scales whose root matches common chord roots or first chord root
        val firstRootMatch = if (scale.root == prog.first().root) 0.0 else 0.05
        return firstRootMatch
    }
}

data class ScaleSuggestion(val scale: Scale, val rationale: String)

data class ScaleSuggestionRanked(val scale: Scale, val score: Double, val rationale: String)

data class Tuning(val name: String, val openNotes: List<Note>) {
    /**
     * Returns the MIDI note number for an open string at the given index.
     * Standard guitar tuning: E2=40, A2=45, D3=50, G3=55, B3=59, E4=64
     * For other tunings, calculates relative to standard tuning.
     */
    fun getOpenStringMidi(stringIndex: Int): Int {
        require(stringIndex in openNotes.indices) { "String index out of range" }
        
        // Standard tuning MIDI notes (from low E to high E)
        val standardMidi = listOf(40, 45, 50, 55, 59, 64)
        
        // Get the standard note for this string position
        val standardTuning = listOf(Note.E, Note.A, Note.D, Note.G, Note.B, Note.E)
        val standardNote = standardTuning[stringIndex]
        val standardMidiNote = standardMidi[stringIndex]
        
        // Calculate the semitone difference from standard tuning
        val currentNote = openNotes[stringIndex]
        val semitoneDiff = currentNote.semitone - standardNote.semitone
        
        // Return MIDI note adjusted for tuning difference
        return standardMidiNote + semitoneDiff
    }
    
    /**
     * Calculates the frequency in Hz for a note at the given string and fret.
     * Each fret adds one semitone (multiplies frequency by 2^(1/12)).
     */
    fun getFrequency(stringIndex: Int, fret: Int): Double {
        val midiNote = getOpenStringMidi(stringIndex) + fret
        // MIDI note 69 is A4 = 440 Hz
        return 440.0 * Math.pow(2.0, (midiNote - 69) / 12.0)
    }
    
    companion object {
        val STANDARD = Tuning("E Standard", listOf(Note.E, Note.A, Note.D, Note.G, Note.B, Note.E))
        val DROP_D = Tuning("Drop D", listOf(Note.D, Note.A, Note.D, Note.G, Note.B, Note.E))
        val DADGAD = Tuning("DADGAD", listOf(Note.D, Note.A, Note.D, Note.G, Note.A, Note.D))
        fun all(): List<Tuning> = listOf(STANDARD, DROP_D, DADGAD)
    }
}