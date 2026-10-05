package com.kasiguru.ui.screens.settings

import com.kasiguru.ui.components.KasiGuruTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
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
import com.kasiguru.ui.screens.help.TourChapterList
import com.kasiguru.ui.tour.TourChapterId
import com.kasiguru.ui.tour.TourChapterState
import com.kasiguru.ui.tour.TourResumePoint
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.widthIn
import com.kasiguru.ui.components.tapSounds

/**
 * Settings: the same grouped-card idiom every OS settings screen uses (Account / Notifications /
 * Preferences / Sync / About), redrawn in the Lime Sheet system rather than the old Coastal one —
 * every section a `SoftCard`, over a canopy carrying just the screen's name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenAppearance: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToProfiles: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    onReplayTutorial: () -> Unit = {},
    chapterStates: Map<TourChapterId, TourChapterState> = emptyMap(),
    resumePoint: TourResumePoint? = null,
    onStartChapter: (TourChapterId) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showTutorialChapters by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val account by viewModel.account.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val tapSoundsEnabled by viewModel.tapSoundsEnabled.collectAsState()
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val sfxVolume by viewModel.sfxVolume.collectAsState()
    val musicVolume by viewModel.musicVolume.collectAsState()
    val streakReminders by viewModel.streakReminders.collectAsState()
    val wordOfDayReminders by viewModel.wordOfDayReminders.collectAsState()
    val leaderboardAlerts by viewModel.leaderboardAlerts.collectAsState()

    var reminderTime by remember { mutableStateOf("08:00 AM") }
    var showTimePicker by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }

    if (showTimePicker) {
        AlertDialog(modifier = Modifier.tapSounds(), 
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
                                colors = RadioButtonDefaults.colors(selectedColor = Lime)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = LimeText)
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
                SoftCard(
                    modifier = Modifier.fillMaxWidth().tourAnchor(TourAnchor.SettingsAccount),
                    onClick = onNavigateToAccount
                ) {
                    Text(
                        text = "Account & Sign In",
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
                                        tint = if (account.isRecoverable) GreenText else Warning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (account.isRecoverable) "Progress protected" else "Guest Mode — Not signed in",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                )
                                Text(
                                    text = account.email
                                        ?: if (account.isRecoverable) {
                                            "Signed in"
                                        } else {
                                            "Tap to sign in or create an account"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (account.isRecoverable) Muted else LimeText
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
                                color = Lime.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Profile2user),
                                        contentDescription = null,
                                        tint = LimeText,
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

                        Surface(shape = Shapes.pill, color = Lime.copy(alpha = 0.12f)) {
                            Text(
                                text = reminderTime,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LimeText
                            )
                        }
                    }
                }

                // Preferences Section
                SoftCard(modifier = Modifier.fillMaxWidth().tourAnchor(TourAnchor.SettingsPreferences)) {
                    Text(
                        text = "App Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.sm))

                    SettingActionRow(title = "Appearance", subtitle = "Theme and text size", iconRes = Iconsax.Setting,
                        onClick = onOpenAppearance, modifier = Modifier.tourAnchor(TourAnchor.SettingsAppearance))
                    Spacer(Modifier.height(Space.sm))
                    SettingSwitchRow(title = "Lesson vibrations", subtitle = "Feel answer feedback", checked = hapticsEnabled,
                        iconRes = Iconsax.FlashBold, onCheckedChange = viewModel::toggleHapticsEnabled)
                    Spacer(Modifier.height(Space.sm))
                    SettingSwitchRow(
                        title = "Sound effects",
                        subtitle = "Answers, wins and celebrations",
                        checked = soundEnabled,
                        iconRes = Iconsax.VolumeHigh,
                        onCheckedChange = { viewModel.toggleSoundEnabled(it) }
                    )
                    VolumeSliderRow(label = "Sound effects volume", percent = sfxVolume, enabled = soundEnabled,
                        onSave = viewModel::saveSfxVolume)
                    Spacer(Modifier.height(Space.sm))
                    SettingSwitchRow(title = "Tap sounds", subtitle = "A soft click on buttons and tabs", checked = tapSoundsEnabled,
                        iconRes = Iconsax.FingerTap, onCheckedChange = viewModel::toggleTapSoundsEnabled)
                    Spacer(Modifier.height(Space.sm))
                    SettingSwitchRow(title = "Background music", subtitle = "Music in menus and games", checked = musicEnabled,
                        iconRes = Iconsax.Music, onCheckedChange = viewModel::toggleMusicEnabled)
                    VolumeSliderRow(label = "Music volume", percent = musicVolume, enabled = musicEnabled,
                        onPreview = viewModel::previewMusicVolume, onSave = viewModel::saveMusicVolume)

                    Spacer(Modifier.height(Space.sm))

                    // Belongs with theme and audio rather than in a card of its own: "show me how
                    // this works again" is an app preference, and one row does not earn a section.
                    SettingActionRow(
                        modifier = Modifier.tourAnchor(TourAnchor.SettingsReplayTutorial),
                        title = "Replay tutorial",
                        subtitle = "Walk through the app again",
                        iconRes = Iconsax.Teacher,
                        onClick = onReplayTutorial
                    )
                    SettingActionRow(title = "Tutorial chapters", subtitle = "Replay a guide to any feature", iconRes = Iconsax.Book,
                        onClick = { showTutorialChapters = !showTutorialChapters })
                    if (showTutorialChapters) TourChapterList(chapterStates, resumePoint, onStartChapter)

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
                                        tint = OnLime,
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
                            color = GreenText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Recovery questions. A lightweight identity hint, not a secret - see
                // AuthRepository.saveSecurityQuestions for why this is not a password-reset gate.
                RecoveryQuestionsCard(
                    signedIn = account.isRecoverable,
                    onSignIn = onNavigateToAccount,
                    viewModel = viewModel
                )

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
                                        tint = RedText,
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
                        Surface(shape = Shapes.chip, color = Lime.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = Iconsax.InfoCircle),
                                    contentDescription = null,
                                    tint = LimeText,
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

/**
 * A settings row that performs an action rather than holding a state, laid out to match
 * [SettingSwitchRow] exactly so a card can mix the two without the rhythm breaking.
 */
@Composable
fun SettingActionRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.chip))
            .clickable(onClick = onClick)
            .heightIn(min = Touch.minTarget),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = Shapes.chip, color = Lime.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = LimeText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
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

        Icon(
            painter = painterResource(id = Iconsax.ArrowRight),
            contentDescription = null,
            tint = Faint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * How loud a sound setting plays. The switch above it is the mute; this sits indented under that
 * row's text so the two read as one setting. Steps of 5% keep TalkBack adjustments meaningful.
 */
@Composable
fun VolumeSliderRow(
    label: String,
    percent: Int,
    enabled: Boolean,
    onSave: (Int) -> Unit,
    onPreview: (Int) -> Unit = {}
) {
    // Local while dragging, so only the released value is written to disk. A saved value that
    // arrives mid-gesture (the first read on a slow start) must not yank the thumb back.
    var value by remember { mutableFloatStateOf(percent.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(percent) { if (!dragging) value = percent.toFloat() }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 40.dp + Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        Icon(painterResource(Iconsax.VolumeLow), contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
        Slider(
            value = value,
            onValueChange = {
                dragging = true
                value = it
                onPreview(it.roundToInt())
            },
            onValueChangeFinished = {
                dragging = false
                onSave(value.roundToInt())
            },
            valueRange = 0f..100f,
            steps = 19,
            enabled = enabled,
            modifier = Modifier.weight(1f).semantics { contentDescription = label },
            colors = SliderDefaults.colors(
                thumbColor = Lime,
                activeTrackColor = Lime,
                // The hairline token stays visible on the card in both themes; SurfaceSunken did not.
                inactiveTrackColor = BorderHairline,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledActiveTickColor = Color.Transparent,
                disabledInactiveTickColor = Color.Transparent
            )
        )
        Icon(painterResource(Iconsax.VolumeHigh), contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
        Text(
            text = "${value.roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) Ink else Muted,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 40.dp)
        )
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
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = Shapes.chip, color = Lime.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = LimeText,
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
                checkedTrackColor = Lime,
                uncheckedThumbColor = Surface,
                uncheckedTrackColor = SurfaceSunken
            )
        )
    }
}
