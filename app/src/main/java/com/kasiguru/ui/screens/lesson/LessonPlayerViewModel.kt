package com.kasiguru.ui.screens.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.LessonRepository
import com.kasiguru.domain.lesson.Exercise
import com.kasiguru.domain.lesson.LessonRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class LessonUiState(
    val isLoading: Boolean = true,
    val queue: List<Exercise> = emptyList(),
    val position: Int = 0,
    /** Total distinct exercises in the lesson, which is what the progress bar measures against. */
    val totalExercises: Int = 0,
    val solvedCount: Int = 0,
    val selectedOption: String? = null,
    /** Null until the learner commits an answer; then true or false. */
    val isCorrect: Boolean? = null,
    val isComplete: Boolean = false,
    val xpAwarded: Int = 0,
    val accuracy: Float = 0f,
    val wordsCovered: List<VocabularyEntity> = emptyList()
) {
    val current: Exercise? get() = queue.getOrNull(position)
    val hasAnswered: Boolean get() = isCorrect != null
    val progressFraction: Float
        get() = if (totalExercises == 0) 0f else solvedCount.toFloat() / totalExercises
}

/**
 * Drives one lesson run.
 *
 * A wrong answer pushes the exercise back onto the end of the queue rather than moving on, so a lesson
 * is only finished when every item has been answered correctly at least once. Accuracy is measured on
 * *first* attempts, which is what separates "got through it" from "knew it" — and only a clean run
 * earns the perfect bonus.
 */
@HiltViewModel
class LessonPlayerViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lessonRef: LessonRef = LessonRef(
        unitId = URLDecoder.decode(
            savedStateHandle.get<String>("unitId").orEmpty(),
            StandardCharsets.UTF_8.name()
        ),
        lessonIndex = savedStateHandle.get<Int>("lessonIndex") ?: 0
    )

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    /** Exercise identities already answered wrong once, so a retry cannot restore the perfect bonus. */
    private val missedFirstAttempt = mutableSetOf<Int>()

    /** Exercises already solved, tracked by queue identity rather than position. */
    private val solved = mutableSetOf<Int>()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val exercises = lessonRepository.exercisesFor(lessonRef)
            val words = lessonRepository.wordsFor(lessonRef)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    queue = exercises,
                    totalExercises = exercises.size,
                    wordsCovered = words,
                    // An empty lesson is a real state: a category can be too small to fill one.
                    isComplete = exercises.isEmpty()
                )
            }
        }
    }

    fun selectOption(option: String) {
        if (_uiState.value.hasAnswered) return
        _uiState.update { it.copy(selectedOption = option) }
    }

    /** Commits the selected answer and reveals feedback. */
    fun check() {
        val state = _uiState.value
        val exercise = state.current ?: return
        val selected = state.selectedOption ?: return
        if (state.hasAnswered) return

        val correct = selected == exercise.answer
        val identity = exerciseIdentity(state.position)
        if (!correct) missedFirstAttempt += identity

        _uiState.update { it.copy(isCorrect = correct) }
    }

    /** Advances past the feedback: forward on a correct answer, requeue on a wrong one. */
    fun advance() {
        val state = _uiState.value
        val exercise = state.current ?: return
        val wasCorrect = state.isCorrect ?: return

        val identity = exerciseIdentity(state.position)
        val newQueue = state.queue.toMutableList()

        if (wasCorrect) {
            solved += identity
        } else {
            // Back of the queue, so the learner meets it again before the lesson can end.
            newQueue.add(exercise)
        }

        val nextPosition = state.position + 1
        val finished = nextPosition >= newQueue.size

        if (finished) {
            complete()
        } else {
            _uiState.update {
                it.copy(
                    queue = newQueue,
                    position = nextPosition,
                    selectedOption = null,
                    isCorrect = null,
                    solvedCount = solved.size
                )
            }
        }
    }

    private fun complete() {
        val state = _uiState.value
        val total = state.totalExercises.coerceAtLeast(1)
        val accuracy = ((total - missedFirstAttempt.size).toFloat() / total).coerceIn(0f, 1f)

        viewModelScope.launch {
            val xp = lessonRepository.completeLesson(lessonRef, accuracy)
            _uiState.update {
                it.copy(
                    isComplete = true,
                    xpAwarded = xp,
                    accuracy = accuracy,
                    solvedCount = it.totalExercises,
                    selectedOption = null,
                    isCorrect = null
                )
            }
        }
    }

    /**
     * Identity of the exercise at [position] within the original run.
     *
     * Positions past the original length are requeued repeats, so they map back to the exercise they
     * came from — otherwise a retry would count as a new item and inflate the total.
     */
    private fun exerciseIdentity(position: Int): Int {
        val state = _uiState.value
        val exercise = state.queue.getOrNull(position) ?: return position
        return state.queue.take(state.totalExercises).indexOfFirst { it === exercise }
            .takeIf { it >= 0 } ?: position
    }
}
