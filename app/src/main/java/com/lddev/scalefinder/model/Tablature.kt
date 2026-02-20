package com.lddev.scalefinder.model

enum class Articulation(val symbol: String) {
    VIBRATO("~"),
    BEND_UP("b"),
    BEND_RELEASE("br"),
    SLIDE_UP("/"),
    SLIDE_DOWN("\\"),
    HAMMER_ON("h"),
    PULL_OFF("p");
}

data class TabNote(
    val string: Int,
    val fret: Int,
    val articulations: Set<Articulation> = emptySet(),
    val bendSemitones: Float = 0f
)

data class TabEvent(
    val timeMs: Long,
    val durationMs: Long,
    val notes: List<TabNote>
)

data class Tablature(
    val tuning: Tuning,
    val events: List<TabEvent>,
    val durationMs: Long
) {
    fun toAscii(): String {
        val stringLabels = listOf("e", "B", "G", "D", "A", "E")
        val lines = Array(6) { StringBuilder(stringLabels[it] + "|") }

        for (event in events) {
            val fretsByString = IntArray(6) { -1 }
            val artByString = Array<Set<Articulation>>(6) { emptySet() }
            val bendByString = FloatArray(6)
            for (note in event.notes) {
                val displayString = 5 - note.string
                if (displayString in 0..5) {
                    fretsByString[displayString] = note.fret
                    artByString[displayString] = note.articulations
                    bendByString[displayString] = note.bendSemitones
                }
            }

            val maxWidth = fretsByString.maxOf { fret ->
                if (fret >= 0) fret.toString().length else 1
            }

            for (s in 0 until 6) {
                val text = if (fretsByString[s] >= 0) {
                    val fretStr = fretsByString[s].toString()
                    val suffix = buildString {
                        val arts = artByString[s]
                        if (Articulation.VIBRATO in arts) append("~")
                        if (Articulation.BEND_UP in arts) {
                            val semi = bendByString[s]
                            if (semi >= 1.5f) append("b^^")
                            else if (semi >= 0.75f) append("b^")
                            else append("b")
                        }
                        if (Articulation.BEND_RELEASE in arts) append("br")
                        if (Articulation.SLIDE_UP in arts) append("/")
                        if (Articulation.SLIDE_DOWN in arts) append("\\")
                        if (Articulation.HAMMER_ON in arts) append("h")
                        if (Articulation.PULL_OFF in arts) append("p")
                    }
                    fretStr + suffix
                } else {
                    "-"
                }
                lines[s].append(text.padStart(maxWidth, '-'))
                lines[s].append("-")
            }
        }
        for (line in lines) line.append("|")
        return lines.joinToString("\n") { it.toString() }
    }
}
