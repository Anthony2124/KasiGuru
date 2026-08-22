package com.kasiguru.ui.screens.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
            CircularProgressIndicator(color = Violet)
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
                        "You reviewed ${uiState.cards.size} Kasiguranin flashcards with SuperMemo-2 spaced repetition.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(Space.lg))

                    ClayButton(
                        label = "Return to Dashboard",
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
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "CardFlip"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = Iconsax.ArrowLeft),
                contentDescription = "Back",
                tint = Muted,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onNavigateBack)
            )
            Spacer(Modifier.width(Space.sm))
            KasiGuruProgressBar(
                progress = (uiState.currentIndex + 1).toFloat() / uiState.cards.size,
                showLabel = true,
                gradientColors = listOf(CanopyTop, CanopyBottom),
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Space.gutter, vertical = Space.sm),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Flashcard with 3D Flip
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isFlipped = !isFlipped
                    }
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    },
                shape = Shapes.panel,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (rotation <= 90f) {
                                Brush.linearGradient(listOf(CanopyTop, CanopyBottom))
                            } else {
                                Brush.linearGradient(listOf(CanopyBottom, CanopyTop))
                            }
                        )
                        .padding(Space.lg),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // Front Side: Kasiguranin Word
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(shape = Shapes.pill, color = Color.White.copy(alpha = 0.22f)) {
                                Text(
                                    text = currentCard.category.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(Modifier.height(Space.lg))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Space.sm)
                            ) {
                                Text(
                                    text = currentCard.kasiguranin,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    letterSpacing = (-0.5).sp
                                )
                                AudioPlayButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        audioPlayerManager.playAudio(currentCard.kasiguranin, currentCard.audioFileName)
                                    },
                                    size = 38.dp,
                                    contentDescription = "Listen"
                                )
                            }
                            if (currentCard.ipaNotation.isNotEmpty()) {
                                Spacer(Modifier.height(Space.xs))
                                Text(
                                    text = "[${currentCard.ipaNotation}]",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(Modifier.height(Space.lg))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = Iconsax.Repeat),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Tap card to flip",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // Back Side: Tagalog & English Meanings + Example
                        Column(
                            modifier = Modifier.graphicsLayer { rotationY = 180f },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(shape = Shapes.pill, color = Color.White.copy(alpha = 0.22f)) {
                                Text(
                                    text = "TRANSLATION",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(Modifier.height(Space.md))
                            Text(
                                text = currentCard.tagalog,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "(${currentCard.english})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )

                            if (currentCard.exampleSentence.isNotEmpty()) {
                                Spacer(Modifier.height(Space.md))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                Spacer(Modifier.height(Space.md))
                                Text(
                                    text = "\"${currentCard.exampleSentence}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = currentCard.exampleTranslation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.md))

            // SM-2 4-Rating Buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Rate your recall performance (SM-2 SRS):",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                    modifier = Modifier.padding(bottom = Space.xs),
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isFlipped = false
                            viewModel.rateCard(ReviewRating.AGAIN)
                        },
                        enabled = !uiState.isRating,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        shape = Shapes.chip,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Again", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isFlipped = false
                            viewModel.rateCard(ReviewRating.HARD)
                        },
                        enabled = !uiState.isRating,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Warning),
                        shape = Shapes.chip,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        // Warning is a dark amber; Ink measures 3.38 on it (fails AA) where White
                        // measures 4.87 — the other three buttons already use white for this reason.
                        Text("Hard", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isFlipped = false
                            viewModel.rateCard(ReviewRating.GOOD)
                        },
                        enabled = !uiState.isRating,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet),
                        shape = Shapes.chip,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Good", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isFlipped = false
                            viewModel.rateCard(ReviewRating.EASY)
                        },
                        enabled = !uiState.isRating,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        shape = Shapes.chip,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Easy", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(Space.navBarClearance))
        }
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
                        .background(Violet.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.TickCircleBold),
                        contentDescription = null,
                        tint = Violet,
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
                    label = "Back to Learn",
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
