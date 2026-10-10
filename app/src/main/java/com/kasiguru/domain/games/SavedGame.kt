package com.kasiguru.domain.games

/**
 * A game the learner left before its end, kept so that opening the same level again carries on
 * where they stopped instead of dealing a fresh round.
 *
 * Saved after every answer rather than only on the way out, so a back gesture or a closed app keeps
 * the round too. Cleared as the round finishes, before its reward is given, so no round can be
 * resumed, and rewarded, twice. See `SavedGameRepository`.
 */
sealed interface SavedGame

/** One answer already given, as the end-of-round review lists it. */
data class SavedAnswer(
    val prompt: String,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val subPrompt: String? = null
)

/**
 * A question round partway through: Word Match, Reverse Match, Fill in the Blank, Recall, Aspect
 * Builder or Sentence Order.
 *
 * Answered questions live on only in [answers]. The next question is rebuilt from its key, so one
 * the learner had open but not answered can come back with its options in a different order.
 */
data class SavedRound(
    /** The round's questions in order: dictionary word ids, or for Sentence Order the sentence itself. */
    val questionKeys: List<String>,
    val answers: List<SavedAnswer>,
    val score: Int,
    /** What the round is scored out of. Can exceed [questionKeys] when the dictionary is small. */
    val totalQuestions: Int,
    /** A hint was shown at some point in the round, which costs the perfect bonus. */
    val usedHint: Boolean,
    /** The next question's hint was already showing, so it is shown, and graded as hinted, again. */
    val hintRevealed: Boolean
) : SavedGame {
    /** Where the round picks up: the first question not yet answered. */
    val nextIndex: Int get() = answers.size
}

/** A Word Search grid partway through. The grid itself is rebuilt from its seed. */
data class SavedWordSearch(
    /** The words hidden in the grid, to tell whether a dictionary update has since changed it. */
    val puzzleWordIds: List<Int>,
    /** In the order found, which also fixes each word's highlight colour. */
    val foundIds: List<Int>,
    val misses: Int,
    val hintRevealed: Boolean
) : SavedGame

/** A Word Wheel board partway through. The board itself is rebuilt from the dictionary. */
data class SavedWordWheel(
    /** The board's words in slot order, to tell whether a dictionary update has since changed it. */
    val boardWords: List<String>,
    /** Board slots found, in the order found. */
    val foundSlots: List<Int>,
    /** Bonus words found, in the order found. */
    val bonusWords: List<String>,
    val hintsUsed: Int,
    /** The board slot whose clue was showing, or null. */
    val cluedSlot: Int?
) : SavedGame
