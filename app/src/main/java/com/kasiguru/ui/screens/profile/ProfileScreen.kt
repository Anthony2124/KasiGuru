package com.kasiguru.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.ui.components.CasiguranAvatarPortrait
import com.kasiguru.ui.components.CasiguranResident
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PlayPurpleStart)
        }
        return
    }

    val progress = uiState.userProgress ?: com.kasiguru.data.local.entity.UserProgressEntity()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "My Profile",
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
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            painter = painterResource(id = Iconsax.Setting),
                            contentDescription = "Settings",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(
                            painter = painterResource(id = Iconsax.Edit),
                            contentDescription = "Edit Profile",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(22.dp)
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
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Banner
            CasiguranAvatarPortrait(
                resident = CasiguranResident.TEACHER,
                size = 96.dp,
                level = progress.level,
                onClick = onNavigateToEditProfile
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (progress.fullName.isNotEmpty()) progress.fullName else progress.userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = CoastInk,
                letterSpacing = (-0.4).sp
            )

            Text(
                text = "@${progress.userName.lowercase().replace(" ", "_")}",
                style = MaterialTheme.typography.bodyMedium,
                color = CoastMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

            // User Info Card
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
                        text = "Personal Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoastInk,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    ProfileInfoRow(
                        iconRes = Iconsax.Sms,
                        label = "Email",
                        value = if (progress.email.isNotEmpty()) progress.email else "kasiguranin.learner@gmail.com"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileInfoRow(
                        iconRes = Iconsax.Calendar,
                        label = "Age",
                        value = if (progress.age != null) "${progress.age} years old" else "Not set"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileInfoRow(
                        iconRes = Iconsax.Location,
                        label = "Address",
                        value = if (progress.address.isNotEmpty()) progress.address else "Casiguran, Aurora"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat Cards (Side by Side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val levelInfo = com.kasiguru.util.gamification.GamificationEngine.getLevelInfo(progress.totalXp)
                StatCard(
                    modifier = Modifier.weight(1f).clickable { onNavigateToAchievements() },
                    title = "Level ${levelInfo.level}",
                    value = "Lv. ${levelInfo.level}",
                    subtitle = levelInfo.title,
                    gradient = listOf(PlayPurpleStart, PlayPurpleEnd),
                    iconRes = Iconsax.MedalStar
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total XP",
                    value = "${progress.totalXp}",
                    subtitle = "Points Earned",
                    gradient = listOf(PlayGoldStart, PlayGoldEnd),
                    iconRes = Iconsax.Cup
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Learning Stats Detail
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
                        text = "Learning Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoastInk,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    StatDetailRow("Words Mastered", "${progress.wordsLearned} / 487", iconRes = Iconsax.BookBold)
                    StatDetailRow("Current Streak", "${progress.currentStreak} Days", iconRes = Iconsax.FlashBold)
                    StatDetailRow("Longest Streak", "${progress.longestStreak} Days", iconRes = Iconsax.Medal)
                    StatDetailRow("Games Played", "${progress.gamesPlayed}", iconRes = Iconsax.Game)
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(iconRes: Int, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PlayPurpleStart.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = PlayPurpleStart,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = CoastMuted)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = CoastInk)
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    gradient: List<Color>,
    iconRes: Int
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradient))
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.88f), fontWeight = FontWeight.Bold)
                Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = (-0.3).sp)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StatDetailRow(label: String, value: String, iconRes: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = CoastMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = CoastMuted)
        }
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = CoastInk)
    }
}
