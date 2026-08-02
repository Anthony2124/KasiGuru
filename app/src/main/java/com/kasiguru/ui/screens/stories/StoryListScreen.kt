package com.kasiguru.ui.screens.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kasiguru.R
import com.kasiguru.data.local.entity.StoryEntity
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
                title = {
                    Text(
                        "Kasiguranin Stories",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StoriesCardEnd)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = if (isLocked) {
                            Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                        } else {
                            Brush.linearGradient(listOf(StoriesCardStart, StoriesCardEnd))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_document_outline),
                    contentDescription = null,
                    tint = TextHeadingBlack,
                    modifier = Modifier.size(28.dp)
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
                        color = if (isLocked) TextSubtleGray else TextHeadingBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (story.isCompleted) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tick_circle),
                            contentDescription = "Completed",
                            tint = Success,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    text = story.titleKasiguranin,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextHeadingBlack,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtleGray,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lock_outline),
                            contentDescription = "Locked",
                            tint = Warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Unlocks at ${story.requiredXp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Warning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    KasiGuruProgressBar(
                        progress = (currentXp.toFloat() / story.requiredXp.toFloat()).coerceIn(0f, 1f),
                        height = 6.dp,
                        gradientColors = listOf(StoriesCardStart, StoriesCardEnd),
                        animated = false
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_outline),
                            contentDescription = "Read",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (story.currentPage > 0 && !story.isCompleted) "Continue" else "Read Story",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                    }
                }
            }
        }
    }
}
