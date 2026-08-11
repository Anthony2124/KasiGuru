package com.kasiguru

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.remote.FirestoreSyncManager
import com.kasiguru.data.repository.AppUpdateRepository
import com.kasiguru.data.repository.FirestoreSyncRepository
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.ui.navigation.KasiGuruNavGraph
import com.kasiguru.ui.theme.KasiGuruTheme
import com.kasiguru.util.worker.StreakReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firestoreSyncManager: FirestoreSyncManager

    @Inject
    lateinit var firestoreSyncRepository: FirestoreSyncRepository

    @Inject
    lateinit var vocabularyDao: VocabularyDao

    @Inject
    lateinit var appUpdateRepository: AppUpdateRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13+: request notification permission so streak reminders work.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Schedule daily background streak reminder notification
        StreakReminderWorker.scheduleDailyReminder(applicationContext)

        // 1. Initial cleanup of any existing duplicate entries
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                vocabularyDao.deleteDuplicateWords()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Initial one-shot sync with Firestore
        lifecycleScope.launch {
            firestoreSyncManager.syncWithFirestore()
        }

        // 3. Start continuous real-time listener (Admin additions/approvals -> App Room DB)
        firestoreSyncRepository.startRealtimeSync()

        // 3. Check for published APK release updates from Admin panel
        lifecycleScope.launch {
            try {
                val currentVersionCode = 1
                val result = appUpdateRepository.getLatestRelease()
                val latest = result.getOrNull()
                if (latest != null && latest.versionCode > currentVersionCode) {
                    android.util.Log.d("KasiGuruUpdate", "New release available: v${latest.versionName} (${latest.apkUrl})")
                }
            } catch (e: Exception) {
                android.util.Log.w("KasiGuruUpdate", "Update check failed", e)
            }
        }

        setContent {
            val isDarkMode by userPreferencesRepository.isDarkMode.collectAsState(initial = false)
            KasiGuruTheme(darkTheme = isDarkMode) {
                KasiGuruNavGraph()
            }
        }
    }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied; reminders degrade silently */ }
}
