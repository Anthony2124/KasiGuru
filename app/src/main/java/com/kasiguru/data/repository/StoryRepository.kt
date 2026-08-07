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

    fun getUnlockedStories(): Flow<List<StoryEntity>> =
        storyDao.getUnlockedStories()

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
