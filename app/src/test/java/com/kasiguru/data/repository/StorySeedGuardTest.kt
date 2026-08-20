package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.StoryDao
import com.kasiguru.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every read path on [StoryRepository] must seed an empty table before it reads.
 *
 * This exists because exactly one of the four did not. `getUnlockedStories()` went straight to the
 * DAO, so on a first launch it raced Room's `onCreate` seeding callback: `LearnViewModel` builds
 * Today's Path before it builds the skills grid, saw zero unlocked stories, and silently dropped the
 * story row. The feature looked deleted while being entirely present — the two stories that ship
 * unlocked at 0 XP were simply never offered.
 *
 * The parity test below is the point. Asserting the one fixed method still seeds would not have
 * caught the original bug, because the original bug was a method nobody thought to check.
 */
class StorySeedGuardTest {

    /** In-memory stand-in for the Room DAO. Starts empty, like a freshly created database. */
    private class FakeStoryDao(private val rows: MutableList<StoryEntity> = mutableListOf()) : StoryDao {
        var insertAllCalls = 0
            private set

        override fun getAllStories(): Flow<List<StoryEntity>> = flowOf(rows.toList())
        override suspend fun getAllStoriesOnce(): List<StoryEntity> = rows.toList()
        override fun getUnlockedStories(): Flow<List<StoryEntity>> = flowOf(rows.filter { it.isUnlocked })
        override suspend fun getStoryById(id: Int): StoryEntity? = rows.firstOrNull { it.id == id }
        override fun getCompletedCount(): Flow<Int> = flowOf(rows.count { it.isCompleted })
        override suspend fun getStoryCount(): Int = rows.size
        override suspend fun markAsCompleted(id: Int) {
            rows.replaceAll { if (it.id == id) it.copy(isCompleted = true) else it }
        }
        override suspend fun updateCurrentPage(id: Int, page: Int) {
            rows.replaceAll { if (it.id == id) it.copy(currentPage = page) else it }
        }
        override suspend fun unlockStory(id: Int) {
            rows.replaceAll { if (it.id == id) it.copy(isUnlocked = true) else it }
        }
        override suspend fun unlockStoriesByXp(xp: Int) {
            rows.replaceAll { if (it.requiredXp <= xp) it.copy(isUnlocked = true) else it }
        }
        override suspend fun insertAll(stories: List<StoryEntity>) {
            insertAllCalls++
            rows.addAll(stories)
        }
        override suspend fun insert(story: StoryEntity) {
            rows.add(story)
        }
    }

    @Test
    fun `getUnlockedStories seeds an empty table and returns the free stories`() = runBlocking {
        val dao = FakeStoryDao()
        val unlocked = StoryRepository(dao).getUnlockedStories().first()

        assertEquals("the empty table should have been seeded exactly once", 1, dao.insertAllCalls)
        assertTrue(
            "a first-launch learner must be offered the stories that ship unlocked at 0 XP",
            unlocked.isNotEmpty()
        )
        assertTrue("every story returned here must actually be unlocked", unlocked.all { it.isUnlocked })
    }

    @Test
    fun `every read path seeds an empty table`() = runBlocking {
        // One fresh DAO per path, so each is asked to seed from genuinely empty.
        val fromAll = FakeStoryDao().also { StoryRepository(it).getAllStories().first() }
        val fromUnlocked = FakeStoryDao().also { StoryRepository(it).getUnlockedStories().first() }
        val fromById = FakeStoryDao().also { StoryRepository(it).getStoryById(1) }
        val fromUnlockByXp = FakeStoryDao().also { StoryRepository(it).unlockStoriesByXp(0) }

        mapOf(
            "getAllStories" to fromAll,
            "getUnlockedStories" to fromUnlocked,
            "getStoryById" to fromById,
            "unlockStoriesByXp" to fromUnlockByXp
        ).forEach { (name, dao) ->
            assertEquals("$name must seed an empty table before reading", 1, dao.insertAllCalls)
        }
    }

    @Test
    fun `a populated table is never reseeded`() = runBlocking {
        val existing = StoryEntity(id = 1, title = "Already here", isUnlocked = true)
        val dao = FakeStoryDao(mutableListOf(existing))

        val unlocked = StoryRepository(dao).getUnlockedStories().first()

        assertEquals("seeding a non-empty table would duplicate every story", 0, dao.insertAllCalls)
        assertEquals(listOf(existing), unlocked)
    }
}
