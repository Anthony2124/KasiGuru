package com.kasiguru.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SegmentedToggle
import com.kasiguru.ui.screens.stories.StoryListContent
import com.kasiguru.ui.screens.vocabulary.DictionaryContent
import com.kasiguru.ui.screens.vocabulary.DictionaryRefreshAction
import com.kasiguru.ui.screens.vocabulary.VocabularyViewModel
import com.kasiguru.ui.theme.Space

/** The Library's two sides, as the index [LibraryScreen]'s toggle and a navigation request use. */
object LibrarySegment {
    const val WORDS = 0
    const val STORIES = 1
}

/**
 * The Library tab: the dictionary and the stories behind one segmented toggle.
 *
 * They were a tab and a screen reachable from nowhere in particular; both are things you look
 * something up in, so they share a place. Each side is the very content its old screen showed
 * ([DictionaryContent], [StoryListContent]), so the pushed `vocabulary` and `stories` routes still
 * show exactly what the tab does.
 *
 * The toggle and the large title scroll with each side's list rather than pinning, so a phone keeps
 * its height for the words. Each side keeps its own search text and scroll position across switches.
 *
 * @param segmentRequest a side another screen asked for - Home's "See all" stories, for instance.
 *   Applied once, then reported back through [onSegmentRequestConsumed] so a later visit to the tab
 *   is not forced onto the same side again.
 */
@Composable
fun LibraryScreen(
    onNavigateToCategory: (String) -> Unit,
    onNavigateToWord: (Int) -> Unit,
    onNavigateToAddWord: () -> Unit,
    onNavigateToStory: (Int) -> Unit,
    onNavigateToShareStory: () -> Unit,
    segmentRequest: Int? = null,
    onSegmentRequestConsumed: () -> Unit = {},
    vocabularyViewModel: VocabularyViewModel = hiltViewModel()
) {
    var segment by rememberSaveable { mutableStateOf(segmentRequest ?: LibrarySegment.WORDS) }
    val isSyncing by vocabularyViewModel.isSyncing.collectAsState()
    val sides = rememberSaveableStateHolder()

    LaunchedEffect(segmentRequest) {
        if (segmentRequest != null) {
            segment = segmentRequest
            onSegmentRequestConsumed()
        }
    }

    val header: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth()) {
            GroundTitleBlock(
                title = "Library",
                subtitle = if (segment == LibrarySegment.WORDS) {
                    "Every word, with its meaning and how it sounds"
                } else {
                    "Folk tales with Tagalog and English alongside"
                }
            )
            Spacer(Modifier.height(Space.xs))
            SegmentedToggle(
                options = listOf("Words", "Stories"),
                selectedIndex = segment,
                onSelect = { segment = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Space.xs))
        }
    }

    // Refreshing from the cloud belongs to the dictionary; the stories side has nothing to refresh.
    val refreshAction: (@Composable RowScope.() -> Unit)? = if (segment == LibrarySegment.WORDS) {
        { DictionaryRefreshAction(isSyncing = isSyncing, onRefresh = vocabularyViewModel::refreshFromCloud) }
    } else {
        null
    }

    GroundScaffold(
        title = "Library",
        pattern = GroundPattern.None,
        actions = refreshAction,
        content = {
            sides.SaveableStateProvider(key = segment) {
                if (segment == LibrarySegment.WORDS) {
                    DictionaryContent(
                        onNavigateToCategory = onNavigateToCategory,
                        onNavigateToWord = onNavigateToWord,
                        onNavigateToSubmitWord = onNavigateToAddWord,
                        header = header,
                        viewModel = vocabularyViewModel
                    )
                } else {
                    StoryListContent(
                        onNavigateToStory = onNavigateToStory,
                        onNavigateToSubmitLiterature = onNavigateToShareStory,
                        header = header
                    )
                }
            }
        }
    )
}
