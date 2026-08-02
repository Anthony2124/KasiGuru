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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.R
import com.kasiguru.ui.components.GameOverView
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AspectBuilderGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: AspectBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
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

        if (uiState.isGameOver) {
            GameOverView(
                score = uiState.score,
                total = uiState.questions.size,
                xpEarned = uiState.xpEarned,
                onFinish = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        val question = uiState.questions.getOrNull(uiState.currentIndex) ?: return@Scaffold
        val qIndex = uiState.currentIndex + 1

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress
            Column {
                KasiGuruProgressBar(
                    progress = qIndex.toFloat() / uiState.questions.size.toFloat(),
                    gradientColors = listOf(HeroCardStart, HeroCardEnd)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question $qIndex/${uiState.questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HeroCardStart
                    ) {
                        Text(
                            text = "Score: ${uiState.score}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                    }
                }
            }

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

            // Option Cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                question.options.forEach { option ->
                    val isSelected = uiState.selectedAnswer == option
                    val isCorrect = isSelected && option == question.correctAnswer
                    val isWrong = isSelected && option != question.correctAnswer

                    val optionBgColor = when {
                        isCorrect -> QuestsCardStart
                        isWrong -> Error.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = uiState.selectedAnswer == null) {
                                viewModel.submitAnswer(option)
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = optionBgColor,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextHeadingBlack
                            )
                            if (isCorrect) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_tick_circle),
                                    contentDescription = "Correct",
                                    tint = Success,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
