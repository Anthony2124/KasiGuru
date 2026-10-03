package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.WordEncounterDao
import com.kasiguru.data.local.entity.WordEncounterEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which dictionary words the learner has met, and where: the source of the Library's My words list.
 *
 * Lessons, flashcard reviews and all eight games report the words they showed through [record].
 * Depends on its table alone, so the repositories that award XP can call it without a cycle.
 */
@Singleton
class WordEncounterRepository @Inject constructor(
    private val dao: WordEncounterDao
) {
    fun observeAll(): Flow<List<WordEncounterEntity>> = dao.observeAll()

    fun observeCount(): Flow<Int> = dao.observeCount()

    /** Marks [wordIds] as met just now in [source] ("lesson", "review", or a game mode). */
    suspend fun record(wordIds: Collection<Int>, source: String, at: Long = System.currentTimeMillis()) {
        val ids = wordIds.filter { it > 0 }.distinct()
        if (ids.isEmpty()) return
        val existing = dao.getAll(ids).associateBy { it.wordId }
        dao.upsertAll(ids.map { id ->
            existing[id]?.let { met -> met.copy(lastSeenAt = at, timesSeen = met.timesSeen + 1, lastSource = source) }
                ?: WordEncounterEntity(id, at, at, 1, source)
        })
    }

    /**
     * Adds the words met before encounters were recorded - anything reviewed or learned, and the
     * words of every finished lesson - without touching rows already there. Safe to run on every
     * open: after a sign-in restores progress from the cloud, this is what brings the list back.
     *
     * Reviewed words carry no record of when they were reviewed, so they are dated [UNKNOWN_TIME]:
     * shown as met "earlier", and sorted after every word with a real time.
     *
     * @param finishedLessons each finished lesson's completion time and its word ids.
     */
    suspend fun fillFromHistory(finishedLessons: List<Pair<Long, List<Int>>>) {
        dao.fillFromReviews(UNKNOWN_TIME)
        val rows = finishedLessons
            .flatMap { (at, ids) -> ids.map { id -> id to at } }
            .groupBy({ it.first }, { it.second })
            .map { (id, times) -> WordEncounterEntity(id, times.min(), times.max(), times.size, "lesson") }
        if (rows.isNotEmpty()) dao.insertMissing(rows)
    }

    companion object {
        /** The time given to words met before encounters were recorded, when the real time is unknown. */
        const val UNKNOWN_TIME = 0L
    }
}
