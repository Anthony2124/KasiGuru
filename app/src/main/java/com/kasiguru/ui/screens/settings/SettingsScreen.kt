package com.kasiguru.ui.screens.settings

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
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import kotlinx.coroutines.launch

/**
 * Settings: the same grouped-card idiom every OS settings screen uses (Account / Notifications /
 * Preferences / Sync / About), redrawn in the Violet Sheet system rather than the old Coastal one —
 * every section a `SoftCard`, over a canopy carrying just the screen's name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccount: () -> Unit = {},
    onNavigateToProfiles: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
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
            title = { Text("Set Daily Learning Reminder", fontWeight = FontWeight.Bold, color = Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text("Select your preferred daily notification time:", color = Muted)
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
                            Text(text = time, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                            RadioButton(
                                selected = (reminderTime == time),
                                onClick = {
                                    reminderTime = time
                                    showTimePicker = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Violet)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Violet)
                }
            }
        )
    }

    GroundScaffold(
        title = "Settings",
        subtitle = "Notifications, sync, and app preferences",
        onBack = onNavigateBack,
        // A settings list is rows of text; colour fields behind them would fight the reading.
        pattern = GroundPattern.Grid,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.gutter)
                    .padding(bottom = Space.navBarClearance),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                GroundTitleBlock(
                    title = "Settings",
                    subtitle = "Notifications, sync, and app preferences"
                )

                // Account Section
                SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToAccount) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(Space.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = Shapes.chip,
                                color = if (account.isRecoverable) Green.copy(alpha = 0.15f) else Warning.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (account.isRecoverable) Iconsax.TickCircle else Iconsax.Lock
                                        ),
                                        contentDescription = null,
                                        tint = if (account.isRecoverable) Green else Warning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (account.isRecoverable) "Progress protected" else "Secure your progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                )
                                Text(
                                    text = account.email
                                        ?: if (account.isRecoverable) {
                                            "Signed in"
                                        } else {
                                            "Guest — progress is only on this device"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted
                                )
                            }
                        }
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowRight),
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Profiles Section
                SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToProfiles) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(Space.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = Shapes.chip,
                                color = Violet.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Profile2user),
                                        contentDescription = null,
                                        tint = Violet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Manage profiles",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                )
                                Text(
                                    text = "Add a family member or switch who's learning",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted
                                )
                            }
                        }
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowRight),
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Notifications Section
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Smart Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.sm))

                    SettingSwitchRow(
                        title = "Streak Protection Reminders",
                        subtitle = "Notify me before losing my streak",
                        checked = streakReminders,
                        iconRes = Iconsax.Flash,
                        onCheckedChange = { viewModel.toggleStreakReminders(it) }
                    )

                    Spacer(Modifier.height(Space.sm))

                    SettingSwitchRow(
                        title = "Word of the Day",
                        subtitle = "Daily Kasiguranin phrase highlight",
                        checked = wordOfDayReminders,
                        iconRes = Iconsax.Book,
                        onCheckedChange = { viewModel.toggleWordOfDayReminders(it) }
                    )

                    Spacer(Modifier.height(Space.sm))

                    SettingSwitchRow(
                        title = "Leaderboard Rank Alerts",
                        subtitle = "Alert me when my rank changes",
                        checked = leaderboardAlerts,
                        iconRes = Iconsax.MedalStar,
                        onCheckedChange = { viewModel.toggleLeaderboardAlerts(it) }
                    )

                    Spacer(Modifier.height(Space.sm))
                    HorizontalDivider(color = Faint.copy(alpha = 0.3f))
                    Spacer(Modifier.height(Space.sm))

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
                                color = Ink
                            )
                            Text(
                                text = "Scheduled at $reminderTime every day",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }

                        Surface(shape = Shapes.pill, color = Violet.copy(alpha = 0.12f)) {
                            Text(
                                text = reminderTime,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Violet
                            )
                        }
                    }
                }

                // Preferences Section
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "App Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.sm))

                    SettingSwitchRow(
                        title = "Dark Theme",
                        subtitle = "Enable sleek dark mode",
                        checked = isDarkMode,
                        iconRes = Iconsax.Moon,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )

                    Spacer(Modifier.height(Space.sm))

                    SettingSwitchRow(
                        title = "Audio Pronunciation",
                        subtitle = "Play Kasiguranin voice audio",
                        checked = soundEnabled,
                        iconRes = Iconsax.VolumeHigh,
                        onCheckedChange = { viewModel.toggleSoundEnabled(it) }
                    )
                }

                // Sync Section
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Data Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.sm))

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
                                color = Ink
                            )
                            Text(
                                text = "Merge this device with your saved cloud progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }
                        Spacer(Modifier.width(Space.sm))

                        ClayButton(
                            modifier = Modifier.width(IntrinsicSize.Min),
                            label = if (isSyncing) "Syncing…" else "Sync",
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
                            enabled = !isSyncing,
                            leading = if (!isSyncing) {
                                {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Refresh),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }

                    if (syncMessage.isNotEmpty()) {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Green,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Recovery questions. A lightweight identity hint, not a secret - see
                // AuthRepository.saveSecurityQuestions for why this is not a password-reset gate.
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    val securityAnswers by viewModel.securityAnswers.collectAsState()
                    val securityStatus by viewModel.securityQuestionsStatus.collectAsState()
                    LaunchedEffect(Unit) { viewModel.loadSecurityQuestions() }

                    Text(
                        text = "Recovery Questions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = "A lightweight hint to help confirm it's you, not a secret. " +
                            "Anyone who unlocks this device can read them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    Spacer(Modifier.height(Space.sm))

                    viewModel.securityQuestions.forEachIndexed { index, question ->
                        OutlinedTextField(
                            value = securityAnswers.getOrElse(index) { "" },
                            onValueChange = { viewModel.onSecurityAnswerChanged(index, it) },
                            label = { Text(question) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = Space.xs),
                            singleLine = true
                        )
                    }

                    ClayButton(
                        label = "Save answers",
                        onClick = { viewModel.saveSecurityQuestions() },
                        tone = ClayButtonTone.Quiet,
                        modifier = Modifier.fillMaxWidth()
                    )

                    securityStatus?.let { message ->
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                    }
                }

                // Support & Feedback Section
                SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToReport) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(Space.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = Shapes.chip,
                                color = Red.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.InfoCircle),
                                        contentDescription = null,
                                        tint = Red,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Report Bug or Wrong Word",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                )
                                Text(
                                    text = "Submit a glitch, wrong translation, or attach photo evidence",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted
                                )
                            }
                        }
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowRight),
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // About Info Card
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        Surface(shape = Shapes.chip, color = Violet.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = Iconsax.InfoCircle),
                                    contentDescription = null,
                                    tint = Violet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "KasiGuru v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Ink
                            )
                            Text(
                                text = "Installed build ${BuildConfig.VERSION_CODE}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Space.navBarClearance))
            }
        }
    )
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
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = Shapes.chip, color = Violet.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Violet,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Violet,
                uncheckedThumbColor = Surface,
                uncheckedTrackColor = SurfaceSunken
            )
        )
    }
}
