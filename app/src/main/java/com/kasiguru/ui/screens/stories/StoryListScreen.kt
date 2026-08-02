package com.kasiguru.ui.screens.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.StoryEntity
import com.kasiguru.ui.components.KasiGuruCard
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStory: (Int) -> Unit,
    viewModel: StoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasiguranin Stories") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.stories, key = { it.id }) { story ->
                StoryCard(
                    story = story,
                    currentXp = uiState.currentXp,
                    onClick = {
                        if (story.isUnlocked) {
                            onNavigateToStory(story.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StoryCard(
    story: StoryEntity,
    currentXp: Int,
    onClick: () -> Unit
) {
    val isLocked = !story.isUnlocked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) DarkSurfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked) 0.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = if (isLocked) {
                            Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))
                        } else {
                            Brush.linearGradient(listOf(Accent, AccentContainer))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = story.iconEmoji,
                    style = MaterialTheme.typography.displayMedium
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = story.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isLocked) TextGray else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (story.isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = Success,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = story.titleKasiguranin,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Accent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = Warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Unlocks at ${story.requiredXp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Warning
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    KasiGuruProgressBar(
                        progress = (currentXp.toFloat() / story.requiredXp.toFloat()).coerceIn(0f, 1f),
                        height = 6.dp,
                        animated = false
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Read",
                            tint = Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (story.currentPage > 0 && !story.isCompleted) "Continue" else "Read Story",
                            style = MaterialTheme.typography.labelMedium,
                            color = Accent
                        )
                    }
                }
            }
        }
    }
}
