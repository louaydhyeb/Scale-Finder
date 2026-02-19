package com.lddev.scalefinder.ui.components.home_components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.model.ChordVoicing
import android.graphics.Color as AndroidColor

/**
 * Draws a classic guitar chord diagram for the given voicing.
 *
 * Layout (top to bottom):
 *  - X / O indicators for muted / open strings
 *  - Nut (thick line) or fret-number label
 *  - Fret grid with finger dots and optional barre
 */
@Composable
fun ChordDiagramView(
    voicing: ChordVoicing,
    modifier: Modifier = Modifier
) {
    // ── Resolve theme colours ────────────────────────────────────────
    val stringLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
    val fretLineColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
    val nutLineColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val dotFillColor    = MaterialTheme.colorScheme.primary
    val indicatorColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val labelColor      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)

    // ── Voicing metrics ──────────────────────────────────────────────
    val baseFret = voicing.displayBaseFret
    val showNut  = voicing.showNut
    val maxFret  = voicing.maxFret
    val numFrets = maxOf(4, maxFret - baseFret + 1)

    val diagDesc = "Chord diagram ${voicing.name}"

    Canvas(
        modifier = modifier
            .size(width = 120.dp, height = 150.dp)
            .semantics { contentDescription = diagDesc }
    ) {
        // ── Padding / layout ─────────────────────────────────────────
        val leftPad   = if (showNut) 12.dp.toPx() else 30.dp.toPx()
        val rightPad  = 12.dp.toPx()
        val topPad    = 24.dp.toPx()   // room for X / O symbols
        val bottomPad = 6.dp.toPx()

        val fretboardW = size.width  - leftPad - rightPad
        val fretboardH = size.height - topPad  - bottomPad

        val stringSpacing = fretboardW / 5f
        val fretSpacing   = fretboardH / numFrets

        val dotRadius = minOf(stringSpacing, fretSpacing) * 0.32f

        // Helper lambdas
        fun sx(idx: Int)   = leftPad + idx * stringSpacing            // string X
        fun fy(idx: Int)   = topPad + idx * fretSpacing               // fret-line Y
        fun dotY(absFret: Int) = topPad + (absFret - baseFret + 0.5f) * fretSpacing

        // ── 1. Fret lines ────────────────────────────────────────────
        for (i in 0..numFrets) {
            val y = fy(i)
            val isNut = i == 0 && showNut
            drawLine(
                color       = if (isNut) nutLineColor else fretLineColor,
                start       = Offset(sx(0), y),
                end         = Offset(sx(5), y),
                strokeWidth = if (isNut) 6f else 1.5f,
                cap         = StrokeCap.Round
            )
        }

        // ── 2. String lines ─────────────────────────────────────────
        for (i in 0..5) {
            drawLine(
                color       = stringLineColor,
                start       = Offset(sx(i), topPad),
                end         = Offset(sx(i), topPad + fretboardH),
                strokeWidth = 1.5f
            )
        }

        // ── 3. Muted (X) / Open (O) indicators ──────────────────────
        val indicatorY = topPad - 12.dp.toPx()
        val indicatorR = 4.5.dp.toPx()

        for (i in 0..5) {
            val x = sx(i)
            when (voicing.frets[i]) {
                -1 -> {
                    // X  –  two crossing lines
                    val r = indicatorR
                    drawLine(indicatorColor, Offset(x - r, indicatorY - r), Offset(x + r, indicatorY + r), strokeWidth = 2f)
                    drawLine(indicatorColor, Offset(x + r, indicatorY - r), Offset(x - r, indicatorY + r), strokeWidth = 2f)
                }
                0 -> {
                    // O  –  open circle
                    drawCircle(
                        color  = indicatorColor,
                        radius = indicatorR,
                        center = Offset(x, indicatorY),
                        style  = Stroke(width = 2f)
                    )
                }
            }
        }

        // ── 4. Barre ────────────────────────────────────────────────
        if (voicing.barreAtFret != null) {
            val barreStrings = voicing.frets.mapIndexedNotNull { idx, f ->
                if (f == voicing.barreAtFret) idx else null
            }
            if (barreStrings.size >= 2) {
                val cy       = dotY(voicing.barreAtFret)
                val startX   = sx(barreStrings.first())
                val endX     = sx(barreStrings.last())
                val barreH   = dotRadius * 1.4f
                drawRoundRect(
                    color        = dotFillColor,
                    topLeft      = Offset(startX - dotRadius, cy - barreH / 2f),
                    size         = Size(endX - startX + dotRadius * 2, barreH),
                    cornerRadius = CornerRadius(barreH / 2f, barreH / 2f)
                )
            }
        }

        // ── 5. Finger dots ──────────────────────────────────────────
        for (i in 0..5) {
            val fret = voicing.frets[i]
            if (fret > 0) {
                val isBarrePos = voicing.barreAtFret != null && fret == voicing.barreAtFret
                if (!isBarrePos) {
                    drawCircle(
                        color  = dotFillColor,
                        radius = dotRadius,
                        center = Offset(sx(i), dotY(fret))
                    )
                }
            }
        }

        // ── 6. Fret-number label (when nut is not shown) ────────────
        if (!showNut) {
            drawIntoCanvas { canvas ->
                val argb = AndroidColor.argb(
                    (labelColor.alpha * 255).toInt(),
                    (labelColor.red   * 255).toInt(),
                    (labelColor.green * 255).toInt(),
                    (labelColor.blue  * 255).toInt()
                )
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    textAlign   = Paint.Align.RIGHT
                    textSize    = 10.dp.toPx()
                    color       = argb
                }
                canvas.nativeCanvas.drawText(
                    "${baseFret}fr",
                    leftPad - 6.dp.toPx(),
                    topPad + fretSpacing * 0.5f + 4.dp.toPx(),
                    textPaint
                )
            }
        }
    }
}
