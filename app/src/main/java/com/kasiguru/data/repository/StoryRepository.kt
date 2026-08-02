package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.StoryDao
import com.kasiguru.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(
    private val storyDao: StoryDao
) {
    fun getAllStories(): Flow<List<StoryEntity>> =
        storyDao.getAllStories()

    fun getUnlockedStories(): Flow<List<StoryEntity>> =
        storyDao.getUnlockedStories()

    suspend fun getStoryById(id: Int): StoryEntity? =
        storyDao.getStoryById(id)

    fun getCompletedCount(): Flow<Int> =
        storyDao.getCompletedCount()

    suspend fun markAsCompleted(id: Int) =
        storyDao.markAsCompleted(id)

    suspend fun updateCurrentPage(id: Int, page: Int) =
        storyDao.updateCurrentPage(id, page)

    suspend fun unlockStoriesByXp(xp: Int) =
        storyDao.unlockStoriesByXp(xp)
}
