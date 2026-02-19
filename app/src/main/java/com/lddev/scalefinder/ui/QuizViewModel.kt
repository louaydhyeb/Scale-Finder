package com.lddev.scalefinder.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.lddev.scalefinder.R
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.ScaleFormulas
import com.lddev.scalefinder.model.ScaleType
import com.lddev.scalefinder.model.Theory
import com.lddev.scalefinder.model.Tuning

enum class QuizCategory(val titleRes: Int, val descriptionRes: Int) {
    FRETBOARD_NOTE(R.string.quiz_fretboard_notes, R.string.quiz_fretboard_notes_desc),
    DIATONIC_CHORD(R.string.quiz_diatonic_chords, R.string.quiz_diatonic_chords_desc),
    SCALE_IDENTIFICATION(R.string.quiz_scale_id, R.string.quiz_scale_id_desc)
}

enum class QuizPhase { SETUP, SESSION, RESULTS }

data class QuizQuestion(
    val category: QuizCategory,
    val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
    val highlightString: Int? = null,
    val highlightFret: Int? = null,
    val scale: Scale? = null
)

class QuizViewModel : ViewModel() {

    var selectedCategory by mutableStateOf(QuizCategory.FRETBOARD_NOTE)
        private set
    var questionCount by mutableIntStateOf(10)
        private set
    var phase by mutableStateOf(QuizPhase.SETUP)
        private set
    var currentQuestionIndex by mutableIntStateOf(0)
        private set
    var score by mutableIntStateOf(0)
        private set
    var selectedAnswerIndex by mutableIntStateOf(-1)
        private set
    var hasAnswered by mutableStateOf(false)
        private set
    var currentQuestion by mutableStateOf<QuizQuestion?>(null)
        private set

    private val questions = mutableListOf<QuizQuestion>()

    fun selectCategory(category: QuizCategory) {
        selectedCategory = category
    }

    fun updateQuestionCount(count: Int) {
        questionCount = count.coerceIn(5, 20)
    }

    fun startQuiz() {
        questions.clear()
        repeat(questionCount) {
            questions.add(generateQuestion(selectedCategory))
        }
        currentQuestionIndex = 0
        score = 0
        selectedAnswerIndex = -1
        hasAnswered = false
        currentQuestion = questions.first()
        phase = QuizPhase.SESSION
    }

    fun selectAnswer(index: Int) {
        if (hasAnswered) return
        selectedAnswerIndex = index
        hasAnswered = true
        if (index == currentQuestion?.correctIndex) score++
    }

    fun nextQuestion() {
        val next = currentQuestionIndex + 1
        if (next >= questions.size) {
            phase = QuizPhase.RESULTS
        } else {
            currentQuestionIndex = next
            currentQuestion = questions[next]
            selectedAnswerIndex = -1
            hasAnswered = false
        }
    }

    fun restartQuiz() {
        startQuiz()
    }

    fun goToSetup() {
        phase = QuizPhase.SETUP
        currentQuestion = null
        selectedAnswerIndex = -1
        hasAnswered = false
    }

    private fun generateQuestion(category: QuizCategory): QuizQuestion = when (category) {
        QuizCategory.FRETBOARD_NOTE -> generateFretboardNoteQuestion()
        QuizCategory.DIATONIC_CHORD -> generateDiatonicChordQuestion()
        QuizCategory.SCALE_IDENTIFICATION -> generateScaleIdentificationQuestion()
    }

    private fun generateFretboardNoteQuestion(): QuizQuestion {
        val stringIdx = (0..5).random()
        val fret = (0..12).random()
        val correct = Note.fromSemitone(Tuning.STANDARD.openNotes[stringIdx].semitone + fret)
        val wrong = Note.entries.filter { it != correct }.shuffled().take(3)
        val choices = (listOf(correct) + wrong).shuffled()
        return QuizQuestion(
            category = QuizCategory.FRETBOARD_NOTE,
            prompt = "What note is highlighted on the fretboard?",
            choices = choices.map { it.label },
            correctIndex = choices.indexOf(correct),
            highlightString = stringIdx,
            highlightFret = fret
        )
    }

    private fun generateDiatonicChordQuestion(): QuizQuestion {
        val root = Note.entries.random()
        val scaleType = listOf(ScaleType.MAJOR, ScaleType.AEOLIAN, ScaleType.DORIAN).random()
        val scale = Scale(root, scaleType)
        val chords = ScaleFormulas.diatonicChords(scale)
        if (chords.isEmpty()) return generateDiatonicChordQuestion()

        val target = chords.random()
        val correct = target.chord
        val wrong = chords.map { it.chord }
            .filter { it != correct }
            .shuffled()
            .take(3)
            .let { list ->
                if (list.size < 3) {
                    list + Theory.allChords()
                        .filter { it != correct && it !in list }
                        .shuffled()
                        .take(3 - list.size)
                } else list
            }
        val choices = (listOf(correct) + wrong).shuffled()
        return QuizQuestion(
            category = QuizCategory.DIATONIC_CHORD,
            prompt = "In ${root.label} ${scaleType.display}, what is the ${target.degree} chord?",
            choices = choices.map { it.toString() },
            correctIndex = choices.indexOf(correct)
        )
    }

    private fun generateScaleIdentificationQuestion(): QuizQuestion {
        val root = Note.entries.random()
        val scaleType = ScaleType.entries.random()
        val scale = Scale(root, scaleType)
        val wrongTypes = ScaleType.entries.filter { it != scaleType }.shuffled().take(3)
        val choices = (listOf(scale) + wrongTypes.map { Scale(root, it) }).shuffled()
        return QuizQuestion(
            category = QuizCategory.SCALE_IDENTIFICATION,
            prompt = "What scale is shown on the fretboard?",
            choices = choices.map { it.toString() },
            correctIndex = choices.indexOf(scale),
            scale = scale
        )
    }
}
