package com.kasiguru.ui.screens.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.tour.TourChapter
import com.kasiguru.ui.tour.TourChapterId
import com.kasiguru.ui.tour.TourChapterState
import com.kasiguru.ui.tour.TourResumePoint
import com.kasiguru.ui.tour.chapterById
import com.kasiguru.ui.tour.tourChapters

/**
 * The chapters, offered rather than imposed.
 *
 * Only the core chapter ever runs on its own. Everything here is a walk the learner chooses to take,
 * which is what lets the tutorial cover every screen without a fifty-step wall that gets skipped at
 * step four.
 */
@Composable
fun TourChapterList(
    states: Map<TourChapterId, TourChapterState>,
    resumePoint: TourResumePoint?,
    onStartChapter: (TourChapterId) -> Unit,
    modifier: Modifier = Modifier
) {
    // The core chapter has its own button at the top of the page; listing it twice would be noise.
    val optional = tourChapters.filter { it.id != TourChapterId.Core }

    SoftCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = Space.xs)
    ) {
        optional.forEachIndexed { index, chapter ->
            ChapterRow(
                chapter = chapter,
                state = states[chapter.id] ?: TourChapterState.Available,
                resumeStep = resumePoint?.takeIf { it.chapterId == chapter.id }?.step,
                onClick = { onStartChapter(chapter.id) }
            )
            if (index != optional.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Space.md),
                    thickness = 1.dp,
                    color = BorderHairline
                )
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: TourChapter,
    state: TourChapterState,
    resumeStep: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        Surface(shape = Shapes.chip, color = Violet.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = Iconsax.Teacher),
                    contentDescription = null,
                    tint = Violet,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                StateBadge(state)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitleFor(chapter, state, resumeStep),
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }

        Icon(
            painter = painterResource(id = Iconsax.ArrowRight),
            contentDescription = null,
            tint = Faint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Only two states are worth a badge.
 *
 * Done is said in the subtitle rather than shown as a mark, and Skipped is said in neither - someone
 * who left a chapter early does not need it flagged back at them every time they open this page.
 */
@Composable
private fun StateBadge(state: TourChapterState) {
    val label = when (state) {
        TourChapterState.New -> "New"
        TourChapterState.Updated -> "Updated"
        else -> return
    }
    Spacer(Modifier.size(Space.xs))
    Surface(shape = Shapes.pill, color = Violet.copy(alpha = 0.12f)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Violet,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun subtitleFor(chapter: TourChapter, state: TourChapterState, resumeStep: Int?): String =
    when (state) {
        TourChapterState.InProgress ->
            "Continue — step ${(resumeStep ?: 0) + 1} of ${chapter.stops.size}"
        TourChapterState.Done -> "Done · take it again"
        else -> chapter.subtitle
    }

/** The green tick shown against a finished chapter, kept out of the row so the row stays readable. */
@Composable
fun ChapterDoneTint(state: TourChapterState): androidx.compose.ui.graphics.Color =
    if (state == TourChapterState.Done) Green else Violet

/** Convenience for callers that hold only an id. */
fun chapterTitle(id: TourChapterId): String = chapterById(id)?.title ?: ""
