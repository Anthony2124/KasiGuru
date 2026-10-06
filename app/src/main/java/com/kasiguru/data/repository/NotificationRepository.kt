package com.kasiguru.data.repository

import com.kasiguru.data.local.dao.NotificationDao
import com.kasiguru.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun addNotification(
        title: String,
        message: String,
        category: String,
        // Stored as epoch milliseconds and shown relative to now. A literal "Just now" was stored
        // instead, and every message read "Just now" for ever.
        timestamp: String = System.currentTimeMillis().toString(),
        deepLinkRoute: String = ""
    ) {
        notificationDao.insert(
            NotificationEntity(
                title = title,
                message = message,
                category = category,
                timestamp = timestamp,
                isRead = false,
                deepLinkRoute = deepLinkRoute
            )
        )
    }

    suspend fun markAsRead(id: Int) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun clearAll() {
        notificationDao.deleteAll()
    }

    /**
     * Removes the four sample messages earlier versions put in every new inbox: a 5-day streak, a
     * Rank #2 and a badge the learner had not earned. Matched by their exact titles, so nothing a
     * learner was really sent is touched.
     */
    suspend fun removeSampleMessages() {
        notificationDao.deleteByTitles(SAMPLE_TITLES)
    }

    private companion object {
        val SAMPLE_TITLES = listOf(
            "\uD83D\uDD25 Keep Your 5-Day Streak Alive!",
            "\uD83C\uDF1F Word of the Day: Magandang Aldew",
            "\uD83C\uDFC6 Leaderboard Rank #2 Reclaimed!",
            "\uD83C\uDF93 Badge Unlocked: Linguistic Scholar!"
        )
    }
}
