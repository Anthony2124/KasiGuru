package com.kasiguru.util.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kasiguru.data.repository.NotificationRepository
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.data.repository.VocabularyRepository
import com.kasiguru.domain.lesson.WordOfDay
import com.kasiguru.ui.navigation.Screen
import com.kasiguru.util.notification.KasiGuruNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/**
 * The morning word of the day: the same word Home and the Library feature today, with its gloss,
 * opening on that word's page.
 *
 * Settings had a "Word of the Day" switch long before anything sent one. The word comes from the
 * corpus as recorded; nothing here writes or rewords it.
 */
class WordOfDayWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WordOfDayEntryPoint {
        fun vocabularyRepository(): VocabularyRepository
        fun userPreferencesRepository(): UserPreferencesRepository
        fun notificationRepository(): NotificationRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(applicationContext, WordOfDayEntryPoint::class.java)
            if (!entryPoint.userPreferencesRepository().wordOfDayReminders.first()) return Result.success()

            val word = WordOfDay.pick(entryPoint.vocabularyRepository().getAllVocabularyOnce())
                ?: return Result.success()
            val title = "Word of the day: ${word.kasiguranin}"
            val body = listOf(word.tagalog, word.english).filter { it.isNotBlank() }.joinToString(" · ")
            val route = Screen.VocabularyDetail.createRoute(word.id)

            KasiGuruNotificationManager.sendWordOfDayNotification(applicationContext, title, body, route)
            entryPoint.notificationRepository().addNotification(
                title = title,
                message = body,
                category = "WordOfDay",
                deepLinkRoute = route
            )
            Result.success()
        } catch (e: Exception) {
            Log.w("WordOfDayWorker", "Word of the day failed", e)
            Result.failure()
        }
    }
}
