package com.kasiguru.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kasiguru.data.local.entity.LessonProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

    @Query("SELECT * FROM lesson_progress")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress")
    suspend fun getAllOnce(): List<LessonProgressEntity>

    @Query("SELECT * FROM lesson_progress WHERE unitId = :unitId ORDER BY lessonIndex ASC")
    fun observeUnit(unitId: String): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE unitId = :unitId AND lessonIndex = :lessonIndex")
    suspend fun get(unitId: String, lessonIndex: Int): LessonProgressEntity?

    @Query("SELECT COUNT(*) FROM lesson_progress WHERE isComplete = 1")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM lesson_progress WHERE isComplete = 1 AND unitId = :unitId")
    suspend fun completedCountInUnit(unitId: String): Int

    /**
     * Completions recorded on or after [sinceEpochMillis], newest first.
     * Backs the weekly activity strip and the XP chart on Progress.
     */
    @Query("SELECT * FROM lesson_progress WHERE lastCompletedAt >= :sinceEpochMillis ORDER BY lastCompletedAt DESC")
    suspend fun completedSince(sinceEpochMillis: Long): List<LessonProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LessonProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progress: List<LessonProgressEntity>)
}
