package com.lddev.scalefinder.ui.components.quiz_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AnswerOption(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    onClick: () -> Unit,
) {
    val borderColor =
        when {
            showResult && isCorrect -> CorrectGreen
            showResult && isSelected -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    val containerColor =
        when {
            showResult && isCorrect -> CorrectGreen.copy(alpha = 0.1f)
            showResult && isSelected -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surface
        }

    OutlinedCard(
        onClick = { if (!showResult) onClick() },
        modifier = Modifier.fillMaxWidth(),
        border =
            BorderStroke(
                width = if ((showResult && isCorrect) || isSelected) 2.dp else 1.dp,
                color = borderColor,
            ),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight =
                    if (isSelected || (showResult && isCorrect)) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                modifier = Modifier.weight(1f),
            )
            if (showResult && isCorrect) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = CorrectGreen,
                    modifier = Modifier.size(20.dp),
                )
            } else if (showResult && isSelected) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
