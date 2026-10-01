package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kasiguru.domain.gamification.RewardRecord

@Entity(tableName = "reward_receipts")
data class RewardReceiptEntity(@PrimaryKey val id: String, val kind: String, val source: String,
    val day: String, val xp: Int, val value: Int = 1, val imported: Boolean = false) {
    fun record() = RewardRecord(id,kind,source,day,xp,value,imported)
}

@Entity(tableName = "progress_normalization")
data class ProgressNormalizationEntity(@PrimaryKey val version: Int = 2,
    val originalXp: Int, val originalLevel: Int, val normalizedXp: Int,
    val unsupportedHistory: String, val acknowledged: Boolean = false)

@Entity(tableName = "reward_celebrations")
data class RewardCelebrationEntity(@PrimaryKey val id: String, val activityXp: Int,
    val bonusXp: Int, val level: Int, val levelChanged: Boolean, val badgeIds: String,
    val createdAt: Long, val acknowledged: Boolean = false)
