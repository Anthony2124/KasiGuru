package com.kasiguru.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kasiguru.data.local.dao.*
import com.kasiguru.data.local.entity.*

@Database(
    entities = [
        VocabularyEntity::class,
        ConjugationEntity::class,
        StoryEntity::class,
        UserProgressEntity::class,
        AchievementEntity::class,
        GameScoreEntity::class,
        SyncQueueEntity::class,
        LeaderboardEntity::class,
        NotificationEntity::class,
        GameLevelEntity::class,
        LessonProgressEntity::class,
        ProfileEntity::class
    ],
    version = 27,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KasiGuruDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun conjugationDao(): ConjugationDao
    abstract fun storyDao(): StoryDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun achievementDao(): AchievementDao
    abstract fun gameScoreDao(): GameScoreDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun notificationDao(): NotificationDao
    abstract fun gameLevelDao(): GameLevelDao
    abstract fun lessonDao(): LessonDao
    abstract fun profileDao(): ProfileDao
}
