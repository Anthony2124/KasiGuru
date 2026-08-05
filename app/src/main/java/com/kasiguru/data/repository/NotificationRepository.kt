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
        timestamp: String = "Just now",
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
}
