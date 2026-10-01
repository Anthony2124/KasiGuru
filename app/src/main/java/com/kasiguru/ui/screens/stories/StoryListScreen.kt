package com.kasiguru.ui.screens.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.StoryCoverCard
import com.kasiguru.ui.components.clay.rememberStoryCoverRes
import com.kasiguru.ui.components.states.EmptyState
import com.kasiguru.ui.components.states.LoadingState
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.WidthClass
import com.kasiguru.ui.theme.rememberWidthClass
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor

/**
 * The stories as a screen of their own, pushed - from a notification or the guided tour. The Library
 * tab shows the same [StoryListContent] under its Stories segment.
 */
@Composable
fun StoryListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStory: (Int) -> Unit,
    onNavigateToSubmitLiterature: () -> Unit = {},
    viewModel: StoriesViewModel = hiltViewModel()
) {
    GroundScaffold(
        title = "Stories",
        onBack = onNavigateBack,
        pattern = GroundPattern.None,
        content = {
            StoryListContent(
                onNavigateToStory = onNavigateToStory,
                onNavigateToSubmitLiterature = onNavigateToSubmitLiterature,
                header = {
                    GroundTitleBlock(title = "Stories", subtitle = "Folk tales with Tagalog and English alongside")
                },
                viewModel = viewModel
            )
        }
    )
}

/**
 * The stories' body: the one to carry on with, every cover, and the way to share one.
 *
 * No bar of its own, so it can sit under [StoryListScreen]'s bar or inside the Library tab; [header]
 * is the first full-width item and scrolls away with the list. Locked stories are shown rather than
 * hidden - the lock is the motivation.
 */
@Composable
fun StoryListContent(
    onNavigateToStory: (Int) -> Unit,
    onNavigateToSubmitLiterature: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    viewModel: StoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // A bookshelf: two covers side by side on a phone, three on a tablet. One full-width cover per
    // row made each story a screen of its own and hid how many there are.
    val columns = if (rememberWidthClass() == WidthClass.COMPACT) 2 else 3

    // A story opened but not finished. The reader stores its page, so this is a real bookmark.
    val continueReading = uiState.stories.firstOrNull {
        it.isUnlocked && !it.isCompleted && it.currentPage > 0
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Space.gutter, end = Space.gutter, top = Space.xs, bottom = Space.navBarClearance
        ),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") { header() }
        }

        when {
            uiState.isLoading -> item(span = { GridItemSpan(maxLineSpan) }, key = "loading") {
                LoadingState(label = "Loading stories")
            }

            uiState.stories.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                EmptyState(
                    pose = JepjepPose.Reading,
                    title = "No stories yet",
                    message = "The folk tales arrive with the next sync. Know one? You can share it.",
                    actionLabel = "Share a story or poem",
                    onAction = onNavigateToSubmitLiterature
                )
            }

            else -> {
                continueReading?.let { story ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "continue") {
                        Column(Modifier.fillMaxWidth()) {
                            SectionHeading(text = "Continue reading")
                            Spacer(Modifier.height(Space.xxs))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                com.kasiguru.ui.components.KasiGuruProgressBar(
                                    progress = (story.currentPage + 1).toFloat() / story.totalPages.coerceAtLeast(1),
                                    modifier = Modifier.weight(1f),
                                    height = 6.dp
                                )
                                Spacer(Modifier.width(Space.sm))
                                Text(
                                    text = "Page ${story.currentPage + 1} of ${story.totalPages}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Muted
                                )
                            }
                            Spacer(Modifier.height(Space.sm))
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

                item(span = { GridItemSpan(maxLineSpan) }, key = "all-heading") {
                    SectionHeading(text = "All stories", modifier = Modifier)
                }

                itemsIndexed(uiState.stories, key = { _, story -> story.id }) { index, story ->
                    StoryCoverCard(
                        titleKasiguranin = story.titleKasiguranin,
                        title = story.title,
                        totalPages = story.totalPages,
                        isUnlocked = story.isUnlocked,
                        isCompleted = story.isCompleted,
                        requiredXp = story.requiredXp,
                        onClick = { onNavigateToStory(story.id) },
                        // Only the first card carries the anchor. The tour points at "the story
                        // shelf" as one idea; attaching this to every card would have each one
                        // overwrite the registry's entry for the same key as it (re)composes,
                        // leaving whichever story happened to compose last as the actual target.
                        modifier = Modifier
                            .fillMaxWidth()
                            .tourAnchor(if (index == 0) TourAnchor.StoryShelf else null),
                        cover = rememberStoryCoverRes(story.id)
                    )
                }

                // Quiet, not lime: reading is what this list is for; sharing is the invitation after it.
                item(span = { GridItemSpan(maxLineSpan) }, key = "share") {
                    ClayButton(
                        label = "Share a story or poem",
                        onClick = onNavigateToSubmitLiterature,
                        tone = ClayButtonTone.Quiet,
                        modifier = Modifier.fillMaxWidth(),
                        leading = {
                            Icon(
                                painter = painterResource(id = Iconsax.Edit),
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}
