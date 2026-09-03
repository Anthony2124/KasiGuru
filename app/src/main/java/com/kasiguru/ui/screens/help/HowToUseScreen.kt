package com.kasiguru.ui.screens.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.kasiguru.ui.components.clay.ActivityState
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SectionCaption
import com.kasiguru.ui.components.clay.SectionHeading
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.clay.TimelineItem
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.tour.TourChapterId
import com.kasiguru.ui.tour.TourChapterState
import com.kasiguru.ui.tour.TourResumePoint

/**
 * The permanent version of the guided tour.
 *
 * Two thirds of learners skip a first-run tour, and a tour that can only be taken once is guidance
 * the app effectively does not have. This page carries the same seven answers as prose, plus the
 * three things the tour has no room for, and it can put the tour itself back on screen.
 *
 * Structured in four deliberately different registers rather than a stack of matching cards - one
 * lead action, one card of rows, one timeline, one run of plain prose on the ground, and a closing
 * card. A page of identical rounded rectangles is the layout failure DESIGN.md exists to prevent,
 * and a help page is exactly where that habit is most tempting.
 */
@Composable
fun HowToUseScreen(
    onNavigateBack: () -> Unit,
    onReplayTour: () -> Unit,
    chapterStates: Map<TourChapterId, TourChapterState> = emptyMap(),
    resumePoint: TourResumePoint? = null,
    onStartChapter: (TourChapterId) -> Unit = {},
    onNavigateToSubmitWord: () -> Unit = {},
    onNavigateToReport: () -> Unit = {}
) {
    GroundScaffold(
        title = "How to use KasiGuru",
        onBack = onNavigateBack,
        pattern = GroundPattern.Grid,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Space.gutter),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                GroundTitleBlock(
                    title = "How to use KasiGuru",
                    subtitle = "Five places, one daily loop, and how your progress is counted",
                    lead = {
                        Spacer(Modifier.height(Space.sm))
                        ClayButton(
                            label = "Take the tour again",
                            onClick = onReplayTour,
                            tone = ClayButtonTone.Primary,
                            modifier = Modifier.fillMaxWidth(),
                            leading = {
                                Icon(
                                    painter = painterResource(id = Iconsax.Teacher),
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                )

                Spacer(Modifier.height(Space.md))

                SectionHeading(text = "Walk through a screen")
                SectionCaption(
                    text = "Short guided tours of one screen at a time. Take them in any order, or " +
                        "not at all."
                )
                Spacer(Modifier.height(Space.xs))

                TourChapterList(
                    states = chapterStates,
                    resumePoint = resumePoint,
                    onStartChapter = onStartChapter
                )

                Spacer(Modifier.height(Space.md))

                SectionHeading(text = "The five tabs")
                SectionCaption(text = "The bar at the bottom follows you everywhere.")
                Spacer(Modifier.height(Space.xs))

                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = Space.xs)
                ) {
                    tabGuide.forEachIndexed { index, entry ->
                        TabGuideRow(entry)
                        if (index != tabGuide.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = Space.md),
                                thickness = 1.dp,
                                color = BorderHairline
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Space.md))

                SectionHeading(text = "A day in KasiGuru")
                SectionCaption(text = "The loop the app is built around. None of it takes long.")
                Spacer(Modifier.height(Space.xs))

                dailyLoop.forEachIndexed { index, step ->
                    TimelineItem(
                        state = ActivityState.Upcoming,
                        isLast = index == dailyLoop.lastIndex
                    ) {
                        Column(modifier = Modifier.padding(bottom = Space.sm)) {
                            Text(
                                text = step.first,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink
                            )
                            Text(
                                text = step.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Space.md))

                // Deliberately no card. After a card of rows and a timeline, a third bordered
                // container would flatten the page; prose on the ground gives it a quiet register.
                SectionHeading(text = "How your progress is counted")
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "XP is the running total of everything you have finished, and it is what " +
                        "moves you up the ten levels. Your daily goal is a slice of that same total, " +
                        "reset each morning.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = "Your streak counts days, not lessons. One finished activity keeps it " +
                        "alive; a day with none of them ends it, and it starts again at one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = "A word counts as practised the first time you meet it, and as mastered " +
                        "only once you have recalled it correctly across several days. The gap " +
                        "between those two numbers is the point: remembering a word tomorrow is " +
                        "harder, and worth more, than recognising it today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = "All of it works offline. Everything you do is saved on this phone first " +
                        "and synced when you next have a connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )

                Spacer(Modifier.height(Space.md))

                SectionHeading(text = "Help us make it better")
                Spacer(Modifier.height(Space.xs))
                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = Space.xs)
                ) {
                    HelpActionRow(
                        iconRes = Iconsax.Edit,
                        accent = Coral,
                        iconTint = Ink,
                        title = "Send us a Kasiguranin word",
                        subtitle = "Missing entries are reviewed and added to the dictionary",
                        onClick = onNavigateToSubmitWord
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Space.md),
                        thickness = 1.dp,
                        color = BorderHairline
                    )
                    HelpActionRow(
                        iconRes = Iconsax.InfoCircle,
                        accent = Violet,
                        iconTint = Violet,
                        title = "Report a problem",
                        subtitle = "A wrong translation, missing audio, or anything broken",
                        onClick = onNavigateToReport
                    )
                }

                Spacer(Modifier.height(Space.xl))
            }
        }
    )
}

private data class TabGuideEntry(
    val iconRes: Int,
    val name: String,
    val body: String
)

/**
 * The same seven answers the tour gives, in the same order and the same voice.
 *
 * No counts here either - not the size of the corpus, not the number of games. Those grow through
 * the admin portal, and a hardcoded figure on a help page goes stale silently.
 */
private val tabGuide = listOf(
    TabGuideEntry(
        Iconsax.HomeBold,
        "Learn",
        "Home. Today's plan, the path you are working through, and one button at the top that " +
            "always names whatever comes next."
    ),
    TabGuideEntry(
        Iconsax.Element4Bold,
        "Practice",
        "Games that drill the words you have already met. Stars unlock the harder ones, so play " +
            "the open ones first."
    ),
    TabGuideEntry(
        Iconsax.BookBold,
        "Words",
        "The whole dictionary, sorted by category, with audio on every entry. Tap a word for its " +
            "aspects, its pronunciation and an example sentence."
    ),
    TabGuideEntry(
        Iconsax.MedalStarBold,
        "Progress",
        "Every badge you have earned, and how close you are to the rest. Locked badges show what " +
            "they still need."
    ),
    TabGuideEntry(
        Iconsax.ProfileBold,
        "Profile",
        "Who you are and what you have learned, plus the way through to Settings, the leaderboard " +
            "and Casiguran's cultural heritage."
    )
)

private val dailyLoop = listOf(
    "Open Learn" to "Your plan is already built. Nothing to choose.",
    "Do the lesson" to "A few minutes. New words, then a check that you kept them.",
    "Clear your review" to "Words come back on the day you are about to forget them.",
    "Play a game or read a story" to "Whichever you feel like. Both count toward your goal."
)

@Composable
private fun TabGuideRow(entry: TabGuideEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.md, vertical = Space.sm),
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        Surface(shape = Shapes.chip, color = Violet.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = entry.iconRes),
                    contentDescription = null,
                    tint = Violet,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }
    }
}

@Composable
private fun HelpActionRow(
    iconRes: Int,
    accent: androidx.compose.ui.graphics.Color,
    /**
     * Violet passes at 6.00 against its own 16% tint and is the house idiom for a tinted chip;
     * Coral measures 2.03 the same way and fails even the non-text floor, so it carries ink instead.
     * DESIGN.md records both figures.
     */
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
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
        Surface(shape = Shapes.chip, color = accent.copy(alpha = 0.16f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            Text(
                text = subtitle,
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
