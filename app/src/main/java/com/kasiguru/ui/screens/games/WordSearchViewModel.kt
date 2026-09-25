package com.kasiguru.ui.screens.games

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.wordsearch.GridCell
import com.kasiguru.domain.wordsearch.WordSearchCandidate
import com.kasiguru.domain.wordsearch.WordSearchGenerator
import com.kasiguru.domain.wordsearch.WordSearchPuzzle
import com.kasiguru.domain.wordsearch.WordSearchTier
import com.kasiguru.util.Constants
import com.kasiguru.util.toIsoString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
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

data class WordSearchUiState(
    val isLoading: Boolean = true,
    val isUnavailable: Boolean = false,
    val category: String = "",
    val level: Int = 1,
    val tier: WordSearchTier = WordSearchTier.forLevel(1),
    val puzzle: WordSearchPuzzle? = null,
    /** Vocabulary rows by id, so the word list can show meanings and play audio. */
    val entries: Map<Int, VocabularyEntity> = emptyMap(),
    /** In the order they were found, which also fixes each word's highlight colour. */
    val foundIds: List<Int> = emptyList(),
    val selectionStart: GridCell? = null,
    val lastTapMissed: Boolean = false,
    val misses: Int = 0,
    val isGameOver: Boolean = false,
    val starsEarned: Int = 0,
    val finalXp: Int = 0,
    val nextLevel: Int? = null
)

@HiltViewModel
class WordSearchViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository,
    private val gameLevelRepository: GameLevelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val levelKey: String = savedStateHandle.get<String>("category").orEmpty()
    private val levelNumber: Int = (savedStateHandle.get<Int>("level") ?: 1).coerceIn(1, WordSearchTier.MAX_LEVEL)

    private val _uiState = MutableStateFlow(WordSearchUiState(level = levelNumber))
    val uiState: StateFlow<WordSearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val category = Constants.Games.categoryForWordSearchKey(levelKey)
        if (category == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, isUnavailable = true)
            return
        }
        val words = vocabularyRepository.getVocabularyByCategory(category).first()
        // Seeded by category and level, never by time: replaying a level to earn more stars means
        // replaying the same grid. String.hashCode is specified by the JVM, so it is stable.
        val seed = levelKey.hashCode().toLong() * 31 + levelNumber
        val puzzle = WordSearchGenerator.generate(
            candidates = words.map { WordSearchCandidate(it.id, it.kasiguranin) },
            level = levelNumber,
            seed = seed
        )
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isUnavailable = puzzle == null,
            category = category,
            tier = WordSearchTier.forLevel(levelNumber),
            puzzle = puzzle,
            entries = words.associateBy { it.id }
        )
    }

    /**
     * A finger dragged from [from] to [to]; the grid has already snapped the line straight. Either
     * end may be the word's first letter. Lifting on the starting cell is a change of mind, not a miss.
     */
    fun onLineSelected(from: GridCell, to: GridCell) {
        val state = _uiState.value
        if (state.puzzle == null || state.isGameOver || from == to) return
        judgeLine(state, from, to)
    }

    /**
     * The TalkBack path: a drag cannot be performed by swipe navigation, so each cell also answers a
     * double-tap, first letter then last, in either order.
     */
    fun onCellTapped(cell: GridCell) {
        val state = _uiState.value
        val puzzle = state.puzzle ?: return
        if (state.isGameOver) return

        val start = state.selectionStart
        when {
            start == null -> _uiState.value = state.copy(selectionStart = cell, lastTapMissed = false)
            start == cell -> _uiState.value = state.copy(selectionStart = null)
            // Not in line with the first tap: treat it as a fresh first tap, not a wrong answer.
            puzzle.lineBetween(start, cell) == null ->
                _uiState.value = state.copy(selectionStart = cell, lastTapMissed = false)
            else -> judgeLine(state, start, cell)
        }
    }

    private fun judgeLine(state: WordSearchUiState, from: GridCell, to: GridCell) {
        val puzzle = state.puzzle ?: return
        val hit = puzzle.match(from, to, state.foundIds.toSet())
        if (hit == null) {
            _uiState.value = state.copy(selectionStart = null, lastTapMissed = true, misses = state.misses + 1)
        } else {
            val found = state.foundIds + hit.id
            _uiState.value = state.copy(selectionStart = null, lastTapMissed = false, foundIds = found)
            if (found.size == puzzle.words.size) finish()
        }
    }

    private fun finish() {
        val state = _uiState.value
        val wordCount = state.puzzle?.words?.size ?: return
        val stars = when {
            state.misses == 0 -> 3
            state.misses <= 2 -> 2
            else -> 1
        }
        // Half the per-answer XP of the choice games: spotting a word's letters is recognition of
        // spelling, a lighter task than recalling what a word means.
        val xp = wordCount * (Constants.XP_PER_GAME_CORRECT / 2)

        viewModelScope.launch {
            gameRepository.saveScore(
                GameScoreEntity(
                    gameType = Constants.Games.WORD_SEARCH,
                    score = wordCount,
                    totalQuestions = wordCount,
                    xpEarned = xp,
                    playedAt = LocalDateTime.now().toIsoString()
                )
            )
            gameLevelRepository.saveLevelResult(levelKey, levelNumber, stars)
            userProgressRepository.addXp(xp)
            userProgressRepository.incrementGamesPlayed()
            // Wrong lines count against accuracy: every attempt is a question, only the hits correct.
            userProgressRepository.updateGameStats(wordCount, wordCount + state.misses)
            if (state.misses == 0) userProgressRepository.checkPerfectGameAchievement()

            // Deliberately no SM-2 review for the words found. Finding a run of letters is not
            // evidence of remembering a meaning, and the review schedule is a thesis claim that must
            // stay exact; see PRODUCT.md.
            _uiState.value = _uiState.value.copy(
                isGameOver = true,
                starsEarned = stars,
                finalXp = xp,
                nextLevel = (levelNumber + 1).takeIf { it <= WordSearchTier.MAX_LEVEL }
            )
        }
    }
}
