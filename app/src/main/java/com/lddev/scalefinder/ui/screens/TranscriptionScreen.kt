package com.lddev.scalefinder.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Articulation
import com.lddev.scalefinder.model.Tablature
import com.lddev.scalefinder.model.Tuning
import com.lddev.scalefinder.ui.TranscriptionViewModel

@Composable
fun TranscriptionScreen(
    modifier: Modifier = Modifier,
    vm: TranscriptionViewModel = viewModel()
) {
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { vm.transcribe(it) }
    }

    val midiSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/midi")
    ) { uri: Uri? ->
        uri?.let { vm.exportMidi(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.transcription_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        when (vm.state) {
            TranscriptionViewModel.State.IDLE ->
                IdleContent(
                    isModelAvailable = vm.isModelAvailable,
                    selectedTuning = vm.selectedTuning,
                    onTuningChanged = vm::setTuning,
                    onSelectFile = { fileLauncher.launch(arrayOf("audio/*", "video/*")) }
                )

            TranscriptionViewModel.State.DECODING,
            TranscriptionViewModel.State.ANALYZING,
            TranscriptionViewModel.State.MAPPING ->
                ProcessingContent(state = vm.state, progress = vm.progress)

            TranscriptionViewModel.State.DONE ->
                DoneContent(
                    tablature = vm.tablature,
                    tuning = vm.selectedTuning,
                    noteCount = vm.detectedNotes.size,
                    midiExported = vm.midiExported,
                    onTuningChanged = vm::setTuning,
                    onExportMidi = { midiSaveLauncher.launch("transcription.mid") },
                    onReset = vm::reset
                )

            TranscriptionViewModel.State.ERROR ->
                ErrorContent(message = vm.errorMessage, onRetry = vm::reset)
        }
    }
}

// ── Idle ─────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    isModelAvailable: Boolean,
    selectedTuning: Tuning,
    onTuningChanged: (Tuning) -> Unit,
    onSelectFile: () -> Unit
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!isModelAvailable) {
                ModelMissingBanner()
            }

            Text(
                stringResource(R.string.transcription_description),
                style = MaterialTheme.typography.bodyMedium
            )

            TuningSelector(selectedTuning, onTuningChanged)

            Button(
                onClick = onSelectFile,
                enabled = isModelAvailable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.transcription_select_file))
            }
        }
    }
}

@Composable
private fun ModelMissingBanner() {
    OutlinedCard(
        Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.transcription_model_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ── Processing ───────────────────────────────────────────────────────

@Composable
private fun ProcessingContent(
    state: TranscriptionViewModel.State,
    progress: Float
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = when (state) {
                    TranscriptionViewModel.State.DECODING ->
                        stringResource(R.string.transcription_decoding)
                    TranscriptionViewModel.State.ANALYZING ->
                        stringResource(R.string.transcription_analyzing)
                    TranscriptionViewModel.State.MAPPING ->
                        stringResource(R.string.transcription_mapping)
                    else -> ""
                },
                style = MaterialTheme.typography.titleMedium
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Done ─────────────────────────────────────────────────────────────

@Composable
private fun DoneContent(
    tablature: Tablature?,
    tuning: Tuning,
    noteCount: Int,
    midiExported: Boolean,
    onTuningChanged: (Tuning) -> Unit,
    onExportMidi: () -> Unit,
    onReset: () -> Unit
) {
    if (tablature == null || tablature.events.isEmpty()) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.transcription_no_notes),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onReset) {
                    Text(stringResource(R.string.transcription_try_again))
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.transcription_done_summary, noteCount, tablature.events.size),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.transcription_new))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TuningSelector(tuning, onTuningChanged)
            MidiExportButton(exported = midiExported, onClick = onExportMidi)
        }

        TablatureCard(tablature)

        AsciiTablatureCard(tablature)
    }
}

@Composable
private fun MidiExportButton(exported: Boolean, onClick: () -> Unit) {
    if (exported) {
        FilledTonalButton(onClick = onClick) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.transcription_midi_saved))
        }
    } else {
        FilledTonalButton(onClick = onClick) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.transcription_export_midi))
        }
    }
}

// ── Tablature Canvas ─────────────────────────────────────────────────

@Composable
private fun TablatureCard(tablature: Tablature) {
    OutlinedCard(Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.transcription_tab_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            TablatureCanvas(tablature)
        }
    }
}

@Composable
private fun TablatureCanvas(tablature: Tablature) {
    val density = LocalDensity.current
    val lineColor = MaterialTheme.colorScheme.outline
    val noteColor = MaterialTheme.colorScheme.primary
    val noteBgColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val stringLabels = listOf("e", "B", "G", "D", "A", "E")
    val lineSpacingDp = 28.dp
    val noteWidthDp = 36.dp
    val labelWidthDp = 24.dp

    val lineSpacingPx = with(density) { lineSpacingDp.toPx() }
    val noteWidthPx = with(density) { noteWidthDp.toPx() }
    val labelWidthPx = with(density) { labelWidthDp.toPx() }
    val canvasHeight = lineSpacingDp * 6 + 8.dp
    val canvasWidth = labelWidthDp + noteWidthDp * (tablature.events.size + 1)

    val scrollState = rememberScrollState()

    Box(Modifier.horizontalScroll(scrollState)) {
        Canvas(
            modifier = Modifier
                .width(canvasWidth)
                .height(canvasHeight)
        ) {
            val yBase = lineSpacingPx * 0.5f

            val labelPaint = android.graphics.Paint().apply {
                color = labelColor.toArgbCompat()
                textSize = with(density) { 13.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.MONOSPACE
            }

            for (s in 0 until 6) {
                val y = yBase + s * lineSpacingPx
                drawContext.canvas.nativeCanvas.drawText(
                    stringLabels[s],
                    labelWidthPx / 2f,
                    y + labelPaint.textSize / 3f,
                    labelPaint
                )
                drawLine(
                    color = lineColor,
                    start = Offset(labelWidthPx, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5f
                )
            }

            val fretPaint = android.graphics.Paint().apply {
                color = noteColor.toArgbCompat()
                textSize = with(density) { 14.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
                typeface = android.graphics.Typeface.MONOSPACE
            }

            val artPaint = android.graphics.Paint().apply {
                color = textColor.toArgbCompat()
                textSize = with(density) { 10.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.MONOSPACE
            }

            tablature.events.forEachIndexed { idx, event ->
                val x = labelWidthPx + (idx + 0.5f) * noteWidthPx
                for (note in event.notes) {
                    val displayString = 5 - note.string
                    if (displayString !in 0..5) continue
                    val y = yBase + displayString * lineSpacingPx
                    val text = note.fret.toString()
                    val textW = fretPaint.measureText(text)

                    drawCircle(
                        color = noteBgColor,
                        radius = maxOf(textW, fretPaint.textSize) * 0.65f,
                        center = Offset(x, y)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        x,
                        y + fretPaint.textSize / 3f,
                        fretPaint
                    )

                    if (note.articulations.isNotEmpty()) {
                        val artStr = note.articulations.joinToString("") { it.symbol }
                        drawContext.canvas.nativeCanvas.drawText(
                            artStr,
                            x + textW * 0.5f + artPaint.measureText(artStr) * 0.5f + 2f,
                            y - fretPaint.textSize * 0.35f,
                            artPaint
                        )
                    }
                }
            }
        }
    }
}

private fun Color.toArgbCompat(): Int {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

// ── ASCII Tab ────────────────────────────────────────────────────────

@Composable
private fun AsciiTablatureCard(tablature: Tablature) {
    val ascii = remember(tablature) { tablature.toAscii() }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.transcription_ascii_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    text = ascii,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ── Error ────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String?, onRetry: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                stringResource(R.string.transcription_error),
                style = MaterialTheme.typography.titleMedium
            )
            if (message != null) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            FilledTonalButton(onClick = onRetry) {
                Text(stringResource(R.string.transcription_try_again))
            }
        }
    }
}

// ── Shared Components ────────────────────────────────────────────────

@Composable
private fun TuningSelector(selected: Tuning, onChanged: (Tuning) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.tuning_label, selected.name))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Tuning.all().forEach { tuning ->
                DropdownMenuItem(
                    text = { Text(tuning.name) },
                    onClick = {
                        onChanged(tuning)
                        expanded = false
                    }
                )
            }
        }
    }
}
