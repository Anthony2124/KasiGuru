package com.kasiguru.ui.screens.games.wordwheel

import com.kasiguru.domain.games.tagalogGloss
import com.kasiguru.domain.games.wordWheelHint
import com.kasiguru.ui.components.HintLanguages
import com.kasiguru.ui.components.hintFor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.GameScoreEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.GameLevelRepository
import com.kasiguru.data.repository.GameRepository
import com.kasiguru.data.repository.SavedGameRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.games.SavedWordWheel
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
    val bonusFound: List<WheelWord> = emptyList(),
    /** Each hint is a clue for one hidden word; see [WordWheelViewModel.hint]. */
    val hintsUsed: Int = 0,
    /** The board slot whose clue is showing, until that word is found. */
    val cluedSlot: Int? = null,
    val feedback: WheelFeedback? = null,
    val entries: Map<Int, VocabularyEntity> = emptyMap(),
    val isGameOver: Boolean = false,
    val starsEarned: Int = 0,
    val finalXp: Int = 0,
    val nextLevel: Int? = null
) {
    val hintsLeft: Int
        get() = (WordWheelViewModel.MAX_HINTS - hintsUsed).coerceAtLeast(0)

    val attempt: String
        get() = puzzle?.let { p -> selection.joinToString("") { p.wheel[it] } }.orEmpty()

    /** Board cells whose letter is showing: every cell of a found word. */
    val shownCells: Set<BoardCell>
        get() {
            val p = puzzle ?: return emptySet()
            return foundSlots.flatMap { p.slots[it].cells }.toSet()
        }

    /** The clue showing under the board, or null when no hint is in play. */
    val clue: String?
        get() = cluedSlot?.let { puzzle?.slots?.getOrNull(it) }?.let { clueFor(it.word) }

    /** The word the next hint clues: the shortest still hidden that has a meaning to give. */
    val nextClueSlot: Int?
        get() {
            val p = puzzle ?: return null
            return p.slots.indices
                .filter { it !in foundSlots && clueFor(p.slots[it].word) != null }
                .minByOrNull { p.slots[it].word.letters.size }
        }

    /** The hint button does something: shows the clue again, or gives the next one. */
    val canHint: Boolean
        get() = cluedSlot != null || (hintsLeft > 0 && nextClueSlot != null)

    /**
     * A hint's clue for [word]: its length and English meaning, never the word itself. English only,
     * since the Tagalog is often spelled close to the Kasiguranin it would give away. Null when the
     * dictionary has no meaning written for it.
     */
    private fun clueFor(word: WheelWord): String? =
        wordWheelHint(listOf(word.letters.size to entries[word.id]?.let { hintFor(it, HintLanguages.EnglishOnly) }))
}

@HiltViewModel
class WordWheelViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameRepository: GameRepository,
    private val gameLevelRepository: GameLevelRepository,
    private val savedGames: SavedGameRepository,
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
            val saved = savedGames.load(Constants.Games.WORD_WHEEL, levelNumber) as? SavedWordWheel
            _uiState.value = withSavedProgress(
                _uiState.value.copy(
                    isLoading = false,
                    isUnavailable = puzzle == null,
                    tier = WordWheelTier.forLevel(levelNumber),
                    puzzle = puzzle,
                    wheelOrder = puzzle?.wheel?.indices?.toList().orEmpty(),
                    entries = words.associateBy { it.id }
                ),
                saved
            )
        }
    }

    /**
     * [state] with [saved]'s progress put back on its board. The board is rebuilt from the dictionary,
     * so when an update has since changed it, the saved words no longer match and the level starts over.
     */
    private fun withSavedProgress(state: WordWheelUiState, saved: SavedWordWheel?): WordWheelUiState {
        val puzzle = state.puzzle ?: return state
        if (saved == null || saved.boardWords != puzzle.slots.map { it.word.key }) return state
        if (saved.foundSlots.any { it !in puzzle.slots.indices }) return state
        return state.copy(
            foundSlots = saved.foundSlots,
            bonusFound = saved.bonusWords.mapNotNull { key -> puzzle.bonusWords.firstOrNull { it.key == key } },
            hintsUsed = saved.hintsUsed.coerceAtMost(MAX_HINTS),
            cluedSlot = saved.cluedSlot?.takeIf { it in puzzle.slots.indices && it !in saved.foundSlots }
        )
    }

    /** Keeps the board so far, so that leaving the game does not lose it. */
    private fun saveProgress() {
        val state = _uiState.value
        val puzzle = state.puzzle ?: return
        if (state.isGameOver) return
        savedGames.save(
            Constants.Games.WORD_WHEEL, levelNumber,
            SavedWordWheel(
                boardWords = puzzle.slots.map { it.word.key },
                foundSlots = state.foundSlots,
                bonusWords = state.bonusFound.map { it.key },
                hintsUsed = state.hintsUsed,
                cluedSlot = state.cluedSlot
            )
        )
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
        val after = _uiState.value
        if (after.foundSlots != state.foundSlots || after.bonusFound != state.bonusFound) saveProgress()
    }

    /**
     * A clue for one hidden word, its length and meaning, as Word Match's hint gives a meaning. The
     * clue stays under the board until that word is found; only then does the next hint give another,
     * and pressing for one before that just shows the current clue again. At most [MAX_HINTS] per
     * level, and each one lowers the stars the level can earn, so the choice stays the learner's.
     */
    fun hint() {
        val state = _uiState.value
        if (state.isGameOver) return
        if (state.cluedSlot != null) {
            _uiState.value = state.copy(feedback = null)
            return
        }
        if (state.hintsLeft <= 0) return
        val slot = state.nextClueSlot ?: return
        _uiState.value = state.copy(cluedSlot = slot, hintsUsed = state.hintsUsed + 1, feedback = null)
        saveProgress()
    }

    /**
     * A word whose every letter is showing through crossings counts as found. A clue whose word is now
     * found is done with, which frees the hint button for the next one.
     */
    private fun completeFilledSlots(state: WordWheelUiState): WordWheelUiState {
        val puzzle = state.puzzle ?: return state
        val shown = state.shownCells
        val completed = puzzle.slots.indices.filter { it !in state.foundSlots && puzzle.slots[it].cells.all(shown::contains) }
        val found = state.foundSlots + completed
        return state.copy(foundSlots = found, cluedSlot = state.cluedSlot?.takeIf { it !in found })
    }

    private fun glossFor(word: WheelWord): String = _uiState.value.entries[word.id]?.let(::tagalogGloss).orEmpty()

    private fun finishIfSolved() {
        val state = _uiState.value
        val puzzle = state.puzzle ?: return
        if (state.isGameOver || state.foundSlots.size < puzzle.slots.size) return
        // Before the reward, so the board can never be picked up and rewarded again.
        savedGames.clear(Constants.Games.WORD_WHEEL, levelNumber)

        val boardWords = puzzle.slots.size
        val stars = when {
            state.hintsUsed == 0 -> 3
            state.hintsUsed <= 2 -> 2
            else -> 1
        }
        // Half the per-answer XP of the choice games per board word, and half again per bonus word:
        // spelling a word from its letters is a recall of form, not of meaning.
        _uiState.value = state.copy(isGameOver = true)

        viewModelScope.launch {
            val xp = userProgressRepository.awardGame("word_wheel",Constants.Games.WORD_WHEEL,levelNumber,boardWords,boardWords,stars,state.hintsUsed == 0,
                puzzle.slots.map { it.word.id } + state.bonusFound.map { it.id })
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
            userProgressRepository.incrementGamesPlayed()
            // Wrong guesses are how this game is played, so they do not count against accuracy.
            // A hint does: it is a word the learner could not produce.
            userProgressRepository.updateGameStats(boardWords, boardWords + state.hintsUsed)

            // No SM-2 review for these words: spelling a word from its letters is not evidence of
            // remembering what it means, and the review schedule is a thesis claim (PRODUCT.md).
            _uiState.value = _uiState.value.copy(
                starsEarned = stars,
                finalXp = xp,
                nextLevel = (levelNumber + 1).takeIf { it <= WordWheelTier.MAX_LEVEL }
            )
        }
    }

    companion object {
        /** Hints allowed per level. Three is also where the star rating bottoms out at one star. */
        const val MAX_HINTS = 3
    }
}
