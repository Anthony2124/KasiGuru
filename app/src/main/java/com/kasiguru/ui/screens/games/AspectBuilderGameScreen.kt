package com.kasiguru.ui.screens.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.GameAnswerFeedback
import com.kasiguru.ui.components.GameHeader
import com.kasiguru.ui.components.GameHintButton
import com.kasiguru.ui.components.GameOptionRow
import com.kasiguru.ui.components.HintLanguages
import com.kasiguru.ui.components.hintFor
import com.kasiguru.ui.components.GameOptionState
import com.kasiguru.ui.components.GameOverView
import com.kasiguru.ui.components.GameUnavailableState
import com.kasiguru.ui.components.rememberGameExitGuard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.theme.Ink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AspectBuilderGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: AspectBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exitGuard = rememberGameExitGuard(
        active = !uiState.isLoading && !uiState.isGameOver && !uiState.isUnavailable,
        onExit = onNavigateBack
    )

    GroundScaffold(
        title = "Aspect Builder",
        onBack = exitGuard,
        // Close, not back: this quits a round in progress, and exitGuard asks before discarding.
        navIcon = Iconsax.CloseCircle,
        // No texture while playing - the exercise is the only thing that should read as content.
        pattern = GroundPattern.None,
        compactTitle = true,
        content = {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet)
            }
            return@GroundScaffold
        }

        if (uiState.isUnavailable) {
            GameUnavailableState(
                accentColor = HeroCardStart,
                onBack = onNavigateBack,
                modifier = Modifier,
                title = "Aspect Builder is coming soon",
                message = "We are documenting the verb aspect forms with our language experts. Check back after the next dictionary update!"
            )
            return@GroundScaffold
        }

        if (uiState.isGameOver) {
            GameOverView(
                score = uiState.score,
                total = uiState.totalQuestions,
                xpEarned = uiState.xpEarned,
                starsEarned = uiState.starsEarned,
                reviewItems = uiState.reviewItems,
                onFinish = onNavigateBack,
                modifier = Modifier
            )
            return@GroundScaffold
        }

        val question = uiState.questions.getOrNull(uiState.currentIndex) ?: return@GroundScaffold
        val qIndex = uiState.currentIndex + 1
        val hasAnswered = uiState.selectedAnswer != null

        Column(
            modifier = Modifier
                .fillMaxSize()
                
                .padding(Space.gutter),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                GameHeader(
                    label = "Question $qIndex/${uiState.totalQuestions}",
                    progress = qIndex.toFloat() / uiState.totalQuestions.toFloat(),
                    score = uiState.score,
                    accentStart = HeroCardStart,
                    accentEnd = HeroCardEnd
                )

                // Prompt Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Root Verb: ${question.rootWord.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Muted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Form required: ${question.targetAspect}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Text(
                            text = "Meaning: \"${question.translation}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted
                        )
                    }
                }

                if (!hasAnswered) {
                    GameHintButton(
                        hint = hintFor(uiState.questions.getOrNull(uiState.currentIndex)?.targetVocab, HintLanguages.Both),
                        revealed = uiState.hintRevealed,
                        onReveal = { viewModel.revealHint() }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    question.options.forEach { option ->
                        val state = when {
                            !hasAnswered -> GameOptionState.Idle
                            option == question.correctAnswer -> GameOptionState.Correct
                            option == uiState.selectedAnswer -> GameOptionState.Wrong
                            else -> GameOptionState.Idle
                        }
                        GameOptionRow(
                            label = option,
                            state = state,
                            enabled = !hasAnswered,
                            onClick = { viewModel.submitAnswer(option) }
                        )
                    }
                }
            }

            if (hasAnswered) {
                GameAnswerFeedback(
                    isCorrect = uiState.selectedAnswer == question.correctAnswer,
                    correctAnswer = question.correctAnswer,
                    word = question.targetVocab,
                    onContinue = { viewModel.nextQuestion() }
                )
            }
        }
        }
    )
}
