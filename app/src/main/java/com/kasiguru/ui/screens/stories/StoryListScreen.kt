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
import com.kasiguru.ui.components.clay.CanopyBackButton
import com.kasiguru.ui.components.clay.CanopyScaffold
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@Composable
fun StoryListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStory: (Int) -> Unit,
    viewModel: StoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    CanopyScaffold(
        canopyHeight = 128.dp,
        canopyContent = {
            CanopyBackButton(onClick = onNavigateBack)
            Spacer(Modifier.height(Space.sm))
            Text(text = "Kasiguranin Stories", style = MaterialTheme.typography.headlineMedium, color = OnCanopy)
            Text(
                text = "Read folk tales in Kasiguranin, with translations alongside",
                style = MaterialTheme.typography.bodyMedium,
                color = OnCanopy
            )
        },
        sheetContent = {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Violet)
                }
                return@CanopyScaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.md, bottom = Space.navBarClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                items(uiState.stories, key = { it.id }) { story ->
                    StoryCard(
                        story = story,
                        onClick = { if (story.isUnlocked) onNavigateToStory(story.id) }
                    )
                }
            }
        }
    )
}

@Composable
private fun StoryCard(
    story: StoryEntity,
    onClick: () -> Unit
) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (story.isUnlocked) onClick else null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(Shapes.panel)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (story.isUnlocked) listOf(CanopyTop, CanopyBottom)
                            else listOf(Color(0xFFB0B7C3), Color(0xFF9098A5))
                        )
                    )
                    .padding(Space.md)
            ) {
                Surface(
                    shape = Shapes.pill,
                    color = if (story.isUnlocked) Gold else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = story.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = RewardInk,
                        fontSize = 10.sp,
                        letterSpacing = 0.4.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (story.isUnlocked) Iconsax.Book else Iconsax.Lock),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(Space.gutter)) {
                Text(
                    text = story.titleKasiguranin,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )
                Text(text = story.title, style = MaterialTheme.typography.bodyMedium, color = Muted)

                Spacer(Modifier.height(Space.sm))

                Text(
                    text = story.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.copy(alpha = 0.85f),
                    maxLines = 2,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(Space.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = Shapes.pill, color = Violet.copy(alpha = 0.12f)) {
                        Text(
                            text = "${story.totalPages} Pages",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Violet,
                            fontSize = 11.sp
                        )
                    }

                    if (story.isUnlocked) {
                        Surface(shape = Shapes.pill, color = Violet) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Read Story",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Icon(
                                    painter = painterResource(id = Iconsax.ArrowRight),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    } else {
                        Surface(shape = Shapes.pill, color = SurfaceSunken) {
                            Text(
                                text = "Locked",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Muted
                            )
                        }
                    }
                }
            }
        }
    }
}
