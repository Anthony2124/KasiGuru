package com.kasiguru.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.kasiguru.R
import com.kasiguru.ui.components.AppUpdateBanner
import com.kasiguru.ui.components.SecureProgressBanner
import com.kasiguru.ui.components.StreakDialog
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToStories: () -> Unit,
    onNavigateToVocabulary: () -> Unit,
    onNavigateToGames: () -> Unit,
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToCultural: () -> Unit = {},
    onNavigateToFlashcards: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToSubmitWord: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showStreakDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val progress = uiState.userProgress ?: com.kasiguru.data.local.entity.UserProgressEntity()

    if (showStreakDialog) {
        StreakDialog(
            currentStreak = progress.currentStreak,
            longestStreak = progress.longestStreak,
            onDismiss = { showStreakDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ─── Update banner (shown when a newer release is published) ───
        uiState.updateRelease?.let { release ->
            AppUpdateBanner(
                release = release,
                onDismiss = viewModel::dismissUpdate
            )
        }

        // ─── Guest progress warning (anonymous session, real progress at risk) ───
        if (uiState.showBackupPrompt) {
            SecureProgressBanner(
                onSecure = onNavigateToAccount,
                onDismiss = viewModel::dismissBackupPrompt
            )
        }

        // ─── 1. Top Header Bar ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.clickable { onNavigateToProfile() }
            ) {
                com.kasiguru.ui.components.CasiguranAvatarPortrait(
                    resident = com.kasiguru.ui.components.CasiguranResident.TEACHER,
                    size = 54.dp,
                    level = progress.level,
                    onClick = { onNavigateToProfile() }
                )

                Column {
                    Text(
                        text = "Magandang Aldew,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSubtleGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (progress.fullName.isNotEmpty()) progress.fullName else progress.userName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextHeadingBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Streak Flame Badge Button
                Surface(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { showStreakDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    color = PlayGoldStart,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.Flash),
                            contentDescription = "Streak",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${progress.currentStreak}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = TextHeadingBlack
                        )
                    }
                }

                // Badges Button
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateToAchievements() },
                    shape = CircleShape,
                    color = PlayPurpleStart,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = Iconsax.MedalStar),
                            contentDescription = "Badges",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Circular Notification Bell Button
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateToNotifications() },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = Iconsax.Notification),
                            contentDescription = "Notifications",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ─── 2. Hero Banner Card ───
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clickable { onNavigateToFlashcards() },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(HeroCardStart, HeroCardEnd)
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_student),
                    contentDescription = "Kasiguranin basics student",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(180.dp)
                        .padding(top = 10.dp, end = 10.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "GET STARTED",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                    }

                    Column {
                        Text(
                            text = "Kasiguranin basics",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TextHeadingBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Learn essential phrases\n& words",
                            fontSize = 14.sp,
                            color = TextHeadingBlack.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Start Learning",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextHeadingBlack
                            )
                            Icon(
                                painter = painterResource(id = Iconsax.ArrowRight),
                                contentDescription = null,
                                tint = TextHeadingBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ─── 2.5 Contribute Kasiguranin Word Banner ───
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSubmitWord() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "COMMUNITY CONTRIBUTIONS",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Know a Kasiguranin Word?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Submit new entries to our linguistic review queue to help expand the corpus!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.Add),
                        contentDescription = "Add Word",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ─── 2.5 Interactive Learning Path Map (Moodboard 1 & 5) ───
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Learning Journey Map",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                        Text(
                            text = "Level ${progress.level} • Kasiguranin Path",
                            fontSize = 12.sp,
                            color = TextSubtleGray
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = NudgeFlame.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "🎯 Daily Goal: Active",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NudgeFlame
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Curved Node Map Checkpoints
                com.kasiguru.ui.components.LearningPathNode(
                    title = "Basic Greetings",
                    categoryName = "Everyday Phrases",
                    nodeState = com.kasiguru.ui.components.NodeState.COMPLETED,
                    horizontalOffset = 0,
                    onClick = { onNavigateToVocabulary() }
                )
                com.kasiguru.ui.components.LearningPathNode(
                    title = "Animals & Nature",
                    categoryName = "Vocabulary",
                    nodeState = com.kasiguru.ui.components.NodeState.ACTIVE,
                    horizontalOffset = 36,
                    onClick = { onNavigateToVocabulary() }
                )
                com.kasiguru.ui.components.LearningPathNode(
                    title = "Folklore Stories",
                    categoryName = "Narrative",
                    nodeState = com.kasiguru.ui.components.NodeState.LOCKED,
                    horizontalOffset = -30,
                    onClick = { onNavigateToStories() }
                )
                com.kasiguru.ui.components.LearningPathNode(
                    title = "Agta Heritage",
                    categoryName = "Culture",
                    nodeState = com.kasiguru.ui.components.NodeState.LOCKED,
                    horizontalOffset = 20,
                    onClick = { onNavigateToCultural() }
                )
            }
        }

        // ─── 3. Bento Grid Section (Vocabulary + Stories + Mini Games) ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Vocabulary Tall Card (Play Gold)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onNavigateToVocabulary() },
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(PlayGoldStart, PlayGoldEnd)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = "Vocabulary",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = TextHeadingBlack
                            )
                        }

                        Column {
                            Text(
                                text = "500+",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = TextHeadingBlack
                            )
                            Text(
                                text = "Kasiguranin\nWords &\nPhrases",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextHeadingBlack,
                                lineHeight = 18.sp
                            )
                        }

                        Column {
                            LinearProgressIndicator(
                                progress = { (progress.wordsLearned.toFloat() / 487f).coerceIn(0.1f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = TextHeadingBlack,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${progress.wordsLearned} over 487 words learned",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextHeadingBlack.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Right Column: Stories (Play Purple) & Mini Games (Play Pink)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stories Card (Play Purple)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                        .clickable { onNavigateToStories() },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PlayPurpleStart, PlayPurpleEnd)
                                )
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_stories_books),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(90.dp)
                                .padding(end = 6.dp, bottom = 6.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.35f)
                            ) {
                                Text(
                                    text = "Stories",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextWhite
                                )
                            }

                            Text(
                                text = "Cultural Stories",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                        }
                    }
                }

                // Mini Games Card (Play Pink)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                        .clickable { onNavigateToGames() },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PlayPinkStart, PlayPinkEnd)
                                )
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_mini_games_board),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(80.dp)
                                .padding(end = 4.dp, bottom = 4.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.35f)
                            ) {
                                Text(
                                    text = "Mini Games",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextHeadingBlack
                                )
                            }

                            Text(
                                text = "Play and Earn XP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextHeadingBlack.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // ─── 4. Leaderboard Banner Card ───
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clickable { onNavigateToLeaderboard() },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PlayPurpleStart, PlayPurpleEnd)
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TextWhite.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "LEADERBOARD 🏆",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "See Global Rankings",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = TextWhite
                        )
                        Text(
                            text = "Compete & climb the ranks!",
                            fontSize = 12.sp,
                            color = TextWhite.copy(alpha = 0.9f)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = TextWhite,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = Iconsax.MedalStar),
                                contentDescription = "Leaderboard",
                                tint = PlayPurpleEnd,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // ─── 5. Daily Quests Section ───
        Text(
            text = "Daily Quests",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextHeadingBlack
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clickable { onNavigateToAchievements() },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PlayGoldStart, PlayGoldEnd)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "Progress",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Goal: 10 Words/Day",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextHeadingBlack
                            )
                            Row(
                                modifier = Modifier.clickable { showStreakDialog = true },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.Flash),
                                    contentDescription = "Streak",
                                    tint = TextHeadingBlack,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Current Streak: ${progress.currentStreak} Days 🔥",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextHeadingBlack.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.Cup),
                                    contentDescription = "XP",
                                    tint = TextHeadingBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${progress.totalXp} XP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextHeadingBlack
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
