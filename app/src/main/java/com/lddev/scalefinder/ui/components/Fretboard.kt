package com.lddev.scalefinder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.Tuning
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

data class FretHighlight(val stringIndex: Int, val fret: Int, val color: Color)

@Composable
fun GuitarFretboard(
    modifier: Modifier = Modifier,
    tuning: Tuning,
    scale: Scale?,
    fretStart: Int = 0,
    fretCount: Int = 12,
    showRoot: Boolean = true,
    chordTones: Set<Int> = emptySet(),
    stringHeight: Dp = 36.dp,
    highlights: List<FretHighlight> = emptyList(),
    highContrast: Boolean = false,
    invertStrings: Boolean = false,
    onNoteTapped: (stringIndex: Int, fret: Int, note: Note) -> Unit = { _, _, _ -> }
) {
    val strings = tuning.openNotes
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    val bgBrush = Brush.verticalGradient(colors = listOf(surfaceVariant, surface))
    val line = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val inlay = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val rootColor = MaterialTheme.colorScheme.primary
    val scaleColor = MaterialTheme.colorScheme.secondary
    val chordColor = MaterialTheme.colorScheme.tertiary

    var boxSize = remember { IntSize.Zero }
    Box(
        modifier
            .fillMaxWidth()
            .height(stringHeight * strings.size + 32.dp)
            .onSizeChanged { boxSize = it }
            .semantics { contentDescription = "Guitar fretboard" }
            .pointerInput(tuning, scale, fretStart, fretCount, invertStrings) {
                detectTapGestures { pos ->
                    if (boxSize.width == 0 || boxSize.height == 0) return@detectTapGestures
                    val stringSpacing = boxSize.height / (strings.size + 1f)
                    val fretSpacing = boxSize.width / (fretCount + 1f)
                    val visualIndex = ((pos.y / stringSpacing) - 1f).roundToInt().coerceIn(0, strings.lastIndex)
                    val dataIndex = if (invertStrings) (strings.lastIndex - visualIndex) else visualIndex
                    val f = ((pos.x / fretSpacing) - 0.5f).roundToInt() + fretStart
                    val fret = f.coerceIn(fretStart, fretStart + fretCount)
                    val note = Note.fromSemitone(strings[dataIndex].semitone + fret)
                    onNoteTapped(dataIndex, fret, note)
                }
            }
    ) {
        val anim = remember { Animatable(0f) }
        LaunchedEffect(scale, chordTones, highlights) {
            anim.snapTo(0f)
            anim.animateTo(1f, animationSpec = tween(300))
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            // Wood background
            drawRect(brush = bgBrush)
            val width = size.width
            val height = size.height
            val stringSpacing = height / (strings.size + 1)
            val fretSpacing = width / (fretCount + 1)

            // Draw strings
            for (s in strings.indices) {
                val visualIndex = if (invertStrings) (strings.lastIndex - s) else s
                val y = stringSpacing * (visualIndex + 1)
                drawLine(
                    color = line,
                    start = Offset(fretSpacing, y),
                    end = Offset(width - fretSpacing * 0.25f, y),
                    strokeWidth = if (highContrast) 4.5f else 3.5f,
                    cap = StrokeCap.Round
                )
            }

            // Draw frets
            for (f in 0..fretCount) {
                val x = fretSpacing * (f + 1)
                drawLine(
                    color = line,
                    start = Offset(x, stringSpacing * 0.6f),
                    end = Offset(x, height - stringSpacing * 0.6f),
                    strokeWidth = if (f == 0) 8f else if (highContrast) 4f else 3f
                )
            }

            // Draw markers (3,5,7,9,12)
            val markerFrets = setOf(3, 5, 7, 9, 12)
            markerFrets.filter { it in fretStart..(fretStart + fretCount) }.forEach { f ->
                val x = fretSpacing * (f - fretStart + 0.5f)
                if (f == 12) {
                    drawCircle(
                        color = inlay,
                        radius = 8f,
                        center = Offset(x, height * 0.35f)
                    )
                    drawCircle(
                        color = inlay,
                        radius = 8f,
                        center = Offset(x, height * 0.65f)
                    )
                } else {
                    drawCircle(
                        color = inlay,
                        radius = 8f,
                        center = Offset(x, height * 0.5f)
                    )
                }
            }

            // Draw scale notes
            if (scale != null) {
                for (s in strings.indices) {
                    val open = strings[s]
                    for (f in fretStart..(fretStart + fretCount)) {
                        val note = Note.fromSemitone(open.semitone + f)
                        val isInScale = note.semitone in scale.tones
                        if (isInScale) {
                            val isRoot = note.semitone == scale.root.semitone
                            val isChordTone = note.semitone in chordTones
                            val visualIndex = if (invertStrings) (strings.lastIndex - s) else s
                            val y = stringSpacing * (visualIndex + 1)
                            val x = fretSpacing * (f - fretStart + 0.5f)
                            val color = when {
                                isRoot && showRoot -> rootColor
                                isChordTone -> chordColor
                                else -> scaleColor
                            }
                            if (isRoot && showRoot) {
                                // Filled root note
                                drawCircle(
                                    color = color.copy(alpha = 0.95f),
                                    radius = 16f,
                                    center = Offset(x, y)
                                )
                            } else {
                                // outer ring for note marker
                                drawCircle(
                                    color = color.copy(alpha = 0.6f + 0.35f * anim.value),
                                    radius = 16f,
                                    center = Offset(x, y),
                                    style = Stroke(width = if (highContrast) 8f else 6f)
                                )
                                // subtle fill
                                drawCircle(
                                    color = color.copy(alpha = if (highContrast) 0.25f else 0.12f),
                                    radius = 16f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }
            }

            // Draw playback highlights (filled circles)
            if (highlights.isNotEmpty()) {
                highlights.forEach { h ->
                    if (h.stringIndex in strings.indices) {
                        val visualIndex = if (invertStrings) (strings.lastIndex - h.stringIndex) else h.stringIndex
                        val y = stringSpacing * (visualIndex + 1)
                        val x = fretSpacing * (h.fret - fretStart + 0.5f)
                        drawCircle(
                            color = h.color.copy(alpha = 0.95f),
                            radius = if (highContrast) 16f else 14f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}


