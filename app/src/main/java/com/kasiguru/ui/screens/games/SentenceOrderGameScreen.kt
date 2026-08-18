package com.kasiguru.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.GameContinueButton
import com.kasiguru.ui.components.GameHeader
import com.kasiguru.ui.components.GameOverView
import com.kasiguru.ui.components.rememberGameExitGuard
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SentenceOrderGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: SentenceOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exitGuard = rememberGameExitGuard(
        active = uiState.questions.isNotEmpty() && !uiState.isGameFinished,
        onExit = onNavigateBack
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sentence Order",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = exitGuard) {
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
        if (uiState.questions.isEmpty() && !uiState.isGameFinished) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet)
            }
            return@Scaffold
        }

        if (uiState.isGameFinished) {
            GameOverView(
                score = uiState.score,
                total = uiState.totalQuestions,
                xpEarned = uiState.score * 10,
                starsEarned = uiState.starsEarned,
                onFinish = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        val question = uiState.questions.getOrNull(uiState.currentQuestionIndex) ?: return@Scaffold
        val qIndex = uiState.currentQuestionIndex + 1
        val hasChecked = uiState.isCorrect != null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
            GameHeader(
                label = "Sentence $qIndex/${uiState.totalQuestions}",
                progress = qIndex.toFloat() / uiState.totalQuestions.toFloat(),
                score = uiState.score,
                accentStart = StoriesCardStart,
                accentEnd = StoriesCardEnd
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Target English Meaning
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Arrange words to mean:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSubtleGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${question.englishSentence}\"",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Selected Words Slot Box — the border and fill carry the result, not colour alone: the
            // icon-free but explicit "Correct!"/"Not quite" line right below it backs it up in text.
            val slotColor = when (uiState.isCorrect) {
                true -> Success.copy(alpha = 0.16f)
                false -> Error.copy(alpha = 0.16f)
                null -> StoriesCardStart.copy(alpha = 0.3f)
            }
            val slotBorder = when (uiState.isCorrect) {
                true -> BorderStroke(2.dp, Success)
                false -> BorderStroke(2.dp, Error)
                null -> null
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(24.dp),
                color = slotColor,
                border = slotBorder
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.constructedWords.isEmpty()) {
                        Text(
                            text = "Tap word blocks below in predicate-initial order",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSubtleGray
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.constructedWords.forEach { word ->
                                FilterChip(
                                    selected = true,
                                    enabled = !hasChecked,
                                    onClick = { viewModel.deselectWord(word) },
                                    label = { Text(word, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TextHeadingBlack,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (hasChecked) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.isCorrect == true) {
                        "Correct!"
                    } else {
                        "Not quite — correct order: ${question.correctKasiguraninWords.joinToString(" ")}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isCorrect == true) Success else Error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Available Word Bank
            if (!hasChecked) {
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Text(
                        text = "Word Bank",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSubtleGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.availableWords.forEach { word ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.selectWord(word) },
                                label = { Text(word, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = HeroCardStart,
                                    labelColor = TextHeadingBlack
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }
            }

            if (hasChecked) {
                GameContinueButton(onClick = { viewModel.nextQuestion() })
            } else {
                ClayButton(
                    label = "Check sentence order",
                    onClick = { viewModel.checkAnswer() },
                    enabled = uiState.constructedWords.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
