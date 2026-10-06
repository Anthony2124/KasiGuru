package com.kasiguru.util.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kasiguru.MainActivity
import com.kasiguru.R

/**
 * Posts every system notification the app makes.
 *
 * Three channels, so a learner can silence one kind in Android's settings and keep the others:
 * the streak reminder, the word of the day, and news pushed by the team. Everything used to share
 * the streak channel, so muting pushes also muted the streak.
 */
object KasiGuruNotificationManager {

    enum class Channel(val id: String, val title: String, val description: String) {
        // The id is the one the old single channel had, so a learner's existing setting carries over.
        Streak("kasiguru_streak_channel", "Streak reminders", "A reminder in the evening when today's streak isn't safe yet."),
        WordOfDay("kasiguru_word_of_day_channel", "Word of the day", "One Kasiguranin word each morning."),
        News("kasiguru_news_channel", "News and updates", "Announcements and messages from the KasiGuru team.")
    }

    private const val STREAK_NOTIFICATION_ID = 1001
    private const val WORD_OF_DAY_NOTIFICATION_ID = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            Channel.entries.forEach { channel ->
                manager.createNotificationChannel(
                    NotificationChannel(channel.id, channel.title, NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = channel.description
                    }
                )
            }
        }
    }

    /** Tonight's streak reminder. One at a time: a new one replaces yesterday's if it is still there. */
    fun sendStreakReminderNotification(context: Context, title: String, body: String) =
        post(context, Channel.Streak, STREAK_NOTIFICATION_ID, title, body, deepLinkRoute = "streak")

    fun sendWordOfDayNotification(context: Context, title: String, body: String, deepLinkRoute: String) =
        post(context, Channel.WordOfDay, WORD_OF_DAY_NOTIFICATION_ID, title, body, deepLinkRoute)

    /** A push from the team. Each gets its own id, so a second push does not replace the first. */
    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        deepLinkRoute: String = ""
    ) = post(context, Channel.News, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), title, message, deepLinkRoute)

    private fun post(
        context: Context,
        channel: Channel,
        notificationId: Int,
        title: String,
        body: String,
        deepLinkRoute: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (deepLinkRoute.isNotBlank()) putExtra("deep_link_route", deepLinkRoute)
        }
        // The request code is the notification's own id. With one shared code and FLAG_UPDATE_CURRENT,
        // every notification's tap carried the extras of the newest one, so an older push opened
        // the wrong screen.
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_notification_outline)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}
