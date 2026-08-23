package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import kotlin.math.abs

/**
 * How hard the wrong answers beside a word should be.
 *
 * A multiple choice is only as good as what it asks the learner to rule out, and the right amount of
 * confusability is not fixed — it depends on how well the word is known. Meeting `apak` for the first
 * time beside three other tools tests nothing but luck; meeting it for the twentieth time beside
 * three unrelated words tests nothing at all.
 */
enum class DistractorDifficulty {
    /** Anything but the answer. A word still being introduced needs a winnable question. */
    GENTLE,

    /** Same semantic field. The default once a word is established. */
    STANDARD,

    /** Same field *and* similar shape, so the choice turns on knowing the word exactly. */
    TIGHT
}

/**
 * Chooses the wrong answers for one exercise.
 *
 * Split out of the repository so the rule is testable without a database: the repository's job is to
 * fetch candidates, this decides which of them make a question worth answering.
 */
object DistractorSelector {

    /**
     * Reads difficulty off the word's own SM-2 state.
     *
     * The easiness factor is the app's existing measure of how well a word is holding: it starts at
     * 2.5, falls on every failure, and rises only on fast, confident recall. A word the learner keeps
     * missing has both a low factor and few reviews behind it, and stacking near-identical
     * alternatives beside it turns a struggle into a guess.
     */
    fun difficultyFor(word: VocabularyEntity): DistractorDifficulty = when {
        word.timesReviewed < 2 || word.easinessFactor < STRUGGLING_FACTOR -> DistractorDifficulty.GENTLE
        word.isLearned || word.easinessFactor >= CONFIDENT_FACTOR -> DistractorDifficulty.TIGHT
        else -> DistractorDifficulty.STANDARD
    }

    /**
     * Picks [count] distractors for [target].
     *
     * [sameCategory] and [otherCategory] are candidate pools; both may be short, and the result falls
     * back through the difficulties rather than returning fewer options than the exercise needs — a
     * question with two choices is worse than a question with an easy third.
     */
    fun choose(
        target: VocabularyEntity,
        sameCategory: List<VocabularyEntity>,
        otherCategory: List<VocabularyEntity>,
        difficulty: DistractorDifficulty,
        count: Int
    ): List<VocabularyEntity> {
        val near = sameCategory.filter { it.id != target.id && it.kasiguranin.isNotBlank() }
        val far = otherCategory.filter { it.id != target.id && it.kasiguranin.isNotBlank() }

        val ordered = when (difficulty) {
            // Shape-alike first, then the rest of the category, then anything.
            DistractorDifficulty.TIGHT ->
                near.sortedByDescending { shapeSimilarity(target, it) } + far

            DistractorDifficulty.STANDARD -> near.shuffled() + far.shuffled()

            // Deliberately away from the answer's own field, and away from its shape.
            DistractorDifficulty.GENTLE ->
                far.sortedBy { shapeSimilarity(target, it) } + near.shuffled()
        }

        return ordered.distinctBy { it.id }.take(count)
    }

    /**
     * How easily two headwords could be mistaken for one another: same first letter, similar length,
     * shared ending. Crude on purpose — the corpus has no semantic vectors, and these are the
     * confusions a learner actually reports.
     */
    private fun shapeSimilarity(target: VocabularyEntity, other: VocabularyEntity): Int {
        val a = target.kasiguranin.lowercase()
        val b = other.kasiguranin.lowercase()
        var score = 0
        if (a.firstOrNull() == b.firstOrNull()) score += 2
        if (abs(a.length - b.length) <= 1) score += 2
        if (a.length >= 2 && b.length >= 2 && a.takeLast(2) == b.takeLast(2)) score += 1
        return score
    }

    /** Below this the word is being missed often enough that harder options would only add noise. */
    private const val STRUGGLING_FACTOR = 2.0

    /** At or above this the word is recalled quickly and cleanly, and deserves a real test. */
    private const val CONFIDENT_FACTOR = 2.5
}
