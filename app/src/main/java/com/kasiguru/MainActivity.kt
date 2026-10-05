package com.kasiguru

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.domain.preferences.AppearanceMode
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
import com.kasiguru.util.audio.LocalSoundEffects
import com.kasiguru.util.audio.MusicPlayer
import com.kasiguru.util.audio.SoundEffects
import com.kasiguru.util.worker.StreakReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.kasiguru.ui.components.tapSounds

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firestoreSyncManager: FirestoreSyncManager

    @Inject
    lateinit var firestoreSyncRepository: FirestoreSyncRepository

    @Inject lateinit var preferences: UserPreferencesRepository

    @Inject lateinit var soundEffects: SoundEffects

    @Inject lateinit var musicPlayer: MusicPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Each screen chooses system-bar contrast against the active appearance.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // The volume keys adjust the media stream that music and sound effects play on, even when
        // nothing is playing at that moment.
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC

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
            val mode by preferences.appearanceMode.collectAsState(initial = AppearanceMode.SYSTEM)
            val textSize by preferences.textSizePercent.collectAsState(initial = 100)
            KasiGuruTheme(mode = mode, textSizePercent = textSize) {
                CompositionLocalProvider(LocalSoundEffects provides soundEffects) {
                    Box(Modifier.fillMaxSize().tapSounds()) {
                        KasiGuruNavGraph(initialDeepLink = deepLinkRoute)
                    }
                }
            }
        }
    }

    // Music stops as soon as the app loses the foreground. onStop can arrive seconds after the
    // learner has left, so pause/resume is the signal.
    override fun onResume() {
        super.onResume()
        musicPlayer.onForeground()
    }

    override fun onPause() {
        musicPlayer.onBackground()
        super.onPause()
    }
}
