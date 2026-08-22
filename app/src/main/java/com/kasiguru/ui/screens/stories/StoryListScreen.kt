package com.kasiguru.ui.screens.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.kasiguru.data.local.entity.StoryEntity
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.StoryCoverCard
import com.kasiguru.ui.components.clay.rememberStoryCoverRes
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@Composable
fun StoryListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStory: (Int) -> Unit,
    onNavigateToSubmitLiterature: () -> Unit = {},
    viewModel: StoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GroundScaffold(
        title = "Stories",
        onBack = onNavigateBack,
        pattern = GroundPattern.Arcs,
        content = {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Violet)
                }
                return@GroundScaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.md, bottom = Space.navBarClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                item { GroundTitleBlock(title = "Stories", subtitle = "Folk tales with Tagalog and English alongside") }
                item {
                    ClayButton(
                        label = "Submit a story or poem",
                        onClick = onNavigateToSubmitLiterature,
                        tone = ClayButtonTone.Reward,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(uiState.stories, key = { it.id }) { story ->
                    StoryCoverCard(
                        titleKasiguranin = story.titleKasiguranin,
                        title = story.title,
                        totalPages = story.totalPages,
                        isUnlocked = story.isUnlocked,
                        isCompleted = story.isCompleted,
                        requiredXp = story.requiredXp,
                        onClick = { onNavigateToStory(story.id) },
                        modifier = Modifier.fillMaxWidth(),
                        cover = rememberStoryCoverRes(story.id)
                    )
                }
            }
        }
    )
}

