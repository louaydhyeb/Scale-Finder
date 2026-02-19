package com.lddev.scalefinder.ui.components.quiz_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lddev.scalefinder.R
import com.lddev.scalefinder.ui.QuizAnswer

@Composable
fun WrongAnswerCard(answer: QuizAnswer) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                answer.question.prompt,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            if (answer.selectedIndex >= 0) {
                Text(
                    stringResource(R.string.quiz_your_answer, answer.question.choices[answer.selectedIndex]),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    stringResource(R.string.quiz_no_answer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                stringResource(R.string.quiz_correct_answer, answer.question.choices[answer.question.correctIndex]),
                style = MaterialTheme.typography.bodySmall,
                color = CorrectGreen
            )
        }
    }
}
