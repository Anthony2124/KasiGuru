package com.kasiguru.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.R
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HeroCardStart)
        }
        return
    }

    val progress = uiState.userProgress ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = "Back",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_setting_outline),
                            contentDescription = "Settings",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit_outline),
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Banner
            Surface(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape),
                color = HeroCardStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(HeroCardEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_profile_outline),
                        contentDescription = "Profile Avatar",
                        tint = TextHeadingBlack,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (progress.fullName.isNotEmpty()) progress.fullName else progress.userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeadingBlack
            )

            Text(
                text = "@${progress.userName.lowercase().replace(" ", "_")}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSubtleGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // User Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Personal Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    ProfileInfoRow(
                        iconRes = R.drawable.ic_document_outline,
                        label = "Email",
                        value = if (progress.email.isNotEmpty()) progress.email else "kasiguranin.learner@gmail.com"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileInfoRow(
                        iconRes = R.drawable.ic_calendar_outline,
                        label = "Age",
                        value = if (progress.age != null) "${progress.age} years old" else "Not set"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileInfoRow(
                        iconRes = R.drawable.ic_global_outline,
                        label = "Address",
                        value = if (progress.address.isNotEmpty()) progress.address else "Casiguran, Aurora"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stat Cards (Side by Side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Level",
                    value = "${progress.level}",
                    subtitle = "Explorer",
                    bgColor = StoriesCardStart,
                    iconRes = R.drawable.ic_medal_outline
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total XP",
                    value = "${progress.totalXp}",
                    subtitle = "Points Earned",
                    bgColor = VocabCardStart,
                    iconRes = R.drawable.ic_cup_outline
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Learning Stats Detail
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Learning Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    StatDetailRow("Words Mastered", "${progress.wordsLearned} / 487")
                    StatDetailRow("Current Streak", "${progress.currentStreak} Days 🔥")
                    StatDetailRow("Longest Streak", "${progress.longestStreak} Days")
                    StatDetailRow("Games Played", "${progress.gamesPlayed}")
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
                .background(QuestsCardStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = TextHeadingBlack,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSubtleGray)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextHeadingBlack)
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    bgColor: androidx.compose.ui.graphics.Color,
    iconRes: Int
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = TextHeadingBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextHeadingBlack.copy(alpha = 0.8f))
            Text(text = value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = TextHeadingBlack)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextHeadingBlack.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun StatDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSubtleGray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextHeadingBlack)
    }
}
