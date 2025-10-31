package com.lddev.scalefinder.model

data class FrettedNote(
    val timeMs: Long,
    val stringIndex: Int, // 0 = lowest string in tuning list
    val fret: Int,
    val durationMs: Long
)

data class SongSection(
    val name: String,
    val events: List<FrettedNote>
)

data class Song(
    val title: String,
    val artist: String,
    val tempoBpm: Int,
    val tuning: Tuning,
    val sections: List<SongSection>
) {
    val totalDurationMs: Long by lazy {
        val all = sections.flatMap { it.events }
        if (all.isEmpty()) 0 else all.maxOf { it.timeMs + it.durationMs }
    }
}



