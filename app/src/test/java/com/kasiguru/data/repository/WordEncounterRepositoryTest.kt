package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.WordEncounterDao
import com.kasiguru.data.local.entity.WordEncounterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordEncounterRepositoryTest {

    /** The table as a map, with Room's upsert and insert-or-ignore semantics. */
    private class FakeDao : WordEncounterDao {
        val rows = MutableStateFlow<Map<Int, WordEncounterEntity>>(emptyMap())
        var filledFromReviews = 0
        override suspend fun getAll(wordIds: List<Int>) = wordIds.mapNotNull { rows.value[it] }
        override suspend fun upsertAll(rows: List<WordEncounterEntity>) {
            this.rows.value = this.rows.value + rows.associateBy { it.wordId }
        }
        override fun observeAll(): Flow<List<WordEncounterEntity>> =
            rows.map { it.values.sortedByDescending { row -> row.lastSeenAt } }
        override fun observeCount(): Flow<Int> = rows.map { it.size }
        override suspend fun insertMissing(rows: List<WordEncounterEntity>) {
            this.rows.value = this.rows.value + rows.filter { it.wordId !in this.rows.value }.associateBy { it.wordId }
        }
        override suspend fun fillFromReviews(at: Long) { filledFromReviews++ }
        override suspend fun clearAll() { rows.value = emptyMap() }
    }

    @Test
    fun meetingAWordAgainUpdatesItsRowInsteadOfAddingOne() = runBlocking {
        val dao = FakeDao()
        val repository = WordEncounterRepository(dao)

        repository.record(listOf(4, 9, 4), "lesson", at = 1_000)
        repository.record(listOf(9), "word_match", at = 2_000)

        val word9 = dao.rows.value.getValue(9)
        assertEquals(2, dao.rows.value.size)
        assertEquals(1_000L, word9.firstSeenAt)
        assertEquals(2_000L, word9.lastSeenAt)
        assertEquals(2, word9.timesSeen)
        assertEquals("word_match", word9.lastSource)
        // A word listed twice in one round is still one meeting.
        assertEquals(1, dao.rows.value.getValue(4).timesSeen)
    }

    @Test
    fun invalidIdsAndEmptyRoundsRecordNothing() = runBlocking {
        val dao = FakeDao()
        WordEncounterRepository(dao).record(listOf(0, -3), "review")
        WordEncounterRepository(dao).record(emptyList(), "word_wheel")
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun historyFillAddsFinishedLessonsButNeverOverwritesAWordAlreadyMet() = runBlocking {
        val dao = FakeDao()
        val repository = WordEncounterRepository(dao)
        repository.record(listOf(1), "word_search", at = 9_000)

        repository.fillFromHistory(listOf(100L to listOf(1, 2), 300L to listOf(2, 3)))

        assertEquals(1, dao.filledFromReviews)
        assertEquals("word_search", dao.rows.value.getValue(1).lastSource)
        assertEquals(9_000L, dao.rows.value.getValue(1).lastSeenAt)
        val word2 = dao.rows.value.getValue(2)
        assertEquals(100L, word2.firstSeenAt)
        assertEquals(300L, word2.lastSeenAt)
        assertEquals(2, word2.timesSeen)
        assertEquals("lesson", dao.rows.value.getValue(3).lastSource)
    }
}
