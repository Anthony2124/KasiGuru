package com.kasiguru.data.local

import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.VocabularyEntity

/**
 * Brings an already-installed device up to the corpus the current build ships.
 *
 * [DatabaseModule]'s seeding callback is `onCreate`, which fires only for a database that did not
 * exist. That is right for a fresh install and useless for an upgrade: a migration adds columns, not
 * rows, so a learner who already had the app would have kept the old 394-word corpus with three
 * empty definition columns and would have seen none of this release's work until someone
 * backfilled Firestore and their next content sync happened to run.
 *
 * What this does, and deliberately no more:
 *
 *  - Inserts senses the device does not have. Keyed on headword *and* English gloss, so the three
 *    senses of `baga` are three rows and not one.
 *  - Fills in a definition or part of speech that is blank locally.
 *
 * It never overwrites a non-blank field, and it never touches a single learning column -- no
 * `isLearned`, no `timesReviewed`, no easiness factor, interval, review date, lapses or relearning
 * step. Content edited in the admin portal still wins later, through the normal sync.
 *
 * Safe to run on every launch: after the first pass it is a set difference over a few thousand rows
 * that finds nothing to do.
 */
object ContentTopUp {

    private fun senseKey(word: VocabularyEntity): String =
        word.kasiguranin.trim().lowercase() + "|" + word.english.trim().lowercase()

    /** What a top-up would change. Separated from the database so it can be tested directly. */
    data class Plan(
        val toInsert: List<VocabularyEntity>,
        val toUpdate: List<VocabularyEntity>
    ) {
        val isEmpty: Boolean get() = toInsert.isEmpty() && toUpdate.isEmpty()
    }

    /** Pure: what [local] is missing from [corpus], and which of its rows need a definition. */
    fun plan(local: List<VocabularyEntity>, corpus: List<VocabularyEntity>): Plan {
        // An empty table means onCreate is about to seed it, or already has. Nothing to top up.
        if (local.isEmpty()) return Plan(emptyList(), emptyList())

        val localByKey = local.associateBy(::senseKey)
        val toInsert = mutableListOf<VocabularyEntity>()
        val toUpdate = mutableListOf<VocabularyEntity>()

        for (word in corpus) {
            val existing = localByKey[senseKey(word)]
            if (existing == null) {
                // id 0 so Room assigns one; the corpus row carries no learning state to begin with.
                toInsert.add(word.copy(id = 0))
                continue
            }

            // Only the fields this release introduced, and only where the device has nothing.
            val patched = existing.copy(
                meaningEnglish = existing.meaningEnglish.ifBlank { word.meaningEnglish },
                meaningTagalog = existing.meaningTagalog.ifBlank { word.meaningTagalog },
                partOfSpeech = existing.partOfSpeech.ifBlank { word.partOfSpeech }
            )
            if (patched != existing) toUpdate.add(patched)
        }

        return Plan(toInsert, toUpdate)
    }

    suspend fun run(vocabularyDao: VocabularyDao) {
        val plan = plan(vocabularyDao.getAllVocabularyOnce(), DatabaseSeeder.getInitialVocabulary())
        if (plan.isEmpty) return

        if (plan.toInsert.isNotEmpty()) vocabularyDao.insertAll(plan.toInsert)
        // insertAll is REPLACE on the primary key, so an update is an insert of a row that already
        // carries its id -- and every learning column travels with it untouched.
        if (plan.toUpdate.isNotEmpty()) vocabularyDao.insertAll(plan.toUpdate)
    }
}
