package com.kasiguru.util.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kasiguru.data.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives Firebase Cloud Messaging pushes (Phase 5 — Android integration).
 *
 * - Saves every message to the local notification inbox.
 * - Posts a system notification whose tap opens the app at the deep-link route.
 * - Registers/updates the FCM token in Firestore (device_tokens/{uid}) so the
 *   send_push.js script (or any backend) can target this device.
 */
@AndroidEntryPoint
class KasiGuruMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "KasiGuru"
        val body = message.notification?.body ?: data["message"] ?: data["body"] ?: ""
        val route = data["route"] ?: ""
        val category = data["category"] ?: "General"

        // Persist to the local inbox (survives restarts, shown in the Notifications tab).
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationRepository.addNotification(
                    title = title,
                    message = body,
                    category = category,
                    deepLinkRoute = route
                )
            } catch (e: Exception) {
                Log.w("KasiGuruPush", "Failed to save notification to inbox", e)
            }
        }

        KasiGuruNotificationManager.sendNotification(
            context = applicationContext,
            title = title,
            message = body,
            deepLinkRoute = route
        )
    }

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            FirebaseFirestore.getInstance()
                .collection("device_tokens")
                .document(uid)
                .set(
                    mapOf(
                        "token" to token,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
        } catch (e: Exception) {
            Log.w("KasiGuruPush", "Failed to register FCM token", e)
        }
    }
}
