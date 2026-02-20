package com.lddev.scalefinder.model

/**
 * Represents a specific chord voicing on a guitar fretboard.
 *
 * @param name Display name for this voicing (e.g., "Open", "E-shape", "A-shape")
 * @param frets List of 6 fret values from low E (string 6) to high E (string 1):
 *              -1 = muted / not played
 *               0 = open string
 *               1+ = fret number
 * @param barreAtFret Optional barre position (absolute fret number)
 */
data class ChordVoicing(
    val name: String,
    val frets: List<Int>,
    val barreAtFret: Int? = null
) {
    /** The lowest fret being fingered (excluding open and muted strings) */
    val minFret: Int get() = frets.filter { it > 0 }.minOrNull() ?: 0

    /** The highest fret being fingered */
    val maxFret: Int get() = frets.filter { it > 0 }.maxOrNull() ?: 0

    /** Whether any string is played open */
    val hasOpenStrings: Boolean get() = frets.any { it == 0 }

    /**
     * The starting fret for diagram display.
     * Shows from fret 1 (nut visible) when the voicing is near the headstock,
     * otherwise starts from the lowest fret.
     */
    val displayBaseFret: Int
        get() = when {
            hasOpenStrings -> 1
            minFret <= 2 -> 1
            else -> minFret
        }

    /** Whether to show the nut (thick top line) in the diagram */
    val showNut: Boolean get() = displayBaseFret == 1
}

/**
 * Provides chord voicings for any chord using moveable barre shapes
 * and well-known open voicings. All voicings assume standard tuning.
 */
object ChordVoicings {

    // ── E-form moveable shapes ──────────────────────────────────────────
    // Root on string 6 (low E). Values are offsets from the barre fret.
    // -1 = muted
    private val E_SHAPES: Map<ChordQuality, List<Int>> = mapOf(
        ChordQuality.MAJOR          to listOf(0, 2, 2, 1, 0, 0),
        ChordQuality.MINOR          to listOf(0, 2, 2, 0, 0, 0),
        ChordQuality.AUGMENTED      to listOf(0, 2, 2, 1, 0, -1),
        ChordQuality.DOMINANT7      to listOf(0, 2, 0, 1, 0, 0),
        ChordQuality.MAJOR7         to listOf(0, 2, 1, 1, 0, 0),
        ChordQuality.MINOR7         to listOf(0, 2, 0, 0, 0, 0),
        ChordQuality.DIMINISHED     to listOf(-1, -1, 1, 2, 1, -1),
        ChordQuality.HALF_DIMINISHED to listOf(0, 1, 0, 0, -1, -1)
    )

    // ── A-form moveable shapes ──────────────────────────────────────────
    // Root on string 5 (A). Values are offsets from the barre fret.
    private val A_SHAPES: Map<ChordQuality, List<Int>> = mapOf(
        ChordQuality.MAJOR          to listOf(-1, 0, 2, 2, 2, 0),
        ChordQuality.MINOR          to listOf(-1, 0, 2, 2, 1, 0),
        ChordQuality.AUGMENTED      to listOf(-1, 0, 2, 1, 1, 0),
        ChordQuality.DOMINANT7      to listOf(-1, 0, 2, 0, 2, 0),
        ChordQuality.MAJOR7         to listOf(-1, 0, 2, 1, 2, 0),
        ChordQuality.MINOR7         to listOf(-1, 0, 2, 0, 1, 0),
        ChordQuality.DIMINISHED     to listOf(-1, 0, 1, 2, 1, -1),
        ChordQuality.HALF_DIMINISHED to listOf(-1, 0, 1, 0, 1, -1)
    )

    // ── Special open voicings ───────────────────────────────────────────
    // Unique fingerings that don't come from E/A moveable shapes.
    private val SPECIAL_OPEN: Map<Pair<Note, ChordQuality>, List<ChordVoicing>> = mapOf(
        // C chords
        Pair(Note.C, ChordQuality.MAJOR) to listOf(
            ChordVoicing("Open C", listOf(-1, 3, 2, 0, 1, 0))
        ),
        Pair(Note.C, ChordQuality.DOMINANT7) to listOf(
            ChordVoicing("Open C7", listOf(-1, 3, 2, 3, 1, 0))
        ),
        Pair(Note.C, ChordQuality.MAJOR7) to listOf(
            ChordVoicing("Open Cmaj7", listOf(-1, 3, 2, 0, 0, 0))
        ),

        // D chords
        Pair(Note.D, ChordQuality.MAJOR) to listOf(
            ChordVoicing("Open D", listOf(-1, -1, 0, 2, 3, 2))
        ),
        Pair(Note.D, ChordQuality.MINOR) to listOf(
            ChordVoicing("Open Dm", listOf(-1, -1, 0, 2, 3, 1))
        ),
        Pair(Note.D, ChordQuality.DOMINANT7) to listOf(
            ChordVoicing("Open D7", listOf(-1, -1, 0, 2, 1, 2))
        ),
        Pair(Note.D, ChordQuality.MAJOR7) to listOf(
            ChordVoicing("Open Dmaj7", listOf(-1, -1, 0, 2, 2, 2))
        ),
        Pair(Note.D, ChordQuality.MINOR7) to listOf(
            ChordVoicing("Open Dm7", listOf(-1, -1, 0, 2, 1, 1))
        ),
        Pair(Note.D, ChordQuality.DIMINISHED) to listOf(
            ChordVoicing("Open Ddim", listOf(-1, -1, 0, 1, 3, 1))
        ),
        Pair(Note.D, ChordQuality.HALF_DIMINISHED) to listOf(
            ChordVoicing("Open Dm7b5", listOf(-1, -1, 0, 1, 1, 1))
        ),

        // F chords (partial barre – easier than full E-form)
        Pair(Note.F, ChordQuality.MAJOR) to listOf(
            ChordVoicing("F (small)", listOf(-1, -1, 3, 2, 1, 1), barreAtFret = 1)
        ),

        // G chords
        Pair(Note.G, ChordQuality.MAJOR) to listOf(
            ChordVoicing("Open G", listOf(3, 2, 0, 0, 0, 3))
        ),
        Pair(Note.G, ChordQuality.DOMINANT7) to listOf(
            ChordVoicing("Open G7", listOf(3, 2, 0, 0, 0, 1))
        ),
        Pair(Note.G, ChordQuality.MAJOR7) to listOf(
            ChordVoicing("Open Gmaj7", listOf(3, 2, 0, 0, 0, 2))
        )
    )

    /**
     * Returns available voicings for the given chord.
     * Results are sorted by position (lower frets first) and deduplicated.
     */
    fun getVoicings(chord: Chord): List<ChordVoicing> {
        val voicings = mutableListOf<ChordVoicing>()

        // 1. Special open voicings first (best fingerings near the nut)
        SPECIAL_OPEN[Pair(chord.root, chord.quality)]?.let {
            voicings.addAll(it)
        }

        // 2. E-form moveable shape
        E_SHAPES[chord.quality]?.let { shape ->
            val baseFret = (chord.root.semitone - Note.E.semitone + 12) % 12
            val frets = shape.map { offset -> if (offset == -1) -1 else offset + baseFret }
            val name = if (baseFret == 0) "Open (E-shape)" else "E-shape"
            val barre = if (baseFret > 0) baseFret else null
            voicings.add(ChordVoicing(name, frets, barreAtFret = barre))
        }

        // 3. A-form moveable shape
        A_SHAPES[chord.quality]?.let { shape ->
            val baseFret = (chord.root.semitone - Note.A.semitone + 12) % 12
            val frets = shape.map { offset -> if (offset == -1) -1 else offset + baseFret }
            val name = if (baseFret == 0) "Open (A-shape)" else "A-shape"
            val barre = if (baseFret > 0) baseFret else null
            voicings.add(ChordVoicing(name, frets, barreAtFret = barre))
        }

        // Deduplicate by fret pattern and sort by lowest fret position
        return voicings
            .distinctBy { it.frets }
            .sortedBy { v -> v.frets.filter { it > 0 }.minOrNull() ?: 0 }
    }
}
