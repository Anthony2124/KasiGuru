package com.kasiguru.ui.screens.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kasiguru.ui.components.ConfettiView
import com.kasiguru.ui.components.KasiGuruProgressBar
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
                title = {
                    Text(
                        "Daily Review Deck",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
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
                CircularProgressIndicator(color = HeroCardStart)
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.Cup),
                            contentDescription = "Complete",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Daily Deck Complete!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "You reviewed ${uiState.cards.size} Kasiguranin flashcards using SuperMemo-2 SRS scheduling!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSubtleGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HeroCardStart),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Return to Dashboard", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
                        }
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
                gradientColors = listOf(HeroCardStart, HeroCardEnd)
            )

            // Flashcard
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
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (rotation <= 90f) {
                                Brush.linearGradient(listOf(HeroCardStart, HeroCardEnd))
                            } else {
                                Brush.linearGradient(listOf(VocabCardStart, VocabCardEnd))
                            }
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // Front Side: Kasiguranin Word
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = currentCard.category.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextHeadingBlack,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentCard.kasiguranin,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = TextHeadingBlack,
                                    fontSize = 32.sp
                                )
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        audioPlayerManager.playAudio(currentCard.kasiguranin, currentCard.audioFileName)
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.VolumeHigh),
                                        contentDescription = "Listen",
                                        tint = TextHeadingBlack,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            if (currentCard.ipaNotation.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "[${currentCard.ipaNotation}]",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextHeadingBlack.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = Iconsax.Repeat),
                                    contentDescription = null,
                                    tint = TextHeadingBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tap card to flip", style = MaterialTheme.typography.labelSmall, color = TextHeadingBlack.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        // Back Side: Tagalog & English Meanings + Example
                        Column(
                            modifier = Modifier.graphicsLayer { rotationY = 180f },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "TRANSLATION",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextHeadingBlack,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currentCard.tagalog,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextHeadingBlack
                            )
                            Text(
                                text = "(${currentCard.english})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextHeadingBlack.copy(alpha = 0.8f)
                            )

                            if (currentCard.exampleSentence.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = TextHeadingBlack.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "\"${currentCard.exampleSentence}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextHeadingBlack,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = currentCard.exampleTranslation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextHeadingBlack.copy(alpha = 0.75f),
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
                    color = TextSubtleGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        Text("Again", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        Text("Hard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isFlipped = false
                            viewModel.rateCard(ReviewRating.GOOD)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Good", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        Text("Easy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
