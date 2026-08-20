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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.GameContinueButton
import com.kasiguru.ui.components.GameHeader
import com.kasiguru.ui.components.GameOptionRow
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
fun ReverseMatchGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReverseMatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exitGuard = rememberGameExitGuard(
        active = !uiState.isLoading && !uiState.isGameOver && !uiState.isUnavailable,
        onExit = onNavigateBack
    )

    GroundScaffold(
        title = "Reverse Match",
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
                accentColor = VocabCardEnd,
                onBack = onNavigateBack,
                modifier = Modifier
            )
            return@GroundScaffold
        }

        if (uiState.isGameOver) {
            GameOverView(
                score = uiState.score,
                total = uiState.totalQuestions,
                xpEarned = uiState.finalXp,
                starsEarned = uiState.starsEarned,
                onFinish = onNavigateBack,
                modifier = Modifier
            )
            return@GroundScaffold
        }

        val roundNum = uiState.currentQuestionIndex + 1
        val hasAnswered = uiState.selectedOption != null

        Column(
            modifier = Modifier
                .fillMaxSize()
                
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                GameHeader(
                    label = "Round $roundNum/${uiState.totalQuestions}",
                    progress = roundNum.toFloat() / uiState.totalQuestions.toFloat(),
                    score = uiState.score,
                    accentStart = VocabCardStart,
                    accentEnd = VocabCardEnd
                )

                // Prompt Card: Tagalog meaning, English hint
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Match the Tagalog Word:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Muted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.currentWord?.tagalog ?: "",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = Ink,
                            fontSize = 32.sp
                        )
                        if (!uiState.currentWord?.english.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "(${uiState.currentWord?.english})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Muted
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.options.forEach { option ->
                        val correctAnswer = uiState.currentWord?.kasiguranin
                        val state = when {
                            !hasAnswered -> GameOptionState.Idle
                            option == correctAnswer -> GameOptionState.Correct
                            option == uiState.selectedOption -> GameOptionState.Wrong
                            else -> GameOptionState.Idle
                        }
                        GameOptionRow(
                            label = option,
                            state = state,
                            enabled = !hasAnswered,
                            onClick = { viewModel.selectOption(option) }
                        )
                    }
                }
            }

            if (hasAnswered) {
                GameContinueButton(onClick = { viewModel.nextQuestion() })
            }
        }
        }
    )
}
