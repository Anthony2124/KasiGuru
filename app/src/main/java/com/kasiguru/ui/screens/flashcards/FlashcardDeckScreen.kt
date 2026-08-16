package com.kasiguru.ui.screens.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.components.CoastPillButton
import com.kasiguru.ui.components.ConfettiView
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.PillVariant
import com.kasiguru.ui.theme.*
import com.kasiguru.util.audio.AudioPlayerManager
import com.kasiguru.util.srs.ReviewRating

@OptIn(ExperimentalMaterial3Api::class)
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
        onDispose {
            audioPlayerManager.stopAudio()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "Daily Review Deck",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextHeadingBlack,
                        letterSpacing = (-0.3).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowLeft),
                            contentDescription = "Back",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PlayPurpleStart)
            }
            return@Scaffold
        }

        if (uiState.cards.isEmpty() || uiState.isDeckComplete) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ConfettiView()

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(XpGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = Iconsax.CupBold),
                                contentDescription = "Complete",
                                tint = XpGoldDark,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Daily Deck Complete!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = CoastInk,
                            letterSpacing = (-0.3).sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "You reviewed ${uiState.cards.size} Kasiguranin flashcards with SuperMemo-2 spaced repetition.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoastMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        CoastPillButton(
                            label = "Return to Dashboard",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateBack()
                            },
                            variant = PillVariant.Purple
                        )
                    }
                }
            }
            return@Scaffold
        }

        val currentCard = uiState.cards.getOrNull(uiState.currentIndex) ?: return@Scaffold
        var isFlipped by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(
            targetValue = if (isFlipped) 180f else 0f,
            animationSpec = tween(durationMillis = 400),
            label = "CardFlip"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KasiGuruProgressBar(
                progress = (uiState.currentIndex + 1).toFloat() / uiState.cards.size,
                showLabel = true,
                gradientColors = listOf(PlayPurpleStart, PlayPurpleEnd)
            )

            // Flashcard with 3D Flip
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isFlipped = !isFlipped
                    }
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (rotation <= 90f) {
                                Brush.linearGradient(listOf(PlayPurpleStart, PlayPurpleEnd))
                            } else {
                                Brush.linearGradient(listOf(VocabSea, VocabSeaDark))
                            }
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // Front Side: Kasiguranin Word
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = currentCard.category.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = currentCard.kasiguranin,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextWhite,
                                    fontSize = 32.sp,
                                    letterSpacing = (-0.5).sp
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = XpGold,
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        audioPlayerManager.playAudio(currentCard.kasiguranin, currentCard.audioFileName)
                                    }
                                ) {
                                    Box(modifier = Modifier.padding(9.dp), contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(id = Iconsax.VolumeHigh),
                                            contentDescription = "Listen",
                                            tint = CoastInk,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            if (currentCard.ipaNotation.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "[${currentCard.ipaNotation}]",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = Iconsax.Repeat),
                                    contentDescription = null,
                                    tint = TextWhite.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Tap card to flip",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextWhite.copy(alpha = 0.85f),
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
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "TRANSLATION",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currentCard.tagalog,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "(${currentCard.english})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextWhite.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )

                            if (currentCard.exampleSentence.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "\"${currentCard.exampleSentence}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = currentCard.exampleTranslation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextWhite.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // SM-2 4-Rating Buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Rate your recall performance (SM-2 SRS):",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoastMuted,
                    modifier = Modifier.padding(bottom = 10.dp),
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isFlipped = false
                            viewModel.rateCard(ReviewRating.AGAIN)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Error),
                        shape = RoundedCornerShape(14.dp),
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
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Warning),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Hard", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = CoastInk)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isFlipped = false
                            viewModel.rateCard(ReviewRating.GOOD)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PlayPurpleStart),
                        shape = RoundedCornerShape(14.dp),
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
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Success),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Easy", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}
