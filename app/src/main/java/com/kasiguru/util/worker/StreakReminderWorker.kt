package com.kasiguru.util.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kasiguru.data.repository.NotificationRepository
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.data.repository.UserProgressRepository
import com.kasiguru.domain.gamification.StreakRules
import com.kasiguru.util.notification.KasiGuruNotificationManager
import com.kasiguru.util.notification.StreakReminderCopy
import com.kasiguru.util.toIsoString
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * The evening streak reminder, at the time chosen in Settings (see [ReminderScheduler]).
 *
 * It stays quiet when the switch is off or today's streak is already safe, and otherwise says
 * exactly what is left of today's quota.
 */
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
        fun userProgressRepository(): UserProgressRepository
        fun userPreferencesRepository(): UserPreferencesRepository
        fun notificationRepository(): NotificationRepository
    }

    override suspend fun doWork(): Result {
        return try {
            // Reached through the app's singletons rather than a database of the worker's own: a
            // second Room instance raced the app's migrations when both started together.
            val entryPoint = EntryPointAccessors
                .fromApplication(applicationContext, StreakReminderEntryPoint::class.java)

            // The switch in Settings was never read here, so turning reminders off did nothing.
            if (!entryPoint.userPreferencesRepository().streakReminders.first()) return Result.success()

            val progressRepository = entryPoint.userProgressRepository()
            val progress = progressRepository.getUserProgressOnce() ?: return Result.success()
            val today = LocalDate.now()
            val todayIso = today.toIsoString()
            if (progress.lastActiveDate == todayIso) return Result.success() // already safe today

            val quota = progressRepository.getDailyStreakQuotaOnce(todayIso)
            if (quota.isQuotaMet) return Result.success()

            // A run already broken yesterday is not one this reminder can save.
            val streak = if (StreakRules.isExpired(progress.currentStreak, progress.lastActiveDate, today)) 0
                else progress.currentStreak
            val title = StreakReminderCopy.title(streak)
            val body = StreakReminderCopy.body(
                currentStreak = streak,
                dueCount = if (quota.reviewDue) progressRepository.dueReviewCount(todayIso) else 0,
                reviewDone = quota.reviewCompleted,
                gamesLeft = quota.gamesRemaining,
                requiredGames = quota.requiredGames
            )

            KasiGuruNotificationManager.sendStreakReminderNotification(applicationContext, title, body)
            entryPoint.notificationRepository().addNotification(
                title = title,
                message = body,
                category = "Streak",
                deepLinkRoute = "streak"
            )
            Result.success()
        } catch (e: Exception) {
            // A worker failing every night must not look identical to one that never ran.
            Log.w(TAG, "Streak reminder failed", e)
            Result.failure()
        }
    }

    private companion object {
        const val TAG = "StreakReminderWorker"
    }
}
