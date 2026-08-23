package com.kasiguru.domain.lesson

/**
 * Identifies one lesson: a slice of words inside a unit.
 *
 * @param unitId the vocabulary category name, used verbatim.
 * @param lessonIndex zero-based position within that unit.
 */
data class LessonRef(val unitId: String, val lessonIndex: Int)

/**
 * A unit as the learner sees it: one vocabulary category, split into lessons.
 *
 * @param completedLessons how many of [lessonCount] the learner has finished.
 */
data class LessonUnit(
    val id: String,
    val title: String,
    val wordCount: Int,
    val lessonCount: Int,
    val completedLessons: Int
) {
    val isComplete: Boolean get() = lessonCount > 0 && completedLessons >= lessonCount
    val progress: Float get() = if (lessonCount == 0) 0f else completedLessons.toFloat() / lessonCount
}

/**
 * How the 417-word corpus is turned into lessons.
 *
 * A unit is a vocabulary category and a lesson is a fixed-size slice of it. Nothing about the split is
 * stored, which matters for two reasons: the admin portal can add words without a schema migration,
 * and there is no authored lesson content to keep in sync with the dictionary. The trade-off is that
 * inserting a word shifts the contents of later lessons in that unit — acceptable, because progress is
 * recorded per lesson index rather than per word, and words themselves carry their own SM-2 state.
 */
object LessonPlan {

    /**
     * Words introduced per lesson. Seven keeps a lesson to roughly two minutes, which is the length
     * a daily-habit app has to fit into; it also stays under the working-memory span for new items.
     */
    const val WORDS_PER_LESSON = 7

    /**
     * Exercises in a lesson. Some words appear twice, in different exercise types.
     *
     * Raised from nine when interleaving began adding up to two revisited words to the seven new
     * ones: at nine, a nine-word lesson filled its whole run in the first pass and no word was ever
     * met twice, which is the part that makes a lesson teach rather than test.
     */
    const val EXERCISES_PER_LESSON = 11

    /** XP for finishing a lesson, before the accuracy bonus. */
    const val XP_PER_LESSON = 30

    /** Extra XP for a lesson completed without a single wrong first answer. */
    const val XP_PERFECT_BONUS = 15

    fun lessonCountFor(wordCount: Int): Int =
        if (wordCount <= 0) 0 else (wordCount + WORDS_PER_LESSON - 1) / WORDS_PER_LESSON

    /**
     * The slice of a unit's word list belonging to [lessonIndex]. Returns an empty range when the
     * index is past the end, so callers do not have to bounds-check.
     */
    fun wordIndicesFor(lessonIndex: Int, wordCount: Int): IntRange {
        val start = lessonIndex * WORDS_PER_LESSON
        if (start >= wordCount) return IntRange.EMPTY
        val end = minOf(start + WORDS_PER_LESSON, wordCount) - 1
        return start..end
    }

    /** XP for a run, given the fraction of exercises answered correctly on the first attempt. */
    fun xpFor(accuracy: Float): Int =
        XP_PER_LESSON + if (accuracy >= 1f) XP_PERFECT_BONUS else 0
}
