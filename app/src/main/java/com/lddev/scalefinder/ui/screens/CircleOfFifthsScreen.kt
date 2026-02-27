package com.lddev.scalefinder.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.ChordQuality
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.ScaleFormulas
import com.lddev.scalefinder.model.ScaleType
import kotlin.math.cos
import kotlin.math.sin

private data class CircleOfFifthsState(
    val major: String,
    val relativeMinor: String,
    val accidentals: String,
    val commonProgression: String,
)

@Composable
fun CircleOfFifthsScreen(modifier: Modifier = Modifier) {
    val outerNotes = listOf("C", "G", "D", "A", "E", "B", "F#/Gb", "Db", "Ab", "Eb", "Bb", "F")
    val innerNotes = listOf("Am", "Em", "Bm", "F#m", "C#m", "G#m", "Ebm", "Bbm", "Fm", "Cm", "Gm", "Dm")

    val keyDetails =
        listOf(
            CircleOfFifthsState("C", "Am", "0 # / 0 b", "I - V - vi - IV"),
            CircleOfFifthsState("G", "Em", "1 #", "I - V - vi - IV"),
            CircleOfFifthsState("D", "Bm", "2 #", "I - V - vi - IV"),
            CircleOfFifthsState("A", "F#m", "3 #", "I - V - vi - IV"),
            CircleOfFifthsState("E", "C#m", "4 #", "I - V - vi - IV"),
            CircleOfFifthsState("B", "G#m", "5 #", "I - V - vi - IV"),
            CircleOfFifthsState("F#/Gb", "Ebm", "6 # / 6 b", "i - VI - III - VII"),
            CircleOfFifthsState("Db", "Bbm", "5 b", "ii - V - I"),
            CircleOfFifthsState("Ab", "Fm", "4 b", "ii - V - I"),
            CircleOfFifthsState("Eb", "Cm", "3 b", "ii - V - I"),
            CircleOfFifthsState("Bb", "Gm", "2 b", "I - V - vi - IV"),
            CircleOfFifthsState("F", "Dm", "1 b", "I - V - vi - IV"),
        )

    var selectedIndex by remember { mutableIntStateOf(0) }
    var selectedInner by remember { mutableStateOf(false) }

    val selected = keyDetails[selectedIndex]
    val majorRoots =
        listOf(
            Note.C, Note.G, Note.D, Note.A, Note.E, Note.B,
            Note.F_SHARP, Note.C_SHARP, Note.G_SHARP, Note.D_SHARP, Note.A_SHARP, Note.F,
        )
    val selectedScale =
        if (selectedInner) {
            Scale(Note.fromSemitone(majorRoots[selectedIndex].semitone + 9), ScaleType.AEOLIAN)
        } else {
            Scale(majorRoots[selectedIndex], ScaleType.MAJOR)
        }
    val degreesText =
        ScaleFormulas.diatonicChords(selectedScale)
            .joinToString("   ") { "${it.degree}:${formatChordShort(it.chord)}" }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Text(
            text =
                buildAnnotatedString {
                    withStyle(
                        style =
                            SpanStyle(
                                brush =
                                    Brush.horizontalGradient(
                                        colors =
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary,
                                            ),
                                    ),
                                fontWeight = FontWeight.Bold,
                            ),
                    ) { append(stringResource(R.string.circle_of_fifths_title)) }
                },
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.circle_of_fifths_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        CircleOfFifthsChart(
            outerNotes = outerNotes,
            innerNotes = innerNotes,
            selectedIndex = selectedIndex,
            selectedInner = selectedInner,
            onSelectOuter = { index ->
                selectedIndex = index
                selectedInner = false
            },
            onSelectInner = { index ->
                selectedIndex = index
                selectedInner = true
            },
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text =
                        if (selectedInner) {
                            stringResource(R.string.circle_selected_minor, selected.relativeMinor, selected.major)
                        } else {
                            stringResource(R.string.circle_selected_major, selected.major, selected.relativeMinor)
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.circle_accidentals, selected.accidentals),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.circle_progression, selected.commonProgression),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Degrees: $degreesText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatChordShort(chord: Chord): String {
    val suffix =
        when (chord.quality) {
            ChordQuality.MAJOR -> ""
            ChordQuality.MINOR -> "m"
            ChordQuality.DIMINISHED -> "dim"
            ChordQuality.AUGMENTED -> "+"
            ChordQuality.DOMINANT7 -> "7"
            ChordQuality.MAJOR7 -> "Maj7"
            ChordQuality.MINOR7 -> "m7"
            ChordQuality.HALF_DIMINISHED -> "m7b5"
        }
    return "${chord.root}$suffix"
}

@Composable
private fun CircleOfFifthsChart(
    outerNotes: List<String>,
    innerNotes: List<String>,
    selectedIndex: Int,
    selectedInner: Boolean,
    onSelectOuter: (Int) -> Unit,
    onSelectInner: (Int) -> Unit,
) {
    val chartSize = 420.dp
    val outerRadius = 165.dp
    val innerRadius = 108.dp
    val outerRingColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val innerRingColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(chartSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.size(chartSize),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadiusPx = size.minDimension * 0.4f
            val innerRadiusPx = size.minDimension * 0.26f

            drawCircle(
                color = outerRingColor,
                radius = outerRadiusPx,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = StrokeCap.Round),
            )
            drawCircle(
                color = innerRingColor,
                radius = innerRadiusPx,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f, cap = StrokeCap.Round),
            )

            repeat(12) { index ->
                val radians = Math.toRadians((index * 30.0) - 90.0)
                val start =
                    Offset(
                        x = center.x + cos(radians).toFloat() * (innerRadiusPx + 16f),
                        y = center.y + sin(radians).toFloat() * (innerRadiusPx + 16f),
                    )
                val end =
                    Offset(
                        x = center.x + cos(radians).toFloat() * (outerRadiusPx - 16f),
                        y = center.y + sin(radians).toFloat() * (outerRadiusPx - 16f),
                    )
                drawLine(
                    color = dividerColor,
                    start = start,
                    end = end,
                    strokeWidth = 2f,
                )
            }
        }

        repeat(12) { index ->
            CircularKeyChip(
                label = outerNotes[index],
                radius = outerRadius,
                index = index,
                selected = index == selectedIndex && !selectedInner,
                onClick = { onSelectOuter(index) },
            )
        }

        repeat(12) { index ->
            CircularKeyChip(
                label = innerNotes[index],
                radius = innerRadius,
                index = index,
                selected = index == selectedIndex && selectedInner,
                onClick = { onSelectInner(index) },
            )
        }
    }
}

@Composable
private fun CircularKeyChip(
    label: String,
    radius: Dp,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val angle = Math.toRadians((index * 30.0) - 90.0)
    val offsetX = (radius.value * cos(angle).toFloat()).dp
    val offsetY = (radius.value * sin(angle).toFloat()).dp

    Box(
        modifier =
            Modifier
                .offset(x = offsetX, y = offsetY)
                .size(52.dp)
                .padding(2.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                )
                .clip(CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}
