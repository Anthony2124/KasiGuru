package com.kasiguru.data.repository

import com.kasiguru.data.local.DatabaseSeeder
import com.kasiguru.data.local.dao.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages clean resetting of all local learner data when signing out or deleting an account.
 * Clears user-specific progress, streaks, daily quota requirements, and review schedules,
 * while ensuring dictionary content and initial game levels remain ready for guest use.
 */
@Singleton
class UserDataResetManager @Inject constructor(
    private val userProgressDao: UserProgressDao,
    private val vocabularyDao: VocabularyDao,
    private val lessonDao: LessonDao,
    private val gameLevelDao: GameLevelDao,
    private val achievementDao: AchievementDao,
    private val gameScoreDao: GameScoreDao,
    private val storyDao: StoryDao,
    private val notificationDao: NotificationDao,
    private val leaderboardDao: LeaderboardDao,
    private val profileDao: ProfileDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val progressSyncManager: dagger.Lazy<ProgressSyncManager>
) {

    /**
     * Completely resets all local user progress, daily goals/quota, and review states.
     *
     * @param uploadPendingChanges whether to push the local state to the signed-in account
     *   first. True on sign-out, so nothing earned in the last few minutes is lost. False on
     *   account deletion, where the cloud documents have already been removed and re-creating
     *   them would leave the deleted account's data behind.
     */
    suspend fun resetAllLocalUserData(uploadPendingChanges: Boolean = true) = withContext(Dispatchers.IO) {
        // 0. Everything below is destructive and the uploaders are debounced, so the last few
        //    minutes of play exist only in Room right now. Push them, and wait, before wiping.
        //    Without this, completing the day's review or the third mini-game and signing out
        //    within the debounce window destroyed both, and the next sign-in restored the
        //    quota and streak from before them.
        if (uploadPendingChanges) {
            progressSyncManager.get().flushToCloud()
        }

        // 1. Tell SyncManager to cancel active uploads and clear cached sync hashes
        progressSyncManager.get().onUserSignedOut()

        // 2. Clear DataStore daily streak quota and session preferences
        userPreferencesRepository.clearUserSessionData()

        // 3. Reset user progress row to clean default state
        userProgressDao.insertOrUpdate(DatabaseSeeder.getInitialUserProgress())

        // 4. Reset vocabulary review and learning schedules back to unlearned
        vocabularyDao.resetAllLearningProgress()

        // 5. Clear completed lesson progress and game score history
        lessonDao.clearAll()
        gameScoreDao.clearAll()
        profileDao.clearAll()
        leaderboardDao.clearAll()

        // 6. Reset game levels, achievements, stories, and notifications to initial state
        gameLevelDao.insertAll(DatabaseSeeder.getInitialGameLevels())
        achievementDao.insertAll(DatabaseSeeder.getInitialAchievements())
        storyDao.insertAll(DatabaseSeeder.getInitialStories())
        notificationDao.deleteAll()
        notificationDao.insertAll(DatabaseSeeder.getInitialNotifications())

        // 7. Ensure vocabulary dictionary has entries if it was somehow empty
        if (vocabularyDao.getTotalCountDirect() == 0) {
            vocabularyDao.insertAll(DatabaseSeeder.getInitialVocabulary())
        }
    }
}
