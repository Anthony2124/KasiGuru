package com.kasiguru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.kasiguru.data.remote.FirestoreSyncManager
import com.kasiguru.data.repository.FirestoreSyncRepository
import com.kasiguru.ui.navigation.KasiGuruNavGraph
import com.kasiguru.ui.theme.KasiGuruTheme
import com.kasiguru.util.worker.StreakReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firestoreSyncManager: FirestoreSyncManager

    @Inject
    lateinit var firestoreSyncRepository: FirestoreSyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // The app is dark only, so the system bars always get light icons, whatever the phone's theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Deep link target from a notification tap (Phase 5).
        val deepLinkRoute = intent?.getStringExtra("deep_link_route")

        // Notification permission is not asked for here: onboarding's reminders step asks when the
        // learner says yes to reminders, and Settings asks when one is turned on.

        // Schedule daily background streak reminder notification
        StreakReminderWorker.scheduleDailyReminder(applicationContext)

        // 1. Initial one-shot sync with Firestore
        lifecycleScope.launch {
            firestoreSyncManager.syncWithFirestore()
        }

        // 2. Start continuous real-time listener (Admin additions/approvals -> App Room DB)
        firestoreSyncRepository.startRealtimeSync()

        setContent {
            KasiGuruTheme {
                KasiGuruNavGraph(initialDeepLink = deepLinkRoute)
            }
        }
    }
}
