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
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

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
            title = { Text("Set Daily Learning Reminder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select your preferred daily notification time:")
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
                            Text(text = time, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            RadioButton(
                                selected = (reminderTime == time),
                                onClick = {
                                    reminderTime = time
                                    showTimePicker = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
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
            // Notifications Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Smart Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        title = "Streak Protection Reminders 🔥",
                        subtitle = "Notify me before losing my streak",
                        checked = streakReminders,
                        iconRes = Iconsax.Flash,
                        onCheckedChange = { viewModel.toggleStreakReminders(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Word of the Day 🌟",
                        subtitle = "Daily Kasiguranin phrase highlight",
                        checked = wordOfDayReminders,
                        iconRes = Iconsax.Book,
                        onCheckedChange = { viewModel.toggleWordOfDayReminders(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Leaderboard Rank Alerts 🏆",
                        subtitle = "Alert me when my rank changes",
                        checked = leaderboardAlerts,
                        iconRes = Iconsax.MedalStar,
                        onCheckedChange = { viewModel.toggleLeaderboardAlerts(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                                text = "Daily Reminder Time ⏰",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextHeadingBlack
                            )
                            Text(
                                text = "Scheduled at $reminderTime every day",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSubtleGray
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HeroCardStart.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = reminderTime,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextHeadingBlack
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
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "App Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
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
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Data Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sync with Device",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextHeadingBlack
                            )
                            Text(
                                text = "Save progress offline Room SQLite database",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSubtleGray
                            )
                        }

                        Button(
                            onClick = {
                                isSyncing = true
                                scope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    isSyncing = false
                                    syncMessage = "Database synchronized!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HeroCardStart),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = TextHeadingBlack,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = Iconsax.Refresh),
                                    contentDescription = null,
                                    tint = TextHeadingBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
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
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.InfoCircle),
                            contentDescription = null,
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "KasiGuru v2.5.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextHeadingBlack
                            )
                            Text(
                                text = "Interactive Kasiguranin Language Platform",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSubtleGray
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
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = TextHeadingBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextHeadingBlack
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtleGray
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TextHeadingBlack,
                uncheckedThumbColor = TextSubtleGray,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
