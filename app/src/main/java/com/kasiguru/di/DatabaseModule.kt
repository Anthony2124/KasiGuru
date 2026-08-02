package com.kasiguru.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kasiguru.data.local.KasiGuruDatabase
import com.kasiguru.data.local.DatabaseSeeder
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
        achievementDaoProvider: Provider<AchievementDao>
    ): KasiGuruDatabase {
        return Room.databaseBuilder(
            context,
            KasiGuruDatabase::class.java,
            "kasiguru_database"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed the database on first creation
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    vocabularyDaoProvider.get().insertAll(DatabaseSeeder.getInitialVocabulary())
                    storyDaoProvider.get().insertAll(DatabaseSeeder.getInitialStories())
                    userProgressDaoProvider.get().insertOrUpdate(DatabaseSeeder.getInitialUserProgress())
                    achievementDaoProvider.get().insertAll(DatabaseSeeder.getInitialAchievements())
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
}
