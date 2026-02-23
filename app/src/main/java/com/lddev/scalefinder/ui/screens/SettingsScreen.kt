package com.lddev.scalefinder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.FretboardTheme
import com.lddev.scalefinder.model.Tuning
import com.lddev.scalefinder.ui.HomeViewModel

@Composable
fun SettingsScreen(
    vm: HomeViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(4.dp))

            SectionLabel(stringResource(R.string.settings_display))

            SettingsToggleRow(
                title = stringResource(R.string.settings_theme),
                subtitle = if (isDark) stringResource(R.string.settings_theme_dark)
                else stringResource(R.string.settings_theme_light),
                checked = isDark,
                onCheckedChange = { onToggleTheme() }
            )

            SettingsToggleRow(
                title = stringResource(R.string.contrast_high),
                subtitle = if (vm.highContrast) stringResource(R.string.contrast_high)
                else stringResource(R.string.contrast_normal),
                checked = vm.highContrast,
                onCheckedChange = { vm.toggleHighContrast() }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(48.dp))

            SectionLabel(stringResource(R.string.settings_fretboard_section))

            TuningSelector(
                selectedTuning = vm.selectedTuning,
                onTuningChanged = vm::setTuning
            )

            FretboardThemeSelector(
                selectedTheme = vm.fretboardTheme,
                onThemeChanged = vm::updateFretboardTheme
            )

            SettingsToggleRow(
                title = stringResource(R.string.fretboard_inverted),
                subtitle = if (vm.invertFretboard) stringResource(R.string.fretboard_inverted)
                else stringResource(R.string.fretboard_normal),
                checked = vm.invertFretboard,
                onCheckedChange = { vm.toggleInvertFretboard() }
            )

            SettingsToggleRow(
                title = stringResource(R.string.note_names_show),
                subtitle = if (vm.showNoteNames) stringResource(R.string.note_names_show)
                else stringResource(R.string.note_names_hide),
                checked = vm.showNoteNames,
                onCheckedChange = { vm.toggleShowNoteNames() }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun TuningSelector(
    selectedTuning: Tuning,
    onTuningChanged: (Tuning) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tuning),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = selectedTuning.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "▼",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                Tuning.all().forEach { tuning ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                tuning.name,
                                fontWeight = if (tuning == selectedTuning) FontWeight.Bold
                                else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onTuningChanged(tuning)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FretboardThemeSelector(
    selectedTheme: FretboardTheme,
    onThemeChanged: (FretboardTheme) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_fretboard_theme),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FretboardTheme.all().forEach { theme ->
                    val selected = theme == selectedTheme
                    Card(
                        onClick = { onThemeChanged(theme) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (selected) BorderStroke(
                            2.dp, MaterialTheme.colorScheme.primary
                        ) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Canvas(Modifier.matchParentSize()) {
                                    drawRect(color = theme.colors.background)
                                    drawLine(
                                        color = theme.colors.fretBody,
                                        start = Offset(size.width * 0.5f, 0f),
                                        end = Offset(size.width * 0.5f, size.height),
                                        strokeWidth = 2f
                                    )
                                    drawLine(
                                        color = theme.colors.woundString,
                                        start = Offset(0f, size.height * 0.35f),
                                        end = Offset(size.width, size.height * 0.35f),
                                        strokeWidth = 2.5f
                                    )
                                    drawLine(
                                        color = theme.colors.plainString,
                                        start = Offset(0f, size.height * 0.65f),
                                        end = Offset(size.width, size.height * 0.65f),
                                        strokeWidth = 1.5f
                                    )
                                    drawCircle(
                                        color = theme.colors.inlayBody,
                                        radius = 3f,
                                        center = Offset(size.width * 0.25f, size.height * 0.5f)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(theme.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
