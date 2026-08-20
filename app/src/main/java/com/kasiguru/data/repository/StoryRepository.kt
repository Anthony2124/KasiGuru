package com.kasiguru.data.repository

import com.kasiguru.data.local.DatabaseSeeder
import com.kasiguru.data.local.dao.StoryDao
import com.kasiguru.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(
    private val storyDao: StoryDao
) {
    fun getAllStories(): Flow<List<StoryEntity>> = flow {
        val count = storyDao.getStoryCount()
        if (count == 0) {
            storyDao.insertAll(DatabaseSeeder.getInitialStories())
        }
        storyDao.getAllStories().collect { emit(it) }
    }

    /**
     * Seeds before reading, like its siblings above and below.
     *
     * Without the guard this raced the Room `onCreate` seeding callback on a first launch:
     * `LearnViewModel.buildActivities()` runs before `buildSkills()`, saw zero unlocked stories,
     * and silently dropped the story row from Today's Path — so a new learner was never offered
     * the two stories that ship unlocked at 0 XP.
     */
    fun getUnlockedStories(): Flow<List<StoryEntity>> = flow {
        if (storyDao.getStoryCount() == 0) {
            storyDao.insertAll(DatabaseSeeder.getInitialStories())
        }
        storyDao.getUnlockedStories().collect { emit(it) }
    }

    suspend fun getStoryById(id: Int): StoryEntity? {
        if (storyDao.getStoryCount() == 0) {
            storyDao.insertAll(DatabaseSeeder.getInitialStories())
        }
        return storyDao.getStoryById(id)
    }

    fun getCompletedCount(): Flow<Int> =
        storyDao.getCompletedCount()

    suspend fun markAsCompleted(id: Int) =
        storyDao.markAsCompleted(id)

    suspend fun updateCurrentPage(id: Int, page: Int) =
        storyDao.updateCurrentPage(id, page)

    suspend fun unlockStoriesByXp(xp: Int) {
        if (storyDao.getStoryCount() == 0) {
            storyDao.insertAll(DatabaseSeeder.getInitialStories())
        }
        storyDao.unlockStoriesByXp(xp)
    }
}
