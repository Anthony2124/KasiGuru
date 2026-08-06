package com.kasiguru.data.local.dao

import androidx.room.*
import com.kasiguru.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

    @Query("SELECT * FROM stories ORDER BY requiredXp ASC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isUnlocked = 1 ORDER BY id")
    fun getUnlockedStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getStoryById(id: Int): StoryEntity?

    @Query("SELECT COUNT(*) FROM stories WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getStoryCount(): Int

    @Query("UPDATE stories SET isCompleted = 1, currentPage = totalPages WHERE id = :id")
    suspend fun markAsCompleted(id: Int)

    @Query("UPDATE stories SET currentPage = :page WHERE id = :id")
    suspend fun updateCurrentPage(id: Int, page: Int)

    @Query("UPDATE stories SET isUnlocked = 1 WHERE id = :id")
    suspend fun unlockStory(id: Int)

    @Query("UPDATE stories SET isUnlocked = 1 WHERE requiredXp <= :xp")
    suspend fun unlockStoriesByXp(xp: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stories: List<StoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(story: StoryEntity)
}
