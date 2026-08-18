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
import com.kasiguru.ui.components.GameContinueButton
import com.kasiguru.ui.components.GameHeader
import com.kasiguru.ui.components.GameOptionRow
import com.kasiguru.ui.components.GameOptionState
import com.kasiguru.ui.components.GameOverView
import com.kasiguru.ui.components.GameUnavailableState
import com.kasiguru.ui.components.rememberGameExitGuard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Aspect Builder",
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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet)
            }
            return@Scaffold
        }

        if (uiState.isUnavailable) {
            GameUnavailableState(
                accentColor = HeroCardStart,
                onBack = onNavigateBack,
                modifier = Modifier.padding(padding),
                title = "Aspect Builder is coming soon",
                message = "We are documenting the verb aspect forms with our language experts. Check back after the next dictionary update!"
            )
            return@Scaffold
        }

        if (uiState.isGameOver) {
            GameOverView(
                score = uiState.score,
                total = uiState.totalQuestions,
                xpEarned = uiState.xpEarned,
                starsEarned = uiState.starsEarned,
                onFinish = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        val question = uiState.questions.getOrNull(uiState.currentIndex) ?: return@Scaffold
        val qIndex = uiState.currentIndex + 1
        val hasAnswered = uiState.selectedAnswer != null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
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
                            color = TextSubtleGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Form required: ${question.targetAspect}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                        Text(
                            text = "Meaning: \"${question.translation}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSubtleGray
                        )
                    }
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
                GameContinueButton(onClick = { viewModel.nextQuestion() })
            }
        }
    }
}
