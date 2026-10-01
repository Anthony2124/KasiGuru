package com.kasiguru.ui.screens.flashcards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.AudioPlayButton
import com.kasiguru.ui.components.ConfettiView
import com.kasiguru.ui.components.FlashCard
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.SegmentedProgress
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor
import com.kasiguru.util.audio.AudioPlayerManager
import com.kasiguru.util.srs.ReviewRating
import com.kasiguru.util.srs.Sm2Algorithm
import kotlinx.coroutines.launch

/**
 * Immersive on purpose, like Lesson Player and the mini-games: no canopy, no bottom nav, just the
 * card and its rating row, so reviewing one word doesn't feel like a detour through app chrome.
 */
@Composable
fun FlashcardDeckScreen(
    onNavigateBack: () -> Unit,
    viewModel: FlashcardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioPlayerManager = remember { AudioPlayerManager(context) }

    DisposableEffect(Unit) {
        onDispose { audioPlayerManager.stopAudio() }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize().background(Ground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Lime)
        }
        return
    }

    // An empty schedule is spaced repetition working, not a deck to celebrate finishing. Kept
    // separate from the completion state below, which used to render "Daily Deck Complete! You
    // reviewed 0 flashcards" on a day with nothing due.
    if (uiState.isNothingDue) {
        NothingDueState(
            onPractiseAnyway = viewModel::practiseAnyway,
            onNavigateBack = onNavigateBack
        )
        return
    }

    if (uiState.cards.isEmpty() || uiState.isDeckComplete) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ground)
                .padding(Space.gutter),
            contentAlignment = Alignment.Center
        ) {
            ConfettiView()
            CloseCorner(onNavigateBack, Modifier.align(Alignment.TopStart).offset(x = (-Space.gutter), y = (-Space.gutter)))

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.CupBold),
                            contentDescription = "Complete",
                            tint = GoldDeep,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(Modifier.height(Space.md))

                    Text(
                        "Daily Deck Complete!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(Space.xs))

                    Text(
                        "You reviewed ${uiState.cards.size} words. They'll come back when it's time to practise again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(Space.lg))

                    ClayButton(
                        label = "Continue",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateBack()
                        },
                        tone = ClayButtonTone.Primary
                    )
                }
            }
        }
        return
    }

    val currentCard = uiState.cards.getOrNull(uiState.currentIndex) ?: return
    var side by remember(currentCard.id) { mutableIntStateOf(0) }
    var ratingPending by remember(currentCard.id) { mutableStateOf(false) }
    val drag = remember(currentCard.id) { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    val reduced = LocalReducedMotion.current
    val rate: (ReviewRating) -> Unit = { rating ->
        if (!uiState.isRating && !ratingPending && side == 2) {
            ratingPending = true
            scope.launch {
                drag.animateTo(if (rating == ReviewRating.AGAIN) -1000f else 1000f, tween(if (reduced) 0 else 180))
                viewModel.rateCard(rating)
            }
        }
    }
    // Header, the card filling the middle, and a fixed answer area at the foot: the card keeps its
    // size when the rating buttons appear, so nothing jumps as it is turned over.
    Column(Modifier.fillMaxSize().background(Ground).statusBarsPadding()
        .navigationBarsPadding().padding(horizontal = Space.gutter)) {
        // Quit, progress and the count on one line, as in a lesson. The category now sits on the card.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(56.dp)) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(painterResource(Iconsax.CloseCircle), "Close flashcards", tint = Muted)
            }
            SegmentedProgress(
                uiState.currentIndex, uiState.cards.size,
                Modifier.weight(1f).offset(x = (-4).dp)
            )
            Spacer(Modifier.width(Space.sm))
            Text(
                "${uiState.currentIndex + 1} / ${uiState.cards.size}",
                style = MaterialTheme.typography.labelLarge,
                color = Muted
            )
        }
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = Space.md),
            contentAlignment = Alignment.Center
        ) {
        // A portrait card, as large as the space allows with room for its tilt and the fanned card.
        val cardWidth = minOf(maxWidth * 0.9f, maxHeight * CardAspect * 0.96f)
        FlashCard(currentCard, uiState.currentIndex + 1, side,
            onFlip = { if (!ratingPending) side = if (side < 2) side + 1 else 1 },
            onAudio = { audioPlayerManager.playWord(currentCard) },
            total = uiState.cards.size,
            modifier = Modifier.width(cardWidth).aspectRatio(CardAspect).tourAnchor(TourAnchor.FlashcardCard)
                .graphicsLayer {
                    translationX = drag.value
                    // Tilts with the swipe, so the direction it will be filed reads before it lands.
                    rotationZ = (drag.value / 40f).coerceIn(-12f, 12f)
                    alpha = if (reduced && ratingPending) 0f else 1f
                }
                .pointerInput(currentCard.id, side, ratingPending) {
                    if (side == 2 && !ratingPending) detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount -> change.consume(); scope.launch { drag.snapTo(drag.value + amount) } },
                        onDragCancel = { scope.launch { drag.animateTo(0f) } },
                        onDragEnd = {
                            if (drag.value > 100f) rate(ReviewRating.GOOD)
                            else if (drag.value < -100f) rate(ReviewRating.AGAIN)
                            else scope.launch { drag.animateTo(0f) }
                        }
                    )
                })
        }
        Box(Modifier.fillMaxWidth().height(AnswerAreaHeight), contentAlignment = Alignment.TopCenter) {
            // Until the meaning is showing, the foot says where the learner is in the three steps;
            // the card itself carries the tap instruction.
            androidx.compose.animation.AnimatedVisibility(visible = side != 2) {
                ReviewSteps(side = side, modifier = Modifier.padding(top = Space.sm))
            }
            androidx.compose.animation.AnimatedVisibility(visible = side == 2) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "How well did you remember?",
                            style = MaterialTheme.typography.titleSmall,
                            color = Ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text("or swipe", style = MaterialTheme.typography.labelMedium, color = Faint)
                    }
                    Spacer(Modifier.height(Space.sm))
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        ReviewRating.entries.forEach { rating ->
                            val days = Sm2Algorithm.calculateNextReview(currentCard, rating).intervalDays
                            RatingButton(
                                rating = rating,
                                days = days,
                                enabled = !uiState.isRating && !ratingPending,
                                onClick = { rate(rating) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Open, flip, rate: the three steps of one card, with the current one lit. */
@Composable
private fun ReviewSteps(side: Int, modifier: Modifier = Modifier) {
    val steps = listOf("Open", "Flip", "Rate")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val done = index < side
            val current = index == side
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (current) LimeTint else Color.Transparent)
                    .padding(horizontal = Space.sm, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (current || done) Lime else TrackNeutral),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(painterResource(Iconsax.TickCircle), null, tint = OnLime, modifier = Modifier.size(14.dp))
                    } else {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = if (current) OnLime else Muted)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (current) Ink else Muted
                )
            }
            if (index < steps.lastIndex) {
                Box(Modifier.width(16.dp).height(2.dp).background(if (done) Lime else TrackNeutral))
            }
        }
    }
}

/**
 * One rating, with its next interval on a second line so four fit side by side. Good is the lime
 * default; Again and Easy carry their meaning in words, the tint only backs it up.
 */
@Composable
private fun RatingButton(rating: ReviewRating, days: Int, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (face, lip, ink) = when (rating) {
        ReviewRating.GOOD -> Triple(Lime, LimeLip, OnLime)
        else -> Triple(SurfaceSunken, BorderHairline, Ink)
    }
    val accent = when (rating) {
        ReviewRating.AGAIN -> RedText
        ReviewRating.HARD -> AmberText
        ReviewRating.GOOD -> OnLime
        ReviewRating.EASY -> Info
    }
    com.kasiguru.ui.components.clay.ClaySurface(
        face = face,
        lipColor = lip,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                rating.name.lowercase().replaceFirstChar(Char::uppercase),
                style = MaterialTheme.typography.titleSmall,
                color = if (rating == ReviewRating.GOOD) ink else accent,
                maxLines = 1
            )
            Text(
                "$days ${if (days == 1) "day" else "days"}",
                style = MaterialTheme.typography.labelSmall,
                color = if (rating == ReviewRating.GOOD) ink else Muted,
                maxLines = 1
            )
        }
    }
}

/** Width over height of a flashcard: a portrait paper card. */
private const val CardAspect = 0.74f

/** Room kept at the foot for the rating buttons, so the card does not resize when they appear. */
private val AnswerAreaHeight = 112.dp

/** A close button in the top corner of the full-screen deck states. */
@Composable
private fun CloseCorner(onClose: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClose, modifier = modifier.statusBarsPadding().padding(Space.xs)) {
        Icon(painterResource(Iconsax.CloseCircle), "Close flashcards", tint = Ink)
    }
}

/**
 * Shown when the schedule has nothing for today.
 *
 * Deliberately not framed as an error or an empty shelf. Nothing due means the words are resting
 * at the point where recall is hardest and therefore most durable — coming back tomorrow is the
 * correct move, and the copy says so rather than nudging the learner into busywork. The practice
 * option stays available because sometimes people want to study anyway, but it is secondary and
 * honestly labelled as ahead of schedule.
 */
@Composable
private fun NothingDueState(
    onPractiseAnyway: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .padding(Space.gutter),
        contentAlignment = Alignment.Center
    ) {
        CloseCorner(onNavigateBack, Modifier.align(Alignment.TopStart).offset(x = (-Space.gutter), y = (-Space.gutter)))
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Lime.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.TickCircleBold),
                        contentDescription = null,
                        tint = LimeText,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.height(Space.md))

                Text(
                    "You're all caught up",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(Space.xs))

                Text(
                    "No words are due for review today. They're scheduled to come back just before you'd forget them — that spacing is what makes them stick.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(Space.lg))

                ClayButton(
                    label = "Continue",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateBack()
                    },
                    tone = ClayButtonTone.Primary
                )

                Spacer(Modifier.height(Space.sm))

                ClayButton(
                    label = "Practise ahead anyway",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPractiseAnyway()
                    },
                    tone = ClayButtonTone.Quiet
                )
            }
        }
    }
}
