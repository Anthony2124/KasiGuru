package com.kasiguru.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.LessonRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.data.repository.WordEncounterRepository
import com.kasiguru.domain.lesson.LessonRef
import com.kasiguru.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Where a word was last met, grouped the way the filter pills offer it. */
enum class MetIn(val label: String) { LESSON("Lessons"), REVIEW("Reviews"), GAME("Games") }

/** One word on the My words list: the entry, where and when it was last met, and whether it is due. */
data class MetWord(
    val word: VocabularyEntity,
    val lastSeenAt: Long,
    val timesSeen: Int,
    val source: String,
    val isDue: Boolean
) {
    val metIn: MetIn get() = when (source) {
        "lesson" -> MetIn.LESSON
        "review" -> MetIn.REVIEW
        else -> MetIn.GAME
    }

    /** Due for review, or lapsed at least once: the words worth practising first. */
    val needsPractice: Boolean get() = isDue || word.lapses > 0
}

data class MyWordsUiState(
    val isLoading: Boolean = true,
    val words: List<MetWord> = emptyList()
) {
    val dueCount: Int get() = words.count { it.isDue }
    val learnedCount: Int get() = words.count { it.word.isLearned }
}

/**
 * The Library's My words list: every dictionary word met in a lesson, a flashcard review or a game,
 * most recent first, so a learner can find again the word they saw yesterday and review it.
 */
@HiltViewModel
class MyWordsViewModel @Inject constructor(
    private val encounters: WordEncounterRepository,
    vocabularyRepository: VocabularyRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {

    private val loaded = MutableStateFlow(false)

    val uiState: StateFlow<MyWordsUiState> = combine(
        encounters.observeAll(),
        vocabularyRepository.getAllVocabulary(),
        loaded
    ) { met, vocabulary, isLoaded ->
        val byId = vocabulary.associateBy { it.id }
        val today = LocalDate.now().toString()
        MyWordsUiState(
            isLoading = !isLoaded && met.isEmpty(),
            words = met.mapNotNull { row ->
                val word = byId[row.wordId] ?: return@mapNotNull null
                MetWord(
                    word = word,
                    lastSeenAt = row.lastSeenAt,
                    timesSeen = row.timesSeen,
                    source = row.lastSource,
                    isDue = word.timesReviewed > 0 && word.nextReviewDate.isNotBlank() && word.nextReviewDate <= today
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyWordsUiState())

    init {
        // Words met before encounters were recorded: reviewed or learned words, and finished
        // lessons. Only adds what is missing, so running it on every open is safe and also brings
        // the list back after a sign-in restores progress.
        viewModelScope.launch {
            runCatching {
                val finished = lessonRepository.allProgressOnce()
                    .filter { it.isComplete }
                    .map { progress ->
                        val ids = lessonRepository.wordsFor(LessonRef(progress.unitId, progress.lessonIndex)).map { it.id }
                        progress.lastCompletedAt to ids
                    }
                encounters.fillFromHistory(finished)
            }
            loaded.value = true
        }
    }

    companion object {
        /** The name a source is shown with, matching the game names in Practice. */
        fun sourceName(source: String): String = when (source) {
            "lesson" -> "Lesson"
            "review" -> "Flashcards"
            "word_match" -> "Word Match"
            "reverse_match" -> "Reverse Match"
            "fill_blank" -> "Fill in the Blank"
            Constants.Games.RECALL -> "Word Recall"
            "aspect_builder" -> "Aspect Builder"
            "sentence_order" -> "Sentence Construction"
            Constants.Games.WORD_SEARCH -> "Word Search"
            Constants.Games.WORD_WHEEL -> "Word Wheel"
            else -> "Practice"
        }
    }
}
