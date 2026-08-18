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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordMatchGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: WordMatchViewModel = hiltViewModel()
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
                        "Word Match",
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
                accentColor = VocabCardEnd,
                onBack = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        if (uiState.isGameOver) {
            GameOverView(
                score = uiState.score,
                total = uiState.totalQuestions,
                xpEarned = uiState.finalXp,
                starsEarned = uiState.starsEarned,
                onFinish = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        val roundNum = uiState.currentQuestionIndex + 1
        val hasAnswered = uiState.selectedOption != null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                            text = "Match Kasiguranin Word:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSubtleGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.currentWord?.kasiguranin ?: "",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = TextHeadingBlack,
                            fontSize = 32.sp
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.options.forEach { option ->
                        val correctAnswer = uiState.currentWord?.tagalog
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
}
