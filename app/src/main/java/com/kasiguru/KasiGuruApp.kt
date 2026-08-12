package com.kasiguru

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
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
            .addOnFailureListener { e ->
                Log.w("KasiGuruAuth", "Anonymous sign-in failed", e)
            }
    }
}
