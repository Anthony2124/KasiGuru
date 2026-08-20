package com.kasiguru.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.NotificationEntity
import com.kasiguru.ui.components.clay.TagChip
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.GlassChip
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@Composable
fun NotificationInboxScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {},
    viewModel: NotificationInboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filterOptions = listOf(
        "All" to "All",
        "Streak" to "Streaks",
        "WordOfDay" to "Word of Day",
        "Leaderboard" to "Leaderboard",
        "Achievement" to "Badges"
    )

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all notifications?") },
            text = { Text("This removes every notification from your inbox. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showClearConfirm = false; viewModel.clearAll() }) {
                    Text("Clear all", color = Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    GroundScaffold(
        title = "Notifications",
        onBack = onNavigateBack,
        pattern = GroundPattern.Grid,
        actions = {
            if (uiState.notifications.isNotEmpty()) {
                Box {
                    // Material's vector rather than an Iconsax drawable, as before - the set has no
                    // overflow glyph. Tinted Muted now that the bar is lavender, not violet.
                    androidx.compose.material3.IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Muted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Clear all", color = Red) },
                            onClick = { showMenu = false; showClearConfirm = true }
                        )
                    }
                }
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                GroundTitleBlock(
                    title = "Notifications",
                    modifier = Modifier.padding(horizontal = Space.gutter),
                    lead = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Was a GlassChip on the canopy. On lavender a translucent white fill has
                            // nothing to be translucent against, so it becomes a solid tinted tag.
                            TagChip(
                                label = if (uiState.unreadCount > 0) {
                                    "${uiState.unreadCount} unread"
                                } else {
                                    "All caught up"
                                }
                            )
                            if (uiState.unreadCount > 0) {
                                Text(
                                    text = "Mark all read",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Violet,
                                    modifier = Modifier
                                        .clip(Shapes.chip)
                                        .clickable { viewModel.markAllAsRead() }
                                        .padding(horizontal = Space.xs, vertical = Space.xxs)
                                )
                            }
                        }
                    }
                )
                // Filter Categories Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Space.gutter, vertical = Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
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
                                selectedContainerColor = Violet,
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceSunken,
                                labelColor = Muted
                            ),
                            border = null,
                            shape = Shapes.pill
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
                            verticalArrangement = Arrangement.spacedBy(Space.sm)
                        ) {
                            Surface(shape = CircleShape, color = Violet.copy(alpha = 0.12f), modifier = Modifier.size(80.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Notification),
                                        contentDescription = null,
                                        tint = Violet,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Text(
                                text = "No Notifications",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Text(
                                text = "You're all caught up! Check back later for streak reminders and daily words.",
                                fontSize = 13.sp,
                                color = Muted,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = Space.gutter, end = Space.gutter, top = Space.xs, bottom = Space.navBarClearance
                        ),
                        verticalArrangement = Arrangement.spacedBy(Space.sm),
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
    )
}

@Composable
fun NotificationCard(
    notification: NotificationEntity,
    onClick: () -> Unit
) {
    val (iconRes, iconTint) = when (notification.category.lowercase()) {
        "streak" -> Iconsax.Flash to Gold
        "wordofday" -> Iconsax.Book to Violet
        "leaderboard" -> Iconsax.MedalStar to Coral
        else -> Iconsax.Cup to Green
    }

    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = if (notification.isRead) 0.dp else 6.dp,
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.Top
        ) {
            Surface(shape = CircleShape, color = iconTint.copy(alpha = 0.15f), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = notification.category,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
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
                        color = Ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Violet)
                        )
                    }
                }

                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = Muted,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.timestamp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Faint
                )
            }

            // A tap on a purely informational notification only marks it read; a tap on one of
            // these also navigates somewhere. Previously nothing distinguished the two in advance.
            if (notification.deepLinkRoute.isNotEmpty()) {
                Icon(
                    painter = painterResource(id = Iconsax.ArrowRight),
                    contentDescription = "Opens more detail",
                    tint = Faint,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(16.dp)
                )
            }
        }
    }
}
