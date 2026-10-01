package com.kasiguru.ui.screens.learn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.kasiguru.domain.lesson.LearningTree
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.states.EmptyState
import com.kasiguru.ui.components.states.LoadingState
import com.kasiguru.ui.screens.learn.tree.learningPath
import com.kasiguru.ui.screens.learn.tree.learningPathCurrentItemIndex
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Space

/** The path stays centred and readable on a tablet rather than winding across the whole width. */
private val PathMaxWidth = 520.dp

/**
 * The Learn tab: the learning path, section by section, and nothing else.
 *
 * Today's plan - the one next action, the day goal, reviews, stories - moved to Home. What is left is
 * the journey itself: each section opens on the Casiguran place it belongs to, and Jepjep waits at
 * the node that is the learner's to take next. The tab opens scrolled to that node, because after a
 * few weeks it sits several screens below the first section.
 */
@Composable
fun LearnScreen(
    onStartLesson: (unitId: String, lessonIndex: Int) -> Unit,
    viewModel: LearnPathViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // The path is a snapshot, and the work that changes it happens in the lesson player. The
    // lifecycle owner inside a NavHost is the back-stack entry, so this fires on return to the tab.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val listState = rememberLazyListState()
    // Advance the view only when completing a lesson changes the current node.
    var arrivedAtCurrent by rememberSaveable { mutableStateOf(false) }

    val subtitle = uiState.currentSection?.let { "You are in ${it.definition.title}" }
        ?: if (uiState.tree.isNotEmpty() && uiState.tree.all { it.isComplete }) {
            "Every section walked. Keep them sharp with review."
        } else {
            "Section by section, from greetings to everyday talk"
        }

    var lastCurrentIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val reducedMotion = com.kasiguru.ui.theme.LocalReducedMotion.current
    val currentIndex = learningPathCurrentItemIndex(uiState.tree)
    LaunchedEffect(uiState.isLoading, currentIndex) {
        if (uiState.isLoading || currentIndex == null) return@LaunchedEffect
        val target = (currentIndex - 1).coerceAtLeast(0)
        if (!arrivedAtCurrent) listState.scrollToItem(target)
        else if (lastCurrentIndex != null && lastCurrentIndex != currentIndex) {
            if (reducedMotion) listState.scrollToItem(target) else listState.animateScrollToItem(target)
        }
        lastCurrentIndex = currentIndex
        arrivedAtCurrent = true
    }

    // The title sits in the bar from the start rather than in a scrolling title block: the tab usually
    // opens already scrolled to the current node, where a title block would be off screen.
    GroundScaffold(
        title = "Learn",
        pattern = GroundPattern.None,
        compactTitle = true,
        content = {
            when {
                uiState.isLoading -> LoadingState(label = "Loading your path")

                uiState.tree.isEmpty() -> EmptyState(
                    pose = JepjepPose.Sleeping,
                    title = "Nothing on the path yet",
                    message = "The lessons appear once the dictionary has finished loading.",
                    actionLabel = "Check again",
                    onAction = viewModel::refresh
                )

                else -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.gutter)
                            .padding(bottom = Space.xs)
                    )
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .widthIn(max = PathMaxWidth),
                        contentPadding = PaddingValues(
                            start = Space.gutter,
                            end = Space.gutter,
                            top = Space.xs,
                            bottom = Space.navBarClearance
                        )
                    ) {
                        learningPath(
                            sections = uiState.tree,
                            onOpenLesson = onStartLesson,
                            // A checkpoint is a lesson over a different set of words, so it opens the
                            // same player by the same route -- see LearningTree.masteryUnitId.
                            onOpenMastery = { sectionId ->
                                onStartLesson(LearningTree.masteryUnitId(sectionId), 0)
                            }
                        )
                    }
                }
            }
        }
    )
}
