package com.lddev.scalefinder.ui.components.quiz_components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.Tuning
import com.lddev.scalefinder.ui.components.home_components.FretHighlight
import com.lddev.scalefinder.ui.components.home_components.GuitarFretboard

@Composable
fun FretboardCard(
    scale: Scale? = null,
    highlights: List<FretHighlight> = emptyList(),
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        GuitarFretboard(
            modifier = Modifier.padding(8.dp),
            tuning = Tuning.STANDARD,
            scale = scale,
            fretStart = 0,
            fretCount = 12,
            highlights = highlights,
            invertStrings = true,
            showNoteNames = false,
        )
    }
}
