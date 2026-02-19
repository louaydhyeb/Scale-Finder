package com.lddev.scalefinder.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lddev.scalefinder.R
import com.lddev.scalefinder.audio.ChordPlayer
import com.lddev.scalefinder.audio.NotePlayer
import com.lddev.scalefinder.model.Chord
import com.lddev.scalefinder.model.ChordQuality
import com.lddev.scalefinder.model.ChordVoicing
import com.lddev.scalefinder.model.ChordVoicings
import com.lddev.scalefinder.model.Note
import com.lddev.scalefinder.model.Scale
import com.lddev.scalefinder.model.ScaleFormulas
import com.lddev.scalefinder.model.ScaleType
import com.lddev.scalefinder.model.Theory
import com.lddev.scalefinder.model.Tuning
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import androidx.core.content.edit

enum class QuizCategory(val titleRes: Int, val descriptionRes: Int) {
    FRETBOARD_NOTE(R.string.quiz_fretboard_notes, R.string.quiz_fretboard_notes_desc),
    DIATONIC_CHORD(R.string.quiz_diatonic_chords, R.string.quiz_diatonic_chords_desc),
    SCALE_IDENTIFICATION(R.string.quiz_scale_id, R.string.quiz_scale_id_desc),
    EAR_NOTE(R.string.quiz_ear_note, R.string.quiz_ear_note_desc),
    EAR_CHORD(R.string.quiz_ear_chord, R.string.quiz_ear_chord_desc),
    CHORD_VOICING(R.string.quiz_chord_voicing, R.string.quiz_chord_voicing_desc)
}

enum class QuizDifficulty(val titleRes: Int, val choiceCount: Int) {
    EASY(R.string.quiz_easy, 3),
    MEDIUM(R.string.quiz_medium, 4),
    HARD(R.string.quiz_hard, 5)
}

enum class QuizPhase { SETUP, SESSION, RESULTS }

data class QuizQuestion(
    val category: QuizCategory,
    val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
    val highlightString: Int? = null,
    val highlightFret: Int? = null,
    val scale: Scale? = null,
    val chordVoicing: ChordVoicing? = null,
    val earChord: Chord? = null,
    val earNoteFrequency: Double? = null
)

data class QuizAnswer(
    val question: QuizQuestion,
    val selectedIndex: Int,
    val wasCorrect: Boolean
)

data class CategoryStats(
    val quizzesPlayed: Int = 0,
    val averageAccuracy: Int = 0,
    val bestAccuracy: Int = 0
)

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TIMER_SECONDS = 15
        private const val PREFS_NAME = "quiz_stats"
        private val NATURAL_NOTES = listOf(
            Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B
        )
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var notePlayer: NotePlayer? = null
    private var chordPlayer: ChordPlayer? = null
    private var timerJob: Job? = null
    private val answers = mutableListOf<QuizAnswer>()
    private val questions = mutableListOf<QuizQuestion>()
    private var resultSaved = false

    // ── Setup state ────────────────────────────────────────────────

    var selectedCategory by mutableStateOf(QuizCategory.FRETBOARD_NOTE)
        private set
    var selectedDifficulty by mutableStateOf(QuizDifficulty.MEDIUM)
        private set
    var questionCount by mutableIntStateOf(10)
        private set
    var timedMode by mutableStateOf(false)
        private set

    // ── Session state ──────────────────────────────────────────────

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
    var timeRemaining by mutableIntStateOf(0)
        private set
    var isTimedOut by mutableStateOf(false)
        private set

    // ── Stats ──────────────────────────────────────────────────────

    var categoryStats by mutableStateOf(CategoryStats())
        private set
    var isNewBest by mutableStateOf(false)
        private set

    val wrongAnswers: List<QuizAnswer> get() = answers.filter { !it.wasCorrect }

    val totalQuizzesPlayed: Int
        get() = QuizCategory.entries.sumOf { prefs.getInt("${it.name}_count", 0) }

    init {
        loadCategoryStats()
    }

    // ── Setup actions ──────────────────────────────────────────────

    fun selectCategory(category: QuizCategory) {
        selectedCategory = category
        loadCategoryStats()
    }

    fun selectDifficulty(difficulty: QuizDifficulty) { selectedDifficulty = difficulty }
    fun updateQuestionCount(count: Int) { questionCount = count.coerceIn(5, 20) }
    fun toggleTimedMode() { timedMode = !timedMode }

    // ── Session actions ────────────────────────────────────────────

    fun startQuiz() {
        questions.clear()
        answers.clear()
        resultSaved = false
        isNewBest = false
        repeat(questionCount) { questions.add(generateQuestion(selectedCategory)) }
        currentQuestionIndex = 0
        score = 0
        selectedAnswerIndex = -1
        hasAnswered = false
        isTimedOut = false
        currentQuestion = questions.first()
        phase = QuizPhase.SESSION
        startTimer()
    }

    fun selectAnswer(index: Int) {
        if (hasAnswered) return
        timerJob?.cancel()
        selectedAnswerIndex = index
        hasAnswered = true
        val correct = index == currentQuestion?.correctIndex
        if (correct) score++
        currentQuestion?.let { answers.add(QuizAnswer(it, index, correct)) }
    }

    fun nextQuestion() {
        val next = currentQuestionIndex + 1
        if (next >= questions.size) {
            timerJob?.cancel()
            phase = QuizPhase.RESULTS
            if (!resultSaved) {
                saveQuizResult()
                resultSaved = true
            }
        } else {
            currentQuestionIndex = next
            currentQuestion = questions[next]
            selectedAnswerIndex = -1
            hasAnswered = false
            isTimedOut = false
            startTimer()
        }
    }

    fun restartQuiz() { startQuiz() }

    fun goToSetup() {
        timerJob?.cancel()
        phase = QuizPhase.SETUP
        currentQuestion = null
        selectedAnswerIndex = -1
        hasAnswered = false
        isTimedOut = false
        isNewBest = false
        loadCategoryStats()
    }

    fun playCurrentQuestionAudio() {
        val question = currentQuestion ?: return
        when (question.category) {
            QuizCategory.EAR_NOTE -> {
                val freq = question.earNoteFrequency ?: return
                val player = notePlayer ?: NotePlayer().also { notePlayer = it }
                player.playGuitarNote(freq, durationMs = 1000)
            }
            QuizCategory.EAR_CHORD -> {
                val chord = question.earChord ?: return
                val player = chordPlayer ?: ChordPlayer().also { chordPlayer = it }
                player.playChord(chord, durationMs = 1500)
            }
            else -> {}
        }
    }

    // ── Stats persistence ──────────────────────────────────────────

    private fun saveQuizResult() {
        val key = selectedCategory.name
        val accuracy = if (questionCount > 0) (score * 100) / questionCount else 0
        val prevBest = prefs.getInt("${key}_best", 0)
        isNewBest = accuracy > prevBest

        val newCount = prefs.getInt("${key}_count", 0) + 1
        val newTotalScore = prefs.getInt("${key}_total_score", 0) + score
        val newTotalQuestions = prefs.getInt("${key}_total_questions", 0) + questionCount

        prefs.edit {
            putInt("${key}_count", newCount)
                .putInt("${key}_total_score", newTotalScore)
                .putInt("${key}_total_questions", newTotalQuestions)
                .putInt("${key}_best", maxOf(prevBest, accuracy))
        }

        loadCategoryStats()
    }

    private fun loadCategoryStats() {
        val key = selectedCategory.name
        val count = prefs.getInt("${key}_count", 0)
        val totalScore = prefs.getInt("${key}_total_score", 0)
        val totalQuestions = prefs.getInt("${key}_total_questions", 0)
        val best = prefs.getInt("${key}_best", 0)
        val avg = if (totalQuestions > 0) (totalScore * 100) / totalQuestions else 0

        categoryStats = CategoryStats(
            quizzesPlayed = count,
            averageAccuracy = avg,
            bestAccuracy = best
        )
    }

    // ── Timer ──────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob?.cancel()
        if (!timedMode) return
        timeRemaining = TIMER_SECONDS
        timerJob = viewModelScope.launch {
            while (timeRemaining > 0) {
                delay(1000)
                if (hasAnswered) return@launch
                timeRemaining--
            }
            if (!hasAnswered) {
                isTimedOut = true
                hasAnswered = true
                selectedAnswerIndex = -1
                currentQuestion?.let { answers.add(QuizAnswer(it, -1, false)) }
            }
        }
    }

    // ── Question generation ────────────────────────────────────────

    private fun generateQuestion(category: QuizCategory): QuizQuestion = when (category) {
        QuizCategory.FRETBOARD_NOTE -> generateFretboardNoteQuestion()
        QuizCategory.DIATONIC_CHORD -> generateDiatonicChordQuestion()
        QuizCategory.SCALE_IDENTIFICATION -> generateScaleIdentificationQuestion()
        QuizCategory.EAR_NOTE -> generateEarNoteQuestion()
        QuizCategory.EAR_CHORD -> generateEarChordQuestion()
        QuizCategory.CHORD_VOICING -> generateChordVoicingQuestion()
    }

    private fun notesForDifficulty(): List<Note> = when (selectedDifficulty) {
        QuizDifficulty.EASY -> NATURAL_NOTES
        else -> Note.entries
    }

    private fun wrongCount(): Int = selectedDifficulty.choiceCount - 1

    private fun midiToFreq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    private fun generateFretboardNoteQuestion(): QuizQuestion {
        val notes = notesForDifficulty()
        val maxFret = when (selectedDifficulty) {
            QuizDifficulty.EASY -> 7
            QuizDifficulty.MEDIUM -> 12
            QuizDifficulty.HARD -> 15
        }
        var stringIdx: Int
        var fret: Int
        var correct: Note
        do {
            stringIdx = (0..5).random()
            fret = (0..maxFret).random()
            correct = Note.fromSemitone(Tuning.STANDARD.openNotes[stringIdx].semitone + fret)
        } while (correct !in notes)

        val wrong = notes.filter { it != correct }.shuffled().take(wrongCount())
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
        val root = notesForDifficulty().random()
        val scaleTypes = when (selectedDifficulty) {
            QuizDifficulty.EASY -> listOf(ScaleType.MAJOR)
            QuizDifficulty.MEDIUM -> listOf(ScaleType.MAJOR, ScaleType.AEOLIAN, ScaleType.DORIAN)
            QuizDifficulty.HARD -> ScaleType.entries.filter { it.intervals.size >= 7 }
        }
        val scaleType = scaleTypes.random()
        val scale = Scale(root, scaleType)
        val chords = ScaleFormulas.diatonicChords(scale)
        if (chords.isEmpty()) return generateDiatonicChordQuestion()

        val target = chords.random()
        val correct = target.chord
        val wrong = chords.map { it.chord }
            .filter { it != correct }
            .shuffled()
            .take(wrongCount())
            .let { list ->
                if (list.size < wrongCount()) {
                    list + Theory.allChords()
                        .filter { it != correct && it !in list }
                        .shuffled()
                        .take(wrongCount() - list.size)
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
        val root = notesForDifficulty().random()
        val scaleTypes = when (selectedDifficulty) {
            QuizDifficulty.EASY -> listOf(
                ScaleType.MAJOR, ScaleType.AEOLIAN,
                ScaleType.MAJOR_PENTATONIC, ScaleType.MINOR_PENTATONIC
            )
            else -> ScaleType.entries.toList()
        }
        val scaleType = scaleTypes.random()
        val scale = Scale(root, scaleType)
        val wrongTypes = scaleTypes.filter { it != scaleType }.shuffled().take(wrongCount())
        val choices = (listOf(scale) + wrongTypes.map { Scale(root, it) }).shuffled()
        return QuizQuestion(
            category = QuizCategory.SCALE_IDENTIFICATION,
            prompt = "What scale is shown on the fretboard?",
            choices = choices.map { it.toString() },
            correctIndex = choices.indexOf(scale),
            scale = scale
        )
    }

    private fun generateEarNoteQuestion(): QuizQuestion {
        val notes = notesForDifficulty()
        val correct = notes.random()
        val midi = 60 + correct.semitone
        val wrong = notes.filter { it != correct }.shuffled().take(wrongCount())
        val choices = (listOf(correct) + wrong).shuffled()
        return QuizQuestion(
            category = QuizCategory.EAR_NOTE,
            prompt = "What note do you hear?",
            choices = choices.map { it.label },
            correctIndex = choices.indexOf(correct),
            earNoteFrequency = midiToFreq(midi)
        )
    }

    private fun generateEarChordQuestion(): QuizQuestion {
        val root = notesForDifficulty().random()
        val qualities = when (selectedDifficulty) {
            QuizDifficulty.EASY -> listOf(ChordQuality.MAJOR, ChordQuality.MINOR)
            QuizDifficulty.MEDIUM -> listOf(
                ChordQuality.MAJOR, ChordQuality.MINOR,
                ChordQuality.DOMINANT7, ChordQuality.MAJOR7, ChordQuality.MINOR7
            )
            QuizDifficulty.HARD -> ChordQuality.entries.toList()
        }
        val quality = qualities.random()
        val correct = Chord(root, quality)
        val wrong = qualities.filter { it != quality }.shuffled().take(wrongCount())
            .map { Chord(root, it) }
        val choices = (listOf(correct) + wrong).shuffled()
        return QuizQuestion(
            category = QuizCategory.EAR_CHORD,
            prompt = "What chord do you hear?",
            choices = choices.map { it.toString() },
            correctIndex = choices.indexOf(correct),
            earChord = correct
        )
    }

    private fun generateChordVoicingQuestion(): QuizQuestion {
        val qualities = when (selectedDifficulty) {
            QuizDifficulty.EASY -> listOf(ChordQuality.MAJOR, ChordQuality.MINOR)
            QuizDifficulty.MEDIUM -> listOf(
                ChordQuality.MAJOR, ChordQuality.MINOR,
                ChordQuality.DOMINANT7, ChordQuality.MAJOR7, ChordQuality.MINOR7
            )
            QuizDifficulty.HARD -> ChordQuality.entries.toList()
        }
        val root = notesForDifficulty().random()
        val quality = qualities.random()
        val chord = Chord(root, quality)
        val voicings = ChordVoicings.getVoicings(chord)
        if (voicings.isEmpty()) return generateChordVoicingQuestion()
        val voicing = voicings.random()

        val wrong = Theory.allChords()
            .filter { it != chord && it.quality in qualities }
            .shuffled()
            .take(wrongCount())
        val choices = (listOf(chord) + wrong).shuffled()
        return QuizQuestion(
            category = QuizCategory.CHORD_VOICING,
            prompt = "What chord is shown in this voicing?",
            choices = choices.map { it.toString() },
            correctIndex = choices.indexOf(chord),
            chordVoicing = voicing
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        notePlayer?.dispose()
        chordPlayer?.stop()
    }
}
