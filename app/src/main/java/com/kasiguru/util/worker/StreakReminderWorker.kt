package com.kasiguru.util.worker

import android.content.Context
import androidx.work.*
import com.kasiguru.data.local.KasiGuruDatabase
import com.kasiguru.data.local.KasiGuruMigrations
import com.kasiguru.util.notification.KasiGuruNotificationManager
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class StreakReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = androidx.room.Room.databaseBuilder(
                applicationContext,
                KasiGuruDatabase::class.java,
                "kasiguru_database"
            )
            .addMigrations(*KasiGuruMigrations.ALL)
            .build()

            val progress = db.userProgressDao().getUserProgressDirect()
            val today = LocalDate.now().toString()

            // If user hasn't been active today, post a streak reminder notification
            if (progress != null && progress.lastActiveDate != today) {
                KasiGuruNotificationManager.sendStreakReminderNotification(
                    applicationContext,
                    progress.currentStreak
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
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
