package com.kasiguru.data.local.dao

import androidx.room.*
import com.kasiguru.data.local.entity.RewardReceiptEntity
import com.kasiguru.data.local.entity.ProgressNormalizationEntity
import com.kasiguru.data.local.entity.RewardCelebrationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {
    @Query("SELECT * FROM reward_receipts") suspend fun all(): List<RewardReceiptEntity>
    @Query("SELECT * FROM reward_receipts") fun observe(): Flow<List<RewardReceiptEntity>>
    @Query("SELECT * FROM reward_receipts WHERE id = :id") suspend fun get(id: String): RewardReceiptEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(row: RewardReceiptEntity)
    @Query("SELECT * FROM progress_normalization WHERE version = 2") suspend fun normalization(): ProgressNormalizationEntity?
    @Query("SELECT * FROM progress_normalization WHERE version = 2") fun observeNormalization(): Flow<ProgressNormalizationEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putNormalization(row: ProgressNormalizationEntity)
    @Query("UPDATE progress_normalization SET acknowledged = 1 WHERE version = 2") suspend fun acknowledge()
    @Query("DELETE FROM reward_receipts") suspend fun clearReceipts()
    @Query("DELETE FROM progress_normalization") suspend fun clearNormalization()
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun celebrate(row: RewardCelebrationEntity)
    @Query("SELECT * FROM reward_celebrations WHERE acknowledged = 0 ORDER BY createdAt, id LIMIT 1")
    fun pendingCelebration(): Flow<RewardCelebrationEntity?>
    @Query("UPDATE reward_celebrations SET acknowledged = 1 WHERE id = :id") suspend fun acknowledgeCelebration(id: String)
    @Query("DELETE FROM reward_celebrations") suspend fun clearCelebrations()
}
