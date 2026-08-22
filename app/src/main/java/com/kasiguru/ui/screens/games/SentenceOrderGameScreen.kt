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
import com.kasiguru.ui.components.GameAnswerFeedback
import com.kasiguru.ui.components.GameHeader
import com.kasiguru.ui.components.GameOverView
import com.kasiguru.ui.components.rememberGameExitGuard
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.theme.Ink

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

    GroundScaffold(
        title = "Sentence Order",
        onBack = exitGuard,
        // Close, not back: this quits a round in progress, and exitGuard asks before discarding.
        navIcon = Iconsax.CloseCircle,
        // No texture while playing - the exercise is the only thing that should read as content.
        pattern = GroundPattern.None,
        compactTitle = true,
        content = {
        if (uiState.questions.isEmpty() && !uiState.isGameFinished) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet)
            }
            return@GroundScaffold
        }

        if (uiState.isGameFinished) {
            GameOverView(
                score = uiState.score,
                total = uiState.totalQuestions,
                xpEarned = uiState.score * 10,
                starsEarned = uiState.starsEarned,
                onFinish = onNavigateBack,
                modifier = Modifier
            )
            return@GroundScaffold
        }

        val question = uiState.questions.getOrNull(uiState.currentQuestionIndex) ?: return@GroundScaffold
        val qIndex = uiState.currentQuestionIndex + 1
        val hasChecked = uiState.isCorrect != null

        Column(
            modifier = Modifier
                .fillMaxSize()
                
                .padding(Space.gutter),
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
                        color = Muted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${question.englishSentence}\"",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
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
                            color = Muted
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
                                        selectedContainerColor = Ink,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Available Word Bank
            if (!hasChecked) {
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Text(
                        text = "Word Bank",
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted,
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
                                    labelColor = Ink
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }
            }

            if (hasChecked) {
                GameAnswerFeedback(
                    isCorrect = uiState.isCorrect == true,
                    correctAnswer = question.correctKasiguraninWords.joinToString(" "),
                    onContinue = { viewModel.nextQuestion() }
                )
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
    )
}
