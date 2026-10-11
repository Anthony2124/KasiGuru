package com.kasiguru.util.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kasiguru.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Puts the daily reminders at the times they are meant for.
 *
 * The streak reminder used to be a 24-hour job first run 12 hours after the app was opened, so it
 * arrived at whatever hour that happened to be, and the time picked in Settings was never read.
 * Each reminder is now a daily job anchored to its time of day, re-anchored on every app start and
 * every Settings change so it cannot wander, and cancelled outright when its switch is off.
 */
object ReminderScheduler {

    private const val STREAK_WORK = "kasiguru_streak_reminder"
    private const val WORD_OF_DAY_WORK = "kasiguru_word_of_day_reminder"
    private const val COMMUNITY_WORK = "kasiguru_community_updates"

    /** How often the background check for word and dictionary news runs. */
    private const val COMMUNITY_INTERVAL_HOURS = 6L

    /** The old randomly-timed job, removed wherever it is still queued. */
    private const val LEGACY_STREAK_WORK = "kasiguru_daily_streak_reminder"

    /** The word of the day goes out at 8:00 AM, to be read with the day ahead. */
    const val WORD_OF_DAY_MINUTE = 8 * 60

    /** Time from [now] to the next [minuteOfDay] (minutes after midnight), always in the future. */
    fun delayUntil(minuteOfDay: Int, now: LocalDateTime = LocalDateTime.now()): Duration {
        val todayAt = now.toLocalDate().atStartOfDay().plusMinutes(minuteOfDay.toLong())
        val next = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
        return Duration.between(now, next)
    }

    /** Reads the learner's reminder settings and schedules (or cancels) both reminders to match. */
    suspend fun syncWithPreferences(context: Context, preferences: UserPreferencesRepository) {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_STREAK_WORK)
        scheduleStreak(context, preferences.streakReminders.first(), preferences.reminderMinuteOfDay.first())
        scheduleWordOfDay(context, preferences.wordOfDayReminders.first())
        scheduleCommunityUpdates(context)
    }

    /**
     * The check for news about the learner's words and the dictionary, every
     * [COMMUNITY_INTERVAL_HOURS] hours when there is a connection. Kept, not re-queued, on each
     * start, so opening the app does not keep pushing the next check back. Learners silence it
     * through its own notification channel.
     */
    fun scheduleCommunityUpdates(context: Context) {
        val request = PeriodicWorkRequestBuilder<CommunityUpdatesWorker>(COMMUNITY_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(COMMUNITY_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun scheduleStreak(context: Context, enabled: Boolean, minuteOfDay: Int) =
        schedule<StreakReminderWorker>(context, STREAK_WORK, enabled, minuteOfDay)

    fun scheduleWordOfDay(context: Context, enabled: Boolean) =
        schedule<WordOfDayWorker>(context, WORD_OF_DAY_WORK, enabled, WORD_OF_DAY_MINUTE)

    private inline fun <reified W : androidx.work.ListenableWorker> schedule(
        context: Context,
        name: String,
        enabled: Boolean,
        minuteOfDay: Int
    ) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(name)
            return
        }
        val request = PeriodicWorkRequestBuilder<W>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(minuteOfDay).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }
}
