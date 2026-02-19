package com.lddev.scalefinder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.Tuning
import com.lddev.scalefinder.ui.QuizCategory
import com.lddev.scalefinder.ui.QuizDifficulty
import com.lddev.scalefinder.ui.QuizPhase
import com.lddev.scalefinder.ui.QuizViewModel
import com.lddev.scalefinder.ui.components.ChordDiagramView
import com.lddev.scalefinder.ui.components.FretHighlight
import com.lddev.scalefinder.ui.components.GuitarFretboard
import com.lddev.scalefinder.ui.components.SectionHeader
import com.lddev.scalefinder.ui.components.Stepper

private val CorrectGreen = Color(0xFF4CAF50)

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

private val categoryIcons = mapOf(
    QuizCategory.FRETBOARD_NOTE to Icons.Default.Create,
    QuizCategory.DIATONIC_CHORD to Icons.Default.Star,
    QuizCategory.SCALE_IDENTIFICATION to Icons.Default.Search,
    QuizCategory.EAR_NOTE to Icons.Default.PlayArrow,
    QuizCategory.EAR_CHORD to Icons.Default.PlayArrow,
    QuizCategory.CHORD_VOICING to Icons.Default.Info
)

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

    Spacer(Modifier.height(12.dp))

    // Difficulty selector
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

    // Timer toggle
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            stringResource(R.string.quiz_timed_mode),
            style = MaterialTheme.typography.titleSmall
        )
        Switch(
            checked = vm.timedMode,
            onCheckedChange = { vm.toggleTimedMode() }
        )
    }

    Spacer(Modifier.height(12.dp))

    // Question count
    Stepper(
        label = stringResource(R.string.quiz_questions),
        value = vm.questionCount,
        onChange = vm::updateQuestionCount
    )

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = { vm.startQuiz() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.quiz_start))
    }
}

@Composable
private fun CategoryCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Session ────────────────────────────────────────────────────────

@Composable
private fun QuizSessionContent(vm: QuizViewModel) {
    val question = vm.currentQuestion ?: return

    // Header: question counter, timer, score
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

    Text(
        question.prompt,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium
    )

    Spacer(Modifier.height(12.dp))

    // Visual prompt based on category
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

    // Answer choices
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

    // Feedback + navigation
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

@Composable
private fun FretboardCard(
    scale: Scale? = null,
    highlights: List<FretHighlight> = emptyList()
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        GuitarFretboard(
            modifier = Modifier.padding(8.dp),
            tuning = Tuning.STANDARD,
            scale = scale,
            fretStart = 0,
            fretCount = 12,
            highlights = highlights,
            invertStrings = true,
            showNoteNames = false
        )
    }
}

@Composable
private fun EarTrainingCard(onPlay: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onPlay) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.quiz_play_sound))
            }
        }
    }
}

@Composable
private fun AnswerOption(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        showResult && isCorrect -> CorrectGreen
        showResult && isSelected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = when {
        showResult && isCorrect -> CorrectGreen.copy(alpha = 0.1f)
        showResult && isSelected -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }

    OutlinedCard(
        onClick = { if (!showResult) onClick() },
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = if ((showResult && isCorrect) || isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected || (showResult && isCorrect)) FontWeight.SemiBold
                else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (showResult && isCorrect) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = CorrectGreen,
                    modifier = Modifier.size(20.dp)
                )
            } else if (showResult && isSelected) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Results ────────────────────────────────────────────────────────

@Composable
private fun QuizResultsContent(vm: QuizViewModel) {
    val accuracy = if (vm.questionCount > 0) (vm.score * 100) / vm.questionCount else 0

    // Centered score section
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Icon(
            imageVector = if (accuracy >= 70) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
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

    // Wrong answers review (start-aligned)
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

@Composable
private fun WrongAnswerCard(answer: com.lddev.scalefinder.ui.QuizAnswer) {
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
