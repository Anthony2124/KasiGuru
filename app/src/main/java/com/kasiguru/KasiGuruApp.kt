package com.kasiguru

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KasiGuruApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Silent anonymous sign-in gives the app a stable identity for cloud
        // submissions and future per-user data. No UI change; degrades
        // gracefully when offline (uid stays empty until sign-in succeeds).
        // Requires "Anonymous" enabled in Firebase Auth sign-in methods.
        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnSuccessListener {
                // Register the FCM token once the anonymous identity exists
                // (Phase 5 device registration; idempotent on every start).
                registerFcmToken()
            }
            .addOnFailureListener { e ->
                Log.w("KasiGuruAuth", "Anonymous sign-in failed", e)
            }
    }

    private fun registerFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                FirebaseFirestore.getInstance()
                    .collection("device_tokens")
                    .document(uid)
                    .set(
                        mapOf(
                            "token" to token,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .addOnFailureListener { e ->
                        Log.w("KasiGuruPush", "Failed to save FCM token", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.w("KasiGuruPush", "Failed to fetch FCM token", e)
            }
    }
}
