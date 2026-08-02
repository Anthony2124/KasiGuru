package com.kasiguru.data.local.dao

import androidx.room.*
import com.kasiguru.data.local.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {

    @Query("SELECT * FROM vocabulary ORDER BY category, kasiguranin")
    fun getAllVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE category = :category ORDER BY kasiguranin")
    fun getVocabularyByCategory(category: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE id = :id")
    suspend fun getVocabularyById(id: Int): VocabularyEntity?

    @Query("SELECT * FROM vocabulary WHERE isLearned = 1 ORDER BY kasiguranin")
    fun getLearnedVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE isLearned = 0 ORDER BY RANDOM() LIMIT :count")
    suspend fun getUnlearnedWords(count: Int): List<VocabularyEntity>

    @Query("SELECT * FROM vocabulary ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomWords(count: Int): List<VocabularyEntity>

    @Query("SELECT COUNT(*) FROM vocabulary WHERE isLearned = 1")
    fun getLearnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary")
    suspend fun getTotalCountDirect(): Int

    @Query("SELECT DISTINCT category FROM vocabulary ORDER BY category")
    fun getCategories(): Flow<List<String>>

    @Update
    suspend fun updateVocabulary(vocabulary: VocabularyEntity)

    @Query("UPDATE vocabulary SET isLearned = 1, timesReviewed = timesReviewed + 1 WHERE id = :id")
    suspend fun markAsLearned(id: Int)

    @Query("UPDATE vocabulary SET timesReviewed = timesReviewed + 1 WHERE id = :id")
    suspend fun incrementReviewCount(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vocabulary: List<VocabularyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vocabulary: VocabularyEntity)
}
