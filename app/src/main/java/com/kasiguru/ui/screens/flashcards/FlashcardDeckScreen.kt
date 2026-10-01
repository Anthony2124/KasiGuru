package com.kasiguru.ui.screens.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor
import com.kasiguru.ui.components.FlashcardFirstNote
import com.kasiguru.ui.components.GestureHint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import com.kasiguru.ui.components.FlashCard
import com.kasiguru.ui.components.SegmentedProgress
import com.kasiguru.util.srs.Sm2Algorithm
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.AudioPlayButton
import com.kasiguru.ui.components.ConfettiView
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.util.audio.AudioPlayerManager
import com.kasiguru.util.srs.ReviewRating

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
    Column(Modifier.fillMaxSize().background(Ground).statusBarsPadding()
        .navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(painterResource(Iconsax.CloseCircle), "Close flashcards", tint = Ink) }
            Text(currentCard.category, style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
            Text("${uiState.currentIndex + 1} / ${uiState.cards.size}", color = Muted)
        }
        SegmentedProgress(uiState.currentIndex, uiState.cards.size, Modifier.fillMaxWidth())
        FlashCard(currentCard, uiState.currentIndex + 1, side,
            onFlip = { if (!ratingPending) side = if (side < 2) side + 1 else 1 },
            onAudio = { audioPlayerManager.playWord(currentCard) },
            modifier = Modifier.fillMaxWidth().height(360.dp).tourAnchor(TourAnchor.FlashcardCard)
                .graphicsLayer { translationX = drag.value; alpha = if (reduced && ratingPending) 0f else 1f }
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
        if (uiState.currentIndex == 0 && side != 2) {
            FlashcardFirstNote(modifier = Modifier.padding(vertical = Space.sm))
        }
        AnimatedVisibility(visible = side == 2) {
            Column {
                Text("How well did you remember?", style = MaterialTheme.typography.titleSmall, color = Ink)
                Spacer(Modifier.height(Space.sm))
                ReviewRating.entries.chunked(2).forEach { ratings ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        ratings.forEach { rating ->
                            val days = Sm2Algorithm.calculateNextReview(currentCard, rating).intervalDays
                            ClayButton(label = "${rating.name.lowercase().replaceFirstChar(Char::uppercase)} · $days ${if (days == 1) "day" else "days"}",
                                onClick = { rate(rating) }, enabled = !uiState.isRating && !ratingPending,
                                tone = if (rating == ReviewRating.GOOD) ClayButtonTone.Primary else ClayButtonTone.Quiet,
                                modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(Space.xs))
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
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
