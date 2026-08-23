package com.kasiguru.util.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.kasiguru.data.local.dao.UserProgressDao
import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.util.notification.KasiGuruNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class StreakReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    /**
     * WorkManager constructs this worker itself, so it never passes through Hilt's
     * injection. An entry point is how a Hilt-managed singleton is reached from that
     * position — cheaper than adding hilt-work, which would also mean a HiltWorkerFactory
     * and a Configuration.Provider on [com.kasiguru.KasiGuruApp].
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StreakReminderEntryPoint {
        fun userProgressDao(): UserProgressDao

        /** For naming what is waiting: a reminder that says how much is due is one worth reading. */
        fun vocabularyDao(): VocabularyDao
    }

    override suspend fun doWork(): Result {
        return try {
            // Previously this called Room.databaseBuilder(...) directly, which returns a
            // *new* database instance rather than the one DatabaseModule already provides.
            // Two instances open two connection pools on the same kasiguru_database file,
            // each able to run the migration chain independently — so a worker firing while
            // the app cold-starts after an upgrade had both racing the same migrations. The
            // worker's copy also lacked DatabaseModule's onCreate seeding callback, so it
            // could create an unseeded database if it happened to get there first.
            val entryPoint = EntryPointAccessors
                .fromApplication(applicationContext, StreakReminderEntryPoint::class.java)
            val dao = entryPoint.userProgressDao()

            val progress = dao.getUserProgressDirect()
            val today = LocalDate.now().toString()

            // If the learner has not practised today, post a streak reminder naming what is due.
            if (progress != null && progress.lastActiveDate != today) {
                val dueCount = entryPoint.vocabularyDao().countScheduledDueWords(today)
                KasiGuruNotificationManager.sendStreakReminderNotification(
                    applicationContext,
                    progress.currentStreak,
                    dueCount
                )
            }
            Result.success()
        } catch (e: Exception) {
            // Was swallowed entirely: a worker failing every night looked identical to one
            // that never ran, with nothing in logcat either way.
            Log.w(TAG, "Streak reminder failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "StreakReminderWorker"
        const val WORK_NAME = "kasiguru_daily_streak_reminder"

        fun scheduleDailyReminder(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<StreakReminderWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(12, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )
        }
    }
}
