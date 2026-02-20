package com.lddev.scalefinder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lddev.scalefinder.R
import com.lddev.scalefinder.ui.QuizCategory
import com.lddev.scalefinder.ui.QuizDifficulty
import com.lddev.scalefinder.ui.QuizPhase
import com.lddev.scalefinder.ui.QuizViewModel
import com.lddev.scalefinder.ui.components.home_components.ChordDiagramView
import com.lddev.scalefinder.ui.components.home_components.FretHighlight
import com.lddev.scalefinder.ui.components.home_components.SectionHeader
import com.lddev.scalefinder.ui.components.home_components.Stepper
import com.lddev.scalefinder.ui.components.quiz_components.AnswerOption
import com.lddev.scalefinder.ui.components.quiz_components.CategoryCard
import com.lddev.scalefinder.ui.components.quiz_components.CorrectGreen
import com.lddev.scalefinder.ui.components.quiz_components.EarTrainingCard
import com.lddev.scalefinder.ui.components.quiz_components.FretboardCard
import com.lddev.scalefinder.ui.components.quiz_components.GoldStar
import com.lddev.scalefinder.ui.components.quiz_components.StatsCard
import com.lddev.scalefinder.ui.components.quiz_components.WrongAnswerCard

private val categoryIcons = mapOf(
    QuizCategory.FRETBOARD_NOTE to Icons.Default.Create,
    QuizCategory.DIATONIC_CHORD to Icons.Default.Star,
    QuizCategory.SCALE_IDENTIFICATION to Icons.Default.Search,
    QuizCategory.EAR_NOTE to Icons.Default.PlayArrow,
    QuizCategory.EAR_CHORD to Icons.Default.PlayArrow,
    QuizCategory.CHORD_VOICING to Icons.Default.Info
)

@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    vm: QuizViewModel = viewModel()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        fontWeight = FontWeight.Bold
                    )
                ) { append(stringResource(R.string.quiz_title)) }
            },
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(16.dp))

        when (vm.phase) {
            QuizPhase.SETUP -> QuizSetupContent(vm)
            QuizPhase.SESSION -> QuizSessionContent(vm)
            QuizPhase.RESULTS -> QuizResultsContent(vm)
        }
    }
}

// ── Setup ──────────────────────────────────────────────────────────

@Composable
private fun QuizSetupContent(vm: QuizViewModel) {
    SectionHeader(icon = Icons.Default.Star, title = stringResource(R.string.quiz_choose_category))
    Spacer(Modifier.height(12.dp))

    QuizCategory.entries.forEach { category ->
        CategoryCard(
            icon = categoryIcons[category] ?: Icons.Default.Star,
            title = stringResource(category.titleRes),
            description = stringResource(category.descriptionRes),
            selected = category == vm.selectedCategory,
            onClick = { vm.selectCategory(category) }
        )
        Spacer(Modifier.height(8.dp))
    }

    val stats = vm.categoryStats
    if (stats.quizzesPlayed > 0) {
        Spacer(Modifier.height(4.dp))
        StatsCard(stats = stats)
    }

    Spacer(Modifier.height(12.dp))

    SectionHeader(icon = Icons.Default.Star, title = stringResource(R.string.quiz_difficulty))
    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuizDifficulty.entries.forEach { difficulty ->
            if (difficulty == vm.selectedDifficulty) {
                Button(onClick = { vm.selectDifficulty(difficulty) }) {
                    Text(stringResource(difficulty.titleRes))
                }
            } else {
                OutlinedButton(onClick = { vm.selectDifficulty(difficulty) }) {
                    Text(stringResource(difficulty.titleRes))
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.quiz_timed_mode), style = MaterialTheme.typography.titleSmall)
        Switch(checked = vm.timedMode, onCheckedChange = { vm.toggleTimedMode() })
    }

    Spacer(Modifier.height(12.dp))

    Stepper(
        label = stringResource(R.string.quiz_questions),
        value = vm.questionCount,
        onChange = vm::updateQuestionCount
    )

    Spacer(Modifier.height(16.dp))

    Button(onClick = { vm.startQuiz() }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.quiz_start))
    }

    val total = vm.totalQuizzesPlayed
    if (total > 0) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.quiz_total_quizzes, total),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Session ────────────────────────────────────────────────────────

@Composable
private fun QuizSessionContent(vm: QuizViewModel) {
    val question = vm.currentQuestion ?: return

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.quiz_question_of, vm.currentQuestionIndex + 1, vm.questionCount),
            style = MaterialTheme.typography.titleSmall
        )
        if (vm.timedMode) {
            val timerColor = when {
                vm.timeRemaining > 10 -> MaterialTheme.colorScheme.onSurface
                vm.timeRemaining > 5 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Text(
                stringResource(R.string.quiz_time_remaining, vm.timeRemaining),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = timerColor
            )
        }
        Text(
            stringResource(R.string.quiz_score_label, vm.score),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }

    Spacer(Modifier.height(8.dp))

    LinearProgressIndicator(
        progress = { (vm.currentQuestionIndex + 1).toFloat() / vm.questionCount },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp)),
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )

    Spacer(Modifier.height(16.dp))

    Text(question.prompt, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)

    Spacer(Modifier.height(12.dp))

    when (question.category) {
        QuizCategory.FRETBOARD_NOTE -> {
            if (question.highlightString != null && question.highlightFret != null) {
                FretboardCard(
                    highlights = listOf(
                        FretHighlight(
                            stringIndex = question.highlightString,
                            fret = question.highlightFret,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        QuizCategory.SCALE_IDENTIFICATION -> {
            if (question.scale != null) {
                FretboardCard(scale = question.scale)
                Spacer(Modifier.height(12.dp))
            }
        }
        QuizCategory.EAR_NOTE, QuizCategory.EAR_CHORD -> {
            EarTrainingCard(onPlay = { vm.playCurrentQuestionAudio() })
            Spacer(Modifier.height(12.dp))
        }
        QuizCategory.CHORD_VOICING -> {
            if (question.chordVoicing != null) {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ChordDiagramView(voicing = question.chordVoicing)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        QuizCategory.DIATONIC_CHORD -> {}
    }

    question.choices.forEachIndexed { index, choice ->
        AnswerOption(
            text = choice,
            isSelected = vm.selectedAnswerIndex == index,
            isCorrect = index == question.correctIndex,
            showResult = vm.hasAnswered,
            onClick = { vm.selectAnswer(index) }
        )
        if (index < question.choices.lastIndex) Spacer(Modifier.height(8.dp))
    }

    if (vm.hasAnswered) {
        Spacer(Modifier.height(12.dp))

        val feedbackText = when {
            vm.isTimedOut -> stringResource(R.string.quiz_timed_out)
            vm.selectedAnswerIndex == question.correctIndex -> stringResource(R.string.quiz_correct)
            else -> stringResource(R.string.quiz_wrong, question.choices[question.correctIndex])
        }
        val feedbackColor = when {
            vm.isTimedOut -> MaterialTheme.colorScheme.error
            vm.selectedAnswerIndex == question.correctIndex -> CorrectGreen
            else -> MaterialTheme.colorScheme.error
        }
        Text(
            text = feedbackText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = feedbackColor
        )

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = { vm.nextQuestion() }) {
                Text(
                    if (vm.currentQuestionIndex < vm.questionCount - 1)
                        stringResource(R.string.quiz_next)
                    else stringResource(R.string.quiz_see_results)
                )
            }
        }
    }
}

// ── Results ────────────────────────────────────────────────────────

@Composable
private fun QuizResultsContent(vm: QuizViewModel) {
    val accuracy = if (vm.questionCount > 0) (vm.score * 100) / vm.questionCount else 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Icon(
            imageVector = if (accuracy >= 70) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = stringResource(R.string.content_quiz_result),
            modifier = Modifier.size(64.dp),
            tint = when {
                accuracy >= 70 -> CorrectGreen
                accuracy >= 40 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.quiz_complete),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.quiz_score_result, vm.score, vm.questionCount),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            stringResource(R.string.quiz_accuracy, accuracy),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { vm.score.toFloat() / vm.questionCount.coerceAtLeast(1) },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(4.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        val stats = vm.categoryStats
        if (vm.isNewBest) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = stringResource(R.string.content_new_best), tint = GoldStar, modifier = Modifier.size(24.dp))
                Text(
                    stringResource(R.string.quiz_new_best),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldStar
                )
                Icon(Icons.Default.Star, contentDescription = null, tint = GoldStar, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(4.dp))
        }

        if (stats.quizzesPlayed > 1) {
            Text(
                stringResource(R.string.quiz_your_best, stats.bestAccuracy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.quiz_your_avg, stats.averageAccuracy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(vm.selectedCategory.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { vm.goToSetup() }) {
                Text(stringResource(R.string.quiz_change_category))
            }
            Button(onClick = { vm.restartQuiz() }) {
                Text(stringResource(R.string.quiz_try_again))
            }
        }
    }

    val wrongAnswers = vm.wrongAnswers
    if (wrongAnswers.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        SectionHeader(
            icon = Icons.Default.Close,
            title = stringResource(R.string.quiz_review_wrong) + " (${wrongAnswers.size})"
        )
        Spacer(Modifier.height(8.dp))
        wrongAnswers.forEach { answer ->
            WrongAnswerCard(answer)
            Spacer(Modifier.height(8.dp))
        }
    } else {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.quiz_all_correct),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = CorrectGreen
        )
    }
}
