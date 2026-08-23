package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity

/**
 * Mixes older material into a lesson of new words.
 *
 * A lesson is a slice of one category, so every exercise in it comes from the same semantic field —
 * blocked practice. It feels easier, and it reliably retains worse than interleaved practice: when
 * every answer for two minutes is a tool, the learner is discriminating between tools rather than
 * recalling what the word means, and none of that transfers to meeting the word in a story.
 *
 * Mixing in a couple of words from elsewhere fixes that cheaply, and it is also the natural home for
 * the words the review deck cannot fix on its own: a leech has been known and forgotten five times,
 * and what it needs is to be taught again beside its meaning rather than asked a sixth time.
 */
object Interleaving {

    /** Older words woven into a lesson. Two, so the lesson is still mostly the new material. */
    const val REVISITED_PER_LESSON = 2

    /**
     * The lesson's own words, plus up to [REVISITED_PER_LESSON] revisited ones.
     *
     * Leeches come first, then words genuinely due — those are the two reasons to spend a new
     * lesson's attention on an old word. Anything already in the lesson is skipped, since a word
     * cannot be interleaved with itself, and the revisited words are appended rather than shuffled
     * in: the generator alternates exercise shapes across the list, and dropping them at the end
     * keeps the new material at the front where the lesson introduces it.
     */
    fun compose(
        lessonWords: List<VocabularyEntity>,
        leeches: List<VocabularyEntity>,
        due: List<VocabularyEntity>,
        limit: Int = REVISITED_PER_LESSON
    ): List<VocabularyEntity> {
        if (lessonWords.isEmpty() || limit <= 0) return lessonWords

        val alreadyHere = lessonWords.map { it.id }.toMutableSet()
        val revisited = mutableListOf<VocabularyEntity>()

        for (candidate in leeches + due) {
            if (revisited.size >= limit) break
            // A word from this lesson's own category is not interleaving — the whole point is that
            // the learner cannot lean on the field the lesson has put them in.
            if (candidate.id in alreadyHere) continue
            if (candidate.category.equals(lessonWords.first().category, ignoreCase = true)) continue
            revisited += candidate
            alreadyHere += candidate.id
        }

        return lessonWords + revisited
    }
}
