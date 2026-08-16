package com.kasiguru.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.BuildConfig
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccount: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    val account by viewModel.account.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val streakReminders by viewModel.streakReminders.collectAsState()
    val wordOfDayReminders by viewModel.wordOfDayReminders.collectAsState()
    val leaderboardAlerts by viewModel.leaderboardAlerts.collectAsState()

    var reminderTime by remember { mutableStateOf("08:00 AM") }
    var showTimePicker by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set Daily Learning Reminder", fontWeight = FontWeight.Bold, color = CoastInk) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select your preferred daily notification time:", color = CoastMuted)
                    listOf("07:00 AM", "08:00 AM", "12:00 PM", "06:00 PM", "08:00 PM", "09:00 PM").forEach { time ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    reminderTime = time
                                    showTimePicker = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = time, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CoastInk)
                            RadioButton(
                                selected = (reminderTime == time),
                                onClick = {
                                    reminderTime = time
                                    showTimePicker = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PlayPurpleStart)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = PlayPurpleStart)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "Settings & Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextHeadingBlack,
                        letterSpacing = (-0.3).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowLeft),
                            contentDescription = "Back",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoastInk,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAccount() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (account.isRecoverable) {
                                    Success.copy(alpha = 0.15f)
                                } else {
                                    Warning.copy(alpha = 0.2f)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (account.isRecoverable) Iconsax.TickCircle else Iconsax.Lock
                                        ),
                                        contentDescription = null,
                                        tint = if (account.isRecoverable) Success else Warning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (account.isRecoverable) "Progress protected" else "Secure your progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CoastInk
                                )
                                Text(
                                    text = account.email
                                        ?: if (account.isRecoverable) {
                                            "Signed in"
                                        } else {
                                            "Guest — progress is only on this device"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoastMuted
                                )
                            }
                        }
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowRight),
                            contentDescription = null,
                            tint = CoastMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Notifications Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Smart Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoastInk,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        title = "Streak Protection Reminders",
                        subtitle = "Notify me before losing my streak",
                        checked = streakReminders,
                        iconRes = Iconsax.Flash,
                        onCheckedChange = { viewModel.toggleStreakReminders(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Word of the Day",
                        subtitle = "Daily Kasiguranin phrase highlight",
                        checked = wordOfDayReminders,
                        iconRes = Iconsax.Book,
                        onCheckedChange = { viewModel.toggleWordOfDayReminders(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Leaderboard Rank Alerts",
                        subtitle = "Alert me when my rank changes",
                        checked = leaderboardAlerts,
                        iconRes = Iconsax.MedalStar,
                        onCheckedChange = { viewModel.toggleLeaderboardAlerts(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Reminder Time",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CoastInk
                            )
                            Text(
                                text = "Scheduled at $reminderTime every day",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoastMuted
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = PlayPurpleStart.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = reminderTime,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PlayPurpleStart
                            )
                        }
                    }
                }
            }

            // Preferences Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "App Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoastInk,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        title = "Dark Theme",
                        subtitle = "Enable sleek dark mode",
                        checked = isDarkMode,
                        iconRes = Iconsax.Moon,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Audio Pronunciation",
                        subtitle = "Play Kasiguranin voice audio",
                        checked = soundEnabled,
                        iconRes = Iconsax.VolumeHigh,
                        onCheckedChange = { viewModel.toggleSoundEnabled(it) }
                    )
                }
            }

            // Sync Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Data Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoastInk,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sync Now",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CoastInk
                            )
                            Text(
                                text = "Merge this device with your saved cloud progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoastMuted
                            )
                        }

                        Button(
                            onClick = {
                                isSyncing = true
                                scope.launch {
                                    val synced = viewModel.syncNow()
                                    isSyncing = false
                                    syncMessage = if (synced) {
                                        "Progress synced with your account."
                                    } else {
                                        "Not signed in yet — sync unavailable."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PlayPurpleStart),
                            shape = RoundedCornerShape(999.dp),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = Iconsax.Refresh),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (syncMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Success,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // About Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PlayPurpleStart.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = Iconsax.InfoCircle),
                                    contentDescription = null,
                                    tint = PlayPurpleStart,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "KasiGuru v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = CoastInk
                            )
                            Text(
                                text = "Installed build ${BuildConfig.VERSION_CODE}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoastMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    iconRes: Int,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PlayPurpleStart.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = PlayPurpleStart,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CoastInk
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoastMuted
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PlayPurpleStart,
                uncheckedThumbColor = CoastMuted,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
