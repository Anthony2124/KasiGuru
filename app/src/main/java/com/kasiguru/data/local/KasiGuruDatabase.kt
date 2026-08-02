package com.kasiguru.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kasiguru.data.local.dao.*
import com.kasiguru.data.local.entity.*

@Database(
    entities = [
        VocabularyEntity::class,
        StoryEntity::class,
        UserProgressEntity::class,
        AchievementEntity::class,
        GameScoreEntity::class,
        SyncQueueEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KasiGuruDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun storyDao(): StoryDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun achievementDao(): AchievementDao
    abstract fun gameScoreDao(): GameScoreDao
    abstract fun syncQueueDao(): SyncQueueDao
}
