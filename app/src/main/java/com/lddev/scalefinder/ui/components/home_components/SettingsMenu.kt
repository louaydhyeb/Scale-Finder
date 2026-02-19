package com.lddev.scalefinder.ui.components.home_components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Tuning

@Composable
fun SettingsMenu(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    selectedTuning: Tuning,
    onTuningChanged: (Tuning) -> Unit,
    highContrast: Boolean,
    onToggleHighContrast: () -> Unit,
    invert: Boolean,
    onToggleInvert: () -> Unit,
    showNoteNames: Boolean,
    onToggleShowNoteNames: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var tuningMenuExpanded by remember { mutableStateOf(false) }
    var themeRotation by remember { mutableFloatStateOf(0f) }
    val rotation = animateFloatAsState(
        targetValue = themeRotation,
        animationSpec = tween(durationMillis = 300),
        label = "theme_rotation"
    )

    val settingsMenuDesc = stringResource(R.string.content_settings_menu)
    val settingsDesc = stringResource(R.string.settings)

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = settingsMenuDesc }
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = settingsDesc,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SettingsMenuItem(
                icon = Icons.Default.Settings,
                iconRotation = rotation.value,
                label = if (isDark) stringResource(R.string.theme_light) else stringResource(R.string.theme_dark),
                onClick = {
                    themeRotation = if (themeRotation == 0f) 180f else 0f
                    onToggleTheme()
                }
            )

            DropdownMenuItem(
                text = {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                stringResource(R.string.tuning),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            selectedTuning.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                onClick = { tuningMenuExpanded = true }
            )

            SettingsToggleItem(
                icon = Icons.Default.Info,
                isActive = highContrast,
                activeLabel = stringResource(R.string.contrast_high),
                inactiveLabel = stringResource(R.string.contrast_normal),
                onClick = onToggleHighContrast
            )

            SettingsToggleItem(
                icon = Icons.Default.Settings,
                isActive = invert,
                activeLabel = stringResource(R.string.fretboard_inverted),
                inactiveLabel = stringResource(R.string.fretboard_normal),
                onClick = onToggleInvert
            )

            SettingsToggleItem(
                icon = Icons.Default.Info,
                isActive = showNoteNames,
                activeLabel = stringResource(R.string.note_names_hide),
                inactiveLabel = stringResource(R.string.note_names_show),
                onClick = onToggleShowNoteNames
            )
        }

        DropdownMenu(
            expanded = tuningMenuExpanded,
            onDismissRequest = { tuningMenuExpanded = false }
        ) {
            Tuning.all().forEach { tuning ->
                DropdownMenuItem(
                    text = {
                        Text(
                            tuning.name,
                            fontWeight = if (tuning == selectedTuning) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onTuningChanged(tuning)
                        tuningMenuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconRotation: Float = 0f
) {
    DropdownMenuItem(
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(iconRotation),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        },
        onClick = onClick
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    isActive: Boolean,
    activeLabel: String,
    inactiveLabel: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    if (isActive) activeLabel else inactiveLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        },
        onClick = onClick
    )
}
