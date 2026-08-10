package com.kasiguru.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kasiguru.data.local.DatabaseSeeder
import com.kasiguru.data.local.KasiGuruDatabase
import com.kasiguru.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        vocabularyDaoProvider: Provider<VocabularyDao>,
        storyDaoProvider: Provider<StoryDao>,
        userProgressDaoProvider: Provider<UserProgressDao>,
        achievementDaoProvider: Provider<AchievementDao>,
        leaderboardDaoProvider: Provider<LeaderboardDao>,
        notificationDaoProvider: Provider<NotificationDao>,
        gameLevelDaoProvider: Provider<GameLevelDao>
    ): KasiGuruDatabase {
        return Room.databaseBuilder(
            context,
            KasiGuruDatabase::class.java,
            "kasiguru_database"
        )
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        if (vocabularyDaoProvider.get().getTotalCountDirect() == 0) {
                            vocabularyDaoProvider.get().insertAll(DatabaseSeeder.getInitialVocabulary())
                        }
                        if (storyDaoProvider.get().getStoryCount() == 0) {
                            storyDaoProvider.get().insertAll(DatabaseSeeder.getInitialStories())
                        }
                        if (achievementDaoProvider.get().getAchievementCount() == 0) {
                            achievementDaoProvider.get().insertAll(DatabaseSeeder.getInitialAchievements())
                        }
                        if (leaderboardDaoProvider.get().getLeaderboardCount() == 0) {
                            leaderboardDaoProvider.get().insertAll(DatabaseSeeder.getInitialLeaderboard())
                        }
                        if (notificationDaoProvider.get().getNotificationCount() == 0) {
                            notificationDaoProvider.get().insertAll(DatabaseSeeder.getInitialNotifications())
                        }
                        if (gameLevelDaoProvider.get().getLevelCount() == 0) {
                            gameLevelDaoProvider.get().insertAll(DatabaseSeeder.getInitialGameLevels())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideVocabularyDao(database: KasiGuruDatabase): VocabularyDao =
        database.vocabularyDao()

    @Provides
    @Singleton
    fun provideConjugationDao(database: KasiGuruDatabase): ConjugationDao =
        database.conjugationDao()

    @Provides
    @Singleton
    fun provideStoryDao(database: KasiGuruDatabase): StoryDao =
        database.storyDao()

    @Provides
    @Singleton
    fun provideUserProgressDao(database: KasiGuruDatabase): UserProgressDao =
        database.userProgressDao()

    @Provides
    @Singleton
    fun provideAchievementDao(database: KasiGuruDatabase): AchievementDao =
        database.achievementDao()

    @Provides
    @Singleton
    fun provideGameScoreDao(database: KasiGuruDatabase): GameScoreDao =
        database.gameScoreDao()

    @Provides
    @Singleton
    fun provideSyncQueueDao(database: KasiGuruDatabase): SyncQueueDao =
        database.syncQueueDao()

    @Provides
    @Singleton
    fun provideLeaderboardDao(database: KasiGuruDatabase): LeaderboardDao =
        database.leaderboardDao()

    @Provides
    @Singleton
    fun provideNotificationDao(database: KasiGuruDatabase): NotificationDao =
        database.notificationDao()

    @Provides
    @Singleton
    fun provideGameLevelDao(database: KasiGuruDatabase): GameLevelDao =
        database.gameLevelDao()
}
