package com.lddev.scalefinder.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Tuning
import com.lddev.scalefinder.ui.TunerViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val TunerGreen = Color(0xFF22C55E)
private val TunerYellow = Color(0xFFF59E0B)
private val TunerRed = Color(0xFFEF4444)

@Composable
fun TunerScreen(vm: TunerViewModel = viewModel()) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) vm.startListening()
    }

    DisposableEffect(hasPermission) {
        if (hasPermission) vm.startListening()
        onDispose { vm.stopListening() }
    }

    if (!hasPermission) {
        PermissionRequest(
            onRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )
    } else {
        TunerContent(vm)
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.tuner_permission_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tuner_permission_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onRequest) {
                    Text(stringResource(R.string.tuner_grant_permission))
                }
            }
        }
    }
}

@Composable
private fun TunerContent(vm: TunerViewModel) {
    val pitch = vm.currentPitch
    val centsFromTarget = vm.centsFromTarget()

    val displayCents = when {
        pitch == null -> 0f
        centsFromTarget != null -> centsFromTarget.coerceIn(-50f, 50f)
        else -> pitch.cents.coerceIn(-50f, 50f)
    }

    val animatedCents by animateFloatAsState(
        targetValue = displayCents,
        animationSpec = tween(durationMillis = 150),
        label = "cents"
    )

    val accuracy = when {
        pitch == null -> TunerAccuracy.NO_SIGNAL
        abs(displayCents) <= 3f -> TunerAccuracy.IN_TUNE
        abs(displayCents) <= 15f -> TunerAccuracy.CLOSE
        else -> TunerAccuracy.OFF
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StringSelectorPanel(
            vm = vm,
            modifier = Modifier.fillMaxHeight()
        )

        GaugePanel(
            vm = vm,
            pitch = pitch,
            animatedCents = animatedCents,
            accuracy = accuracy,
            displayCents = displayCents,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
private fun StringSelectorPanel(vm: TunerViewModel, modifier: Modifier = Modifier) {
    var showTuningMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            OutlinedButton(
                onClick = { showTuningMenu = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 10.dp, vertical = 4.dp
                )
            ) {
                Text(
                    text = vm.selectedTuning.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
            DropdownMenu(
                expanded = showTuningMenu,
                onDismissRequest = { showTuningMenu = false }
            ) {
                Tuning.all().forEach { tuning ->
                    DropdownMenuItem(
                        text = { Text(tuning.name) },
                        onClick = {
                            vm.selectTuning(tuning)
                            showTuningMenu = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .background(
                    color = if (vm.targetStringIndex < 0)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable { vm.selectTargetString(-1) }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.tuner_auto),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (vm.targetStringIndex < 0) FontWeight.Bold else FontWeight.Normal,
                color = if (vm.targetStringIndex < 0)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val stringCount = vm.selectedTuning.openNotes.size
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in (stringCount - 1) downTo 0) {
                val isSelected = vm.targetStringIndex == i
                StringButton(
                    label = vm.getStringLabel(i),
                    isSelected = isSelected,
                    onClick = { vm.selectTargetString(i) }
                )
            }
        }
    }
}

@Composable
private fun StringButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "stringBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "stringText"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(bgColor, CircleShape)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun GaugePanel(
    vm: TunerViewModel,
    pitch: com.lddev.scalefinder.audio.PitchResult?,
    animatedCents: Float,
    accuracy: TunerAccuracy,
    displayCents: Float,
    modifier: Modifier = Modifier
) {
    val accentColor by animateColorAsState(
        targetValue = when (accuracy) {
            TunerAccuracy.IN_TUNE -> TunerGreen
            TunerAccuracy.CLOSE -> TunerYellow
            TunerAccuracy.OFF -> TunerRed
            TunerAccuracy.NO_SIGNAL -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(300),
        label = "accent"
    )

    val noteDisplay = when {
        pitch == null -> "–"
        vm.targetStringIndex >= 0 -> vm.getStringLabel(vm.targetStringIndex)
        else -> "${pitch.noteName}${pitch.octave}"
    }

    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            drawTunerGauge(
                cents = animatedCents,
                accentColor = accentColor,
                noteDisplay = noteDisplay,
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                outline = outline,
                surfaceVariant = surfaceVariant,
                textMeasurer = textMeasurer
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            if (pitch != null) {
                Text(
                    text = "%.1f Hz".format(pitch.frequency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "%+.0f cents".format(displayCents),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            } else {
                Text(
                    text = stringResource(R.string.tuner_no_signal),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (accuracy == TunerAccuracy.IN_TUNE) {
            Text(
                text = stringResource(R.string.tuner_in_tune),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TunerGreen,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

private fun DrawScope.drawTunerGauge(
    cents: Float,
    accentColor: Color,
    noteDisplay: String,
    onSurface: Color,
    onSurfaceVariant: Color,
    outline: Color,
    surfaceVariant: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val w = size.width
    val h = size.height
    val gaugeRadius = minOf(w * 0.42f, h * 0.65f)
    val cx = w / 2f
    val cy = h * 0.62f

    val arcStartDeg = 200f
    val arcSweepDeg = 140f
    val arcCenterDeg = arcStartDeg + arcSweepDeg / 2f

    // Background arc
    drawArc(
        color = surfaceVariant,
        startAngle = arcStartDeg,
        sweepAngle = arcSweepDeg,
        useCenter = false,
        topLeft = Offset(cx - gaugeRadius, cy - gaugeRadius),
        size = androidx.compose.ui.geometry.Size(gaugeRadius * 2, gaugeRadius * 2),
        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
    )

    // Colored center segment (shows "in tune" zone, ±5 cents)
    val zoneSweep = arcSweepDeg * (10f / 100f)
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(TunerYellow, TunerGreen, TunerGreen, TunerYellow),
        ),
        startAngle = arcCenterDeg - zoneSweep / 2f,
        sweepAngle = zoneSweep,
        useCenter = false,
        topLeft = Offset(cx - gaugeRadius, cy - gaugeRadius),
        size = androidx.compose.ui.geometry.Size(gaugeRadius * 2, gaugeRadius * 2),
        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
    )

    // Tick marks
    val tickCount = 21
    for (i in 0..tickCount - 1) {
        val frac = i.toFloat() / (tickCount - 1)
        val angle = arcStartDeg + frac * arcSweepDeg
        val rad = Math.toRadians(angle.toDouble())
        val isMajor = i % 5 == 0
        val tickLen = if (isMajor) 16.dp.toPx() else 8.dp.toPx()
        val outerR = gaugeRadius + 14.dp.toPx()
        val innerR = outerR + tickLen

        drawLine(
            color = if (isMajor) onSurfaceVariant else outline,
            start = Offset(
                cx + outerR * cos(rad).toFloat(),
                cy + outerR * sin(rad).toFloat()
            ),
            end = Offset(
                cx + innerR * cos(rad).toFloat(),
                cy + innerR * sin(rad).toFloat()
            ),
            strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Flat/sharp labels
    val labelRadius = gaugeRadius + 40.dp.toPx()
    val flatAngle = Math.toRadians(arcStartDeg.toDouble())
    val sharpAngle = Math.toRadians((arcStartDeg + arcSweepDeg).toDouble())

    val flatLayout = textMeasurer.measure(
        "♭",
        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariant)
    )
    drawText(
        flatLayout,
        topLeft = Offset(
            cx + labelRadius * cos(flatAngle).toFloat() - flatLayout.size.width / 2f,
            cy + labelRadius * sin(flatAngle).toFloat() - flatLayout.size.height / 2f
        )
    )
    val sharpLayout = textMeasurer.measure(
        "♯",
        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurfaceVariant)
    )
    drawText(
        sharpLayout,
        topLeft = Offset(
            cx + labelRadius * cos(sharpAngle).toFloat() - sharpLayout.size.width / 2f,
            cy + labelRadius * sin(sharpAngle).toFloat() - sharpLayout.size.height / 2f
        )
    )

    // Needle
    val needleAngle = arcCenterDeg + (cents / 50f) * (arcSweepDeg / 2f)
    val needleRad = Math.toRadians(needleAngle.toDouble())
    val needleLen = gaugeRadius - 10.dp.toPx()

    drawLine(
        color = accentColor,
        start = Offset(cx, cy),
        end = Offset(
            cx + needleLen * cos(needleRad).toFloat(),
            cy + needleLen * sin(needleRad).toFloat()
        ),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Pivot circle
    drawCircle(
        color = accentColor,
        radius = 8.dp.toPx(),
        center = Offset(cx, cy)
    )
    drawCircle(
        color = onSurface,
        radius = 4.dp.toPx(),
        center = Offset(cx, cy)
    )

    // Note text centered below the gauge pivot
    val noteLayout = textMeasurer.measure(
        noteDisplay,
        style = TextStyle(
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface
        )
    )
    drawText(
        noteLayout,
        topLeft = Offset(
            cx - noteLayout.size.width / 2f,
            cy + 16.dp.toPx()
        )
    )
}

private enum class TunerAccuracy {
    NO_SIGNAL, OFF, CLOSE, IN_TUNE
}
