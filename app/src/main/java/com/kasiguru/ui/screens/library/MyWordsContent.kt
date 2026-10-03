package com.kasiguru.ui.screens.library

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.repository.WordEncounterRepository
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.FilterPills
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.states.EmptyState
import com.kasiguru.ui.components.states.LoadingState
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Info
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.LimeText
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.util.audio.AudioPlayerManager

/** The filter pills, in order. "Needs practice" is due or lapsed words, the ones to review first. */
private val MyWordsFilters = listOf("All", MetIn.LESSON.label, MetIn.REVIEW.label, MetIn.GAME.label, "Needs practice")

/**
 * Every word the learner has met - in lessons, flashcard reviews and games - with where and when,
 * so the word seen in yesterday's game can be found and reviewed. The Library's middle side.
 *
 * The lead is the one thing to do with them: review what is due, in the flashcard deck. Each row
 * opens the full dictionary entry and can play the word.
 */
@Composable
fun MyWordsContent(
    onNavigateToWord: (Int) -> Unit,
    onOpenReview: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    viewModel: MyWordsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayerManager(context) }
    DisposableEffect(Unit) { onDispose { audioPlayer.stopAudio() } }

    val shown = remember(state.words, filter) {
        when (filter) {
            1 -> state.words.filter { it.metIn == MetIn.LESSON }
            2 -> state.words.filter { it.metIn == MetIn.REVIEW }
            3 -> state.words.filter { it.metIn == MetIn.GAME }
            4 -> state.words.filter { it.needsPractice }
            else -> state.words
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Space.gutter, end = Space.gutter, top = Space.xs, bottom = Space.navBarClearance
        ),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        if (header != null) item(key = "header") { header() }

        when {
            state.isLoading -> item(key = "loading") { LoadingState(label = "Gathering your words") }
            state.words.isEmpty() -> item(key = "empty") {
                EmptyState(
                    pose = JepjepPose.Curious,
                    title = "No words yet",
                    message = "Every word you meet in a lesson, a game or a flashcard review will collect " +
                        "here, so you can find it again and review it.",
                    actionLabel = "Review words",
                    onAction = onOpenReview
                )
            }
            else -> {
                item(key = "summary") { Summary(state = state, onOpenReview = onOpenReview) }
                item(key = "filters") {
                    FilterPills(
                        options = MyWordsFilters,
                        selectedIndex = filter,
                        onSelect = { filter = it },
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    )
                }
                if (shown.isEmpty()) {
                    item(key = "none-in-filter") {
                        Text(
                            text = if (filter == 4) "Nothing needs practice right now. Well done."
                            else "No words met in ${MyWordsFilters[filter].lowercase()} yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted,
                            modifier = Modifier.padding(top = Space.sm)
                        )
                    }
                }
                items(shown, key = { it.word.id }) { met ->
                    MetWordRow(
                        met = met,
                        onClick = { onNavigateToWord(met.word.id) },
                        onPlay = { audioPlayer.playWord(met.word) }
                    )
                }
            }
        }
    }
}

/** How many words, how many are learned, and the review of whatever is due. */
@Composable
private fun Summary(state: MyWordsUiState, onOpenReview: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.panel, border = BorderHairline) {
        Text(
            text = "${state.words.size} ${if (state.words.size == 1) "word" else "words"} met",
            style = MaterialTheme.typography.headlineSmall,
            color = Ink
        )
        Text(
            text = "${state.learnedCount} learned · ${state.dueCount} due for review",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
        Spacer(Modifier.height(Space.md))
        ClayButton(
            label = if (state.dueCount > 0) "Review ${state.dueCount} due" else "Practise these words",
            onClick = onOpenReview,
            modifier = Modifier.fillMaxWidth(),
            leading = {
                Icon(
                    painter = painterResource(id = Iconsax.Repeat),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

/** One word: the headword and its meanings, where and when it was met, and a play button. */
@Composable
private fun MetWordRow(met: MetWord, onClick: () -> Unit, onPlay: () -> Unit) {
    val word = met.word
    val now = System.currentTimeMillis()
    val when_ = when {
        met.lastSeenAt <= WordEncounterRepository.UNKNOWN_TIME -> "earlier"
        now - met.lastSeenAt < DateUtils.MINUTE_IN_MILLIS -> "just now"
        else -> DateUtils.getRelativeTimeSpanString(
            met.lastSeenAt, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.tile,
        border = BorderHairline,
        onClick = onClick,
        contentPadding = PaddingValues(start = Space.md, end = Space.xs, top = Space.sm, bottom = Space.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.kasiguranin,
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (met.isDue) {
                        Spacer(Modifier.width(Space.xs))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(LimeText))
                        Spacer(Modifier.width(Space.xxs))
                        Text("Due", style = MaterialTheme.typography.labelSmall, color = LimeText)
                    }
                }
                Text(
                    text = listOf(word.tagalog, word.english).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${MyWordsViewModel.sourceName(met.source)} · $when_",
                    style = MaterialTheme.typography.labelSmall,
                    color = Faint,
                    maxLines = 1
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    painter = painterResource(id = Iconsax.VolumeHigh),
                    contentDescription = "Listen to ${word.kasiguranin}",
                    tint = Info
                )
            }
        }
    }
}
