package com.lddev.scalefinder.ui.components.home_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.Theory
import kotlinx.coroutines.delay

@Composable
fun SuggestionsPanel(
    progression: List<Chord>,
    onChooseScale: (Scale) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(icon = Icons.Default.Search, title = stringResource(R.string.scale_suggestions))

        if (progression.isEmpty()) {
            Text(stringResource(R.string.add_chords_to_see_suggestions))
        } else {
            val ranked = Theory.suggestScalesForProgression(progression)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(ranked) { index, s ->
                    ScaleSuggestionCard(
                        index = index,
                        scaleName = s.scale.toString(),
                        rationale = s.rationale,
                        onChoose = { onChooseScale(s.scale) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScaleSuggestionCard(
    index: Int,
    scaleName: String,
    rationale: String,
    onChoose: () -> Unit,
) {
    val isTop = index == 0
    var pulseScale by remember { mutableFloatStateOf(1f) }
    val pulseAnimation =
        animateFloatAsState(
            targetValue = pulseScale,
            animationSpec =
                if (isTop) {
                    tween(durationMillis = 1000, delayMillis = index * 100)
                } else {
                    tween(durationMillis = 300, delayMillis = index * 50)
                },
            label = "pulse_scale",
        )

    LaunchedEffect(isTop) {
        if (isTop) {
            while (true) {
                delay(1500)
                delay(1500)
            }
        }
    }

    AnimatedVisibility(
        visible = true,
        enter =
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec =
                    tween(
                        durationMillis = 400,
                        delayMillis = index * 80,
                        easing = FastOutSlowInEasing,
                    ),
            ) +
                fadeIn(
                    animationSpec =
                        tween(
                            durationMillis = 400,
                            delayMillis = index * 80,
                        ),
                ),
        modifier = Modifier.scale(if (isTop) pulseAnimation.value else 1f),
    ) {
        Card {
            Column(Modifier.padding(8.dp)) {
                Text(scaleName, style = MaterialTheme.typography.titleSmall)
                Text(rationale, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val showScaleDesc = stringResource(R.string.content_show_scale_on_neck)
                OutlinedButton(
                    onClick = onChoose,
                    modifier = Modifier.height(48.dp).semantics { contentDescription = showScaleDesc },
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.show_on_neck))
                }
            }
        }
    }
}
