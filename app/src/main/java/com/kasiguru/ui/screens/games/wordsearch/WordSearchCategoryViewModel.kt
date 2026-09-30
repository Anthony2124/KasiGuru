package com.kasiguru.ui.screens.games.wordsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.wordsearch.WordSearchGenerator
import com.kasiguru.domain.wordsearch.WordSearchTier
import com.kasiguru.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One row of the category picker. */
data class WordSearchCategoryRow(
    val category: String,
    val levelKey: String,
    val levelsCleared: Int,
    val starsEarned: Int,
    /** Enough short words to fill an Easy grid. A category below it is shown, but not playable yet. */
    val isPlayable: Boolean
)

data class WordSearchCategoryUiState(
    val isLoading: Boolean = true,
    val rows: List<WordSearchCategoryRow> = emptyList()
)

/**
 * Backs the screen that asks which category to play before a Word Search starts. Each category is its
 * own 30-level track, so this screen is where the learner sees how far along each track they are.
 */
@HiltViewModel
class WordSearchCategoryViewModel @Inject constructor(
    vocabularyRepository: VocabularyRepository,
    gameLevelRepository: GameLevelRepository
) : ViewModel() {

    val uiState: StateFlow<WordSearchCategoryUiState> = combine(
        vocabularyRepository.getAllVocabulary(),
        gameLevelRepository.getAllLevels()
    ) { words, levels ->
        val easy = WordSearchTier.forLevel(1)
        val wordsByCategory = words.groupBy { it.category }
        val levelsByKey = levels.groupBy { it.gameType }
        WordSearchCategoryUiState(
            isLoading = false,
            rows = Constants.VocabCategories.ALL.map { category ->
                val key = Constants.Games.wordSearchLevelKey(category)
                val track = levelsByKey[key].orEmpty()
                val fitting = wordsByCategory[category].orEmpty()
                    .mapNotNull { WordSearchGenerator.letters(it.kasiguranin) }
                    .filter { it.size in 3..easy.gridSize }
                    .distinctBy { it.joinToString("") }
                WordSearchCategoryRow(
                    category = category,
                    levelKey = key,
                    levelsCleared = track.count { it.starsEarned > 0 },
                    starsEarned = track.sumOf { it.starsEarned },
                    isPlayable = fitting.size >= easy.wordCount
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordSearchCategoryUiState())
}
