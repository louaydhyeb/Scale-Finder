package com.lddev.scalefinder.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.Tuning
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

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
    showNoteNames: Boolean = false,
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
    val noteColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    var boxSize = remember { IntSize.Zero }
    Box(
        modifier
            .fillMaxWidth()
            .height(stringHeight * strings.size + 32.dp)
            .onSizeChanged { boxSize = it }
            .semantics { contentDescription = "Guitar fretboard" }
            .pointerInput(tuning, scale, fretStart, fretCount, invertStrings) {
                awaitEachGesture {
                    val down = awaitFirstDown() // Trigger immediately when finger touches
                    if (boxSize.width == 0 || boxSize.height == 0) return@awaitEachGesture

                    val stringSpacing = boxSize.height / (strings.size + 1f)
                    val fretSpacing = boxSize.width / (fretCount + 1f)

                    val visualIndex = ((down.position.y / stringSpacing) - 1f)
                        .roundToInt()
                        .coerceIn(0, strings.lastIndex)

                    val dataIndex = if (invertStrings) (strings.lastIndex - visualIndex) else visualIndex
                    val f = ((down.position.x / fretSpacing) - 0.5f).roundToInt() + fretStart
                    val fret = f.coerceIn(fretStart, fretStart + fretCount)
                    val note = Note.fromSemitone(strings[dataIndex].semitone + fret)

                    onNoteTapped(dataIndex, fret, note)
                    // You can optionally wait for release:
                    waitForUpOrCancellation()
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

            // Draw note names on each fret (draw first, so scale markers appear on top)
            if (showNoteNames) {
                val noteColorArgb = AndroidColor.argb(
                    (noteColor.alpha * 255).toInt(),
                    (noteColor.red * 255).toInt(),
                    (noteColor.green * 255).toInt(),
                    (noteColor.blue * 255).toInt()
                )
                
                drawIntoCanvas { canvas ->
                    val textPaint = Paint().apply {
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        textSize = if (highContrast) 14f * density else 12f * density
                    }
                    
                    for (s in strings.indices) {
                        for (f in fretStart..(fretStart + fretCount)) {
                            val note = Note.fromSemitone(strings[s].semitone + f)
                            val visualIndex = if (invertStrings) (strings.lastIndex - s) else s
                            val y = stringSpacing * (visualIndex + 1)
                            val x = fretSpacing * (f - fretStart + 0.5f)
                            
                            // Only show note names on frets that don't have scale markers (to avoid clutter)
                            val hasScaleMarker = scale != null && note.semitone in scale.tones
                            val hasHighlight = highlights.any { it.stringIndex == s && it.fret == f }
                            
                            // Show note names on all frets, but with lower opacity if there's a marker
                            val opacity = if (hasScaleMarker || hasHighlight) 0.4f else 0.6f
                            val alpha = (opacity * 255).toInt()
                            
                            textPaint.color = AndroidColor.argb(
                                alpha,
                                AndroidColor.red(noteColorArgb),
                                AndroidColor.green(noteColorArgb),
                                AndroidColor.blue(noteColorArgb)
                            )
                            
                            // Format note name
                            val noteName = note.toString()
                            canvas.nativeCanvas.drawText(noteName, x, y + 4f * density, textPaint)
                        }
                    }
                }
            }

            // Draw scale notes (draw after note names so they appear on top)
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
                            
                            // Make scale markers more visible when note names are shown
                            val baseAlpha = if (showNoteNames) 0.9f else 0.95f
                            val strokeWidth = if (showNoteNames && highContrast) 10f else if (showNoteNames) 8f else if (highContrast) 8f else 6f
                            val fillAlpha = if (showNoteNames && highContrast) 0.4f else if (showNoteNames) 0.3f else if (highContrast) 0.25f else 0.12f
                            
                            if (isRoot && showRoot) {
                                // Filled root note
                                drawCircle(
                                    color = color.copy(alpha = baseAlpha),
                                    radius = if (showNoteNames) 18f else 16f,
                                    center = Offset(x, y)
                                )
                            } else {
                                // outer ring for note marker
                                drawCircle(
                                    color = color.copy(alpha = if (showNoteNames) baseAlpha else (0.6f + 0.35f * anim.value)),
                                    radius = if (showNoteNames) 18f else 16f,
                                    center = Offset(x, y),
                                    style = Stroke(width = strokeWidth)
                                )
                                // subtle fill
                                drawCircle(
                                    color = color.copy(alpha = fillAlpha),
                                    radius = if (showNoteNames) 18f else 16f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }
            }

            // Draw playback highlights (filled circles) - draw last so they appear on top
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
