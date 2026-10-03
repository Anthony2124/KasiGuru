package com.kasiguru.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kasiguru.data.local.entity.WordEncounterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordEncounterDao {
    @Query("SELECT * FROM word_encounters WHERE wordId IN (:wordIds)")
    suspend fun getAll(wordIds: List<Int>): List<WordEncounterEntity>

    @Upsert
    suspend fun upsertAll(rows: List<WordEncounterEntity>)

    @Query("SELECT * FROM word_encounters ORDER BY lastSeenAt DESC")
    fun observeAll(): Flow<List<WordEncounterEntity>>

    @Query("SELECT COUNT(*) FROM word_encounters")
    fun observeCount(): Flow<Int>

    /** Adds rows for words not met yet; leaves every existing row exactly as it is. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(rows: List<WordEncounterEntity>)

    /** Every word reviewed or learned before encounters were recorded counts as met in a review. */
    @Query(
        "INSERT OR IGNORE INTO word_encounters (wordId, firstSeenAt, lastSeenAt, timesSeen, lastSource) " +
            "SELECT id, :at, :at, MAX(timesReviewed, 1), 'review' FROM vocabulary " +
            "WHERE timesReviewed > 0 OR isLearned = 1"
    )
    suspend fun fillFromReviews(at: Long)

    @Query("DELETE FROM word_encounters")
    suspend fun clearAll()
}
