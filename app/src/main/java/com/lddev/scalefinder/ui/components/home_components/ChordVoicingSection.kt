package com.lddev.scalefinder.ui.components.home_components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.ChordVoicing
import kotlinx.coroutines.delay

@Composable
fun ChordVoicingSection(
    chord: Chord,
    voicings: List<ChordVoicing>,
    selectedVoicing: ChordVoicing?,
    onShowOnNeck: (ChordVoicing) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            icon = Icons.Default.Info,
            title = stringResource(R.string.chord_voicings),
            trailing = {
                Text(
                    chord.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                )
            },
        )

        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(voicings) { _, voicing ->
                VoicingCard(
                    voicing = voicing,
                    isSelected = voicing == selectedVoicing,
                    onShowOnNeck = { onShowOnNeck(voicing) },
                )
            }
        }
    }
}

@Composable
private fun VoicingCard(
    voicing: ChordVoicing,
    isSelected: Boolean,
    onShowOnNeck: () -> Unit,
) {
    val voicingCardDesc = stringResource(R.string.content_chord_voicing_card, voicing.name)

    var cardScale by remember { mutableFloatStateOf(0.9f) }
    val scaleAnim =
        animateFloatAsState(
            targetValue = cardScale,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "voicing_card_scale",
        )

    LaunchedEffect(voicing) {
        delay(50)
    }

    OutlinedCard(
        modifier =
            Modifier
                .scale(scaleAnim.value)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.shapes.medium,
                        )
                    } else {
                        Modifier
                    },
                )
                .semantics { contentDescription = voicingCardDesc },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp),
        ) {
            ChordDiagramView(voicing = voicing)

            Spacer(Modifier.height(6.dp))

            Text(
                voicing.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )

            Spacer(Modifier.height(6.dp))

            val showOnNeckDesc = stringResource(R.string.content_show_voicing_on_neck)
            OutlinedButton(
                onClick = onShowOnNeck,
                modifier =
                    Modifier
                        .height(32.dp)
                        .semantics { contentDescription = showOnNeckDesc },
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    stringResource(R.string.show_voicing_on_neck),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
