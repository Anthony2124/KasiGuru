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
import com.kasiguru.domain.wordwheel.BoardCell
import com.kasiguru.domain.wordwheel.WheelWord
import com.kasiguru.domain.wordwheel.WordWheelCandidate
import com.kasiguru.domain.wordwheel.WordWheelGenerator
import com.kasiguru.domain.wordwheel.WordWheelPuzzle
import com.kasiguru.domain.wordwheel.WordWheelTier
import com.kasiguru.util.Constants
import com.kasiguru.util.toIsoString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/** What the last submitted word turned out to be, for the line under the board. */
sealed interface WheelFeedback {
    data class Found(val word: String, val gloss: String) : WheelFeedback
    data class Bonus(val word: String, val gloss: String) : WheelFeedback
    data class AlreadyFound(val word: String) : WheelFeedback
    data class NotAWord(val attempt: String) : WheelFeedback
    data object TooShort : WheelFeedback
    data class Revealed(val letter: String) : WheelFeedback
}

data class WordWheelUiState(
    val isLoading: Boolean = true,
    val isUnavailable: Boolean = false,
    val level: Int = 1,
    val tier: WordWheelTier = WordWheelTier.forLevel(1),
    val puzzle: WordWheelPuzzle? = null,
    /** Wheel indices in the order they are drawn around the wheel; Shuffle reorders this. */
    val wheelOrder: List<Int> = emptyList(),
    /** Wheel indices picked so far for the word being spelled, in order. */
    val selection: List<Int> = emptyList(),
    /** Board slots found, in the order found. */
    val foundSlots: List<Int> = emptyList(),
    /** Board cells uncovered by a hint. */
    val revealed: Set<BoardCell> = emptySet(),
    val bonusFound: List<WheelWord> = emptyList(),
    val hintsUsed: Int = 0,
    val feedback: WheelFeedback? = null,
    val entries: Map<Int, VocabularyEntity> = emptyMap(),
    val isGameOver: Boolean = false,
    val starsEarned: Int = 0,
    val finalXp: Int = 0,
    val nextLevel: Int? = null
) {
    val attempt: String
        get() = puzzle?.let { p -> selection.joinToString("") { p.wheel[it] } }.orEmpty()

    /** Board cells whose letter is showing: every cell of a found word, plus hinted cells. */
    val shownCells: Set<BoardCell>
        get() {
            val p = puzzle ?: return emptySet()
            return foundSlots.flatMap { p.slots[it].cells }.toSet() + revealed
        }
}

@HiltViewModel
class WordWheelViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository,
    private val gameLevelRepository: GameLevelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val levelNumber: Int =
        (savedStateHandle.get<Int>("level") ?: 1).coerceIn(1, WordWheelTier.MAX_LEVEL)

    private val _uiState = MutableStateFlow(WordWheelUiState(level = levelNumber))
    val uiState: StateFlow<WordWheelUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val words = vocabularyRepository.getAllVocabularyOnce()
            val puzzle = WordWheelGenerator.buildLevel(
                words.map { WordWheelCandidate(it.id, it.kasiguranin) },
                levelNumber
            )
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isUnavailable = puzzle == null,
                tier = WordWheelTier.forLevel(levelNumber),
                puzzle = puzzle,
                wheelOrder = puzzle?.wheel?.indices?.toList().orEmpty(),
                entries = words.associateBy { it.id }
            )
        }
    }

    /**
     * A tap on a wheel letter. Adds it to the word; tapping the last letter again takes it back off,
     * so a mistyped letter can be undone without clearing the whole word.
     */
    fun onLetterTapped(index: Int) {
        val state = _uiState.value
        if (state.isGameOver) return
        val selection = state.selection
        _uiState.value = when {
            selection.lastOrNull() == index -> state.copy(selection = selection.dropLast(1))
            index in selection -> state
            else -> state.copy(selection = selection + index, feedback = null)
        }
    }

    /**
     * A drag passing over a wheel letter. Moving back onto the letter before the last one retracts
     * the last, the way a swipe keyboard lets a finger back out of a wrong turn.
     */
    fun onLetterDraggedOver(index: Int) {
        val state = _uiState.value
        if (state.isGameOver) return
        val selection = state.selection
        _uiState.value = when {
            selection.size >= 2 && selection[selection.size - 2] == index ->
                state.copy(selection = selection.dropLast(1))
            index in selection -> state
            else -> state.copy(selection = selection + index, feedback = null)
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selection = emptyList())
    }

    fun shuffle() {
        val state = _uiState.value
        if (state.wheelOrder.size < 2) return
        var order = state.wheelOrder.shuffled()
        while (order == state.wheelOrder) order = state.wheelOrder.shuffled()
        _uiState.value = state.copy(wheelOrder = order, selection = emptyList())
    }

    fun submit() {
        val state = _uiState.value
        val puzzle = state.puzzle ?: return
        if (state.isGameOver || state.selection.isEmpty()) return
        val letters = state.selection.map { puzzle.wheel[it] }
        val cleared = state.copy(selection = emptyList())

        if (letters.size < WordWheelGenerator.MIN_WORD_LENGTH) {
            _uiState.value = cleared.copy(feedback = WheelFeedback.TooShort)
            return
        }
        val slot = puzzle.slotFor(letters)
        val bonus = puzzle.bonusFor(letters)
        _uiState.value = when {
            slot != null && slot in state.foundSlots ->
                cleared.copy(feedback = WheelFeedback.AlreadyFound(puzzle.slots[slot].word.word))
            slot != null -> {
                val word = puzzle.slots[slot].word
                completeFilledSlots(
                    cleared.copy(
                        foundSlots = state.foundSlots + slot,
                        feedback = WheelFeedback.Found(word.word, glossFor(word))
                    )
                )
            }
            bonus != null && state.bonusFound.any { it.key == bonus.key } ->
                cleared.copy(feedback = WheelFeedback.AlreadyFound(bonus.word))
            bonus != null ->
                cleared.copy(
                    bonusFound = state.bonusFound + bonus,
                    feedback = WheelFeedback.Bonus(bonus.word, glossFor(bonus))
                )
            else -> cleared.copy(feedback = WheelFeedback.NotAWord(letters.joinToString("")))
        }
        finishIfSolved()
    }

    /**
     * Uncovers one letter: the first hidden cell of the first unfinished word. Free to use, but each
     * hint lowers the stars the level can earn, so the choice stays the learner's.
     */
    fun hint() {
        val state = _uiState.value
        val puzzle = state.puzzle ?: return
        if (state.isGameOver) return
        val shown = state.shownCells
        val cell = puzzle.slots.indices
            .filter { it !in state.foundSlots }
            .sortedBy { puzzle.slots[it].word.letters.size }
            .firstNotNullOfOrNull { i -> puzzle.slots[i].cells.firstOrNull { it !in shown } }
            ?: return
        _uiState.value = completeFilledSlots(
            state.copy(
                revealed = state.revealed + cell,
                hintsUsed = state.hintsUsed + 1,
                selection = emptyList(),
                feedback = WheelFeedback.Revealed(puzzle.letterAt.getValue(cell))
            )
        )
        finishIfSolved()
    }

    /** A word whose every letter is showing, through crossings and hints, counts as found. */
    private fun completeFilledSlots(state: WordWheelUiState): WordWheelUiState {
        val puzzle = state.puzzle ?: return state
        val shown = state.shownCells
        val completed = puzzle.slots.indices.filter { it !in state.foundSlots && puzzle.slots[it].cells.all(shown::contains) }
        return if (completed.isEmpty()) state else state.copy(foundSlots = state.foundSlots + completed)
    }

    private fun glossFor(word: WheelWord): String = _uiState.value.entries[word.id]?.let(::tagalogGloss).orEmpty()

    private fun finishIfSolved() {
        val state = _uiState.value
        val puzzle = state.puzzle ?: return
        if (state.isGameOver || state.foundSlots.size < puzzle.slots.size) return

        val boardWords = puzzle.slots.size
        val stars = when {
            state.hintsUsed == 0 -> 3
            state.hintsUsed <= 2 -> 2
            else -> 1
        }
        // Half the per-answer XP of the choice games per board word, and half again per bonus word:
        // spelling a word from its letters is a recall of form, not of meaning.
        val xp = boardWords * (Constants.XP_PER_GAME_CORRECT / 2) +
            state.bonusFound.size * (Constants.XP_PER_GAME_CORRECT / 4)
        _uiState.value = state.copy(isGameOver = true)

        viewModelScope.launch {
            gameRepository.saveScore(
                GameScoreEntity(
                    gameType = Constants.Games.WORD_WHEEL,
                    score = boardWords,
                    totalQuestions = boardWords,
                    xpEarned = xp,
                    playedAt = LocalDateTime.now().toIsoString()
                )
            )
            gameLevelRepository.saveLevelResult(Constants.Games.WORD_WHEEL, levelNumber, stars)
            userProgressRepository.addXp(xp)
            userProgressRepository.incrementGamesPlayed()
            // Wrong guesses are how this game is played, so they do not count against accuracy.
            // A hint does: it is a word the learner could not produce.
            userProgressRepository.updateGameStats(boardWords, boardWords + state.hintsUsed)
            if (state.hintsUsed == 0) userProgressRepository.checkPerfectGameAchievement()

            // No SM-2 review for these words: spelling a word from its letters is not evidence of
            // remembering what it means, and the review schedule is a thesis claim (PRODUCT.md).
            _uiState.value = _uiState.value.copy(
                starsEarned = stars,
                finalXp = xp,
                nextLevel = (levelNumber + 1).takeIf { it <= WordWheelTier.MAX_LEVEL }
            )
        }
    }
}
