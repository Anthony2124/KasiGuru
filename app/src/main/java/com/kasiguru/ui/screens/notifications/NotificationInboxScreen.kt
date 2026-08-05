package com.kasiguru.ui.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kasiguru.data.local.entity.NotificationEntity
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {},
    viewModel: NotificationInboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val filterOptions = listOf(
        "All" to "All 📬",
        "Streak" to "Streaks 🔥",
        "WordOfDay" to "Word of Day 🌟",
        "Leaderboard" to "Leaderboard 🏆",
        "Achievement" to "Badges 🎓"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notification Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                        Text(
                            text = if (uiState.unreadCount > 0) "${uiState.unreadCount} Unread Alerts" else "All caught up!",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSubtleGray
                        )
                    }
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
                    if (uiState.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text(
                                text = "Mark All Read",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
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
                .padding(padding)
        ) {
            // Filter Categories Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { (key, label) ->
                    val isSelected = uiState.selectedFilter.equals(key, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(key) },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TextHeadingBlack,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = TextHeadingBlack
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            if (uiState.notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = HeroCardStart.copy(alpha = 0.4f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = Iconsax.Notification),
                                    contentDescription = null,
                                    tint = TextHeadingBlack,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = "No Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                        Text(
                            text = "You're all caught up! Check back later for streak reminders and daily words.",
                            fontSize = 13.sp,
                            color = TextSubtleGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.notifications, key = { it.id }) { item ->
                        NotificationCard(
                            notification = item,
                            onClick = {
                                viewModel.markAsRead(item.id)
                                if (item.deepLinkRoute.isNotEmpty()) {
                                    onNavigateToRoute(item.deepLinkRoute)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationEntity,
    onClick: () -> Unit
) {
    val (cardBg, iconRes, iconTint) = when (notification.category.lowercase()) {
        "streak" -> Triple(
            listOf(VocabCardStart.copy(alpha = 0.4f), VocabCardEnd.copy(alpha = 0.2f)),
            Iconsax.Flash,
            VocabCardEnd
        )
        "wordofday" -> Triple(
            listOf(HeroCardStart.copy(alpha = 0.4f), HeroCardEnd.copy(alpha = 0.2f)),
            Iconsax.Book,
            HeroCardEnd
        )
        "leaderboard" -> Triple(
            listOf(StoriesCardStart.copy(alpha = 0.4f), StoriesCardEnd.copy(alpha = 0.2f)),
            Iconsax.MedalStar,
            StoriesCardEnd
        )
        else -> Triple(
            listOf(QuestsCardStart.copy(alpha = 0.4f), QuestsCardEnd.copy(alpha = 0.2f)),
            Iconsax.Cup,
            QuestsCardEnd
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Category Icon Badge
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = notification.category,
                        tint = TextHeadingBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 15.sp,
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        color = TextHeadingBlack,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        )
                    }
                }

                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = TextSubtleGray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.timestamp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSubtleGray.copy(alpha = 0.7f)
                )
            }
        }
    }
}
