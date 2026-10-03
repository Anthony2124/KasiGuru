package com.kasiguru.ui.screens.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.states.EmptyState
import com.kasiguru.ui.theme.Space

/**
 * What every way into stories shows while [com.kasiguru.util.Constants.STORIES_ENABLED] is off: the
 * folk tales are written, but there is no narrator to record them yet. Points to My words, the one
 * place in the Library a learner can go instead.
 */
@Composable
fun StoriesComingSoonContent(
    onOpenMyWords: () -> Unit,
    header: (@Composable () -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Space.gutter, end = Space.gutter, top = Space.xs, bottom = Space.navBarClearance
        ),
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        if (header != null) item(key = "header") { header() }
        item(key = "coming-soon") {
            EmptyState(
                pose = JepjepPose.Reading,
                title = "Stories are coming soon",
                message = "Folk tales from Casiguran are on their way. We are finding a narrator so you " +
                    "can hear every story read aloud in Kasiguranin. Until then, the words you meet " +
                    "collect in My words.",
                actionLabel = "Open My words",
                onAction = onOpenMyWords
            )
        }
    }
}

/** The same notice as a screen of its own, for the pushed stories and story routes. */
@Composable
fun StoriesComingSoonScreen(onNavigateBack: () -> Unit, onOpenMyWords: () -> Unit) {
    GroundScaffold(title = "Stories", onBack = onNavigateBack, pattern = GroundPattern.None) {
        StoriesComingSoonContent(onOpenMyWords = onOpenMyWords)
    }
}
