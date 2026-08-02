package com.kasiguru.ui.screens.games

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordMatchGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: WordMatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Match") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Secondary)
            }
            return@Scaffold
        }

        if (uiState.isGameOver) {
            GameOverView(
                score = uiState.score,
                total = 5,
                xpEarned = uiState.finalXp,
                onFinish = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${uiState.currentQuestionIndex + 1} of 5",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextGray
                )
                Text(
                    text = "Score: ${uiState.score}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Secondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            KasiGuruProgressBar(
                progress = (uiState.currentQuestionIndex.toFloat() / 5f),
                gradientColors = listOf(Secondary, SecondaryLight),
                height = 8.dp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // The Word to Match
            Text(
                text = "What is the Tagalog meaning of:",
                style = MaterialTheme.typography.titleMedium,
                color = TextDarkGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PrimaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.currentWord?.kasiguranin ?: "",
                    style = MaterialTheme.typography.displayMedium,
                    color = PrimaryDark,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Options
            uiState.options.forEach { option ->
                val isSelected = option == uiState.selectedOption
                val isCorrectAnswer = option == uiState.currentWord?.tagalog
                
                val backgroundColor = when {
                    !isSelected && uiState.selectedOption != null && isCorrectAnswer -> Success.copy(alpha = 0.2f)
                    isSelected && uiState.isCorrect == true -> Success
                    isSelected && uiState.isCorrect == false -> Error
                    else -> MaterialTheme.colorScheme.surface
                }

                val contentColor = when {
                    isSelected -> TextWhite
                    !isSelected && uiState.selectedOption != null && isCorrectAnswer -> Success
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = uiState.selectedOption == null) {
                            viewModel.selectOption(option)
                        },
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.titleLarge,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = if (uiState.isCorrect == true) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                contentDescription = null,
                                tint = TextWhite
                            )
                        } else if (uiState.selectedOption != null && isCorrectAnswer) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Success
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverView(
    score: Int,
    total: Int,
    xpEarned: Int,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (score == total) "Perfect!" else "Good Job!",
            style = MaterialTheme.typography.displayMedium,
            color = if (score == total) XpGold else Primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "You scored $score out of $total",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(32.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = XpGold.copy(alpha = 0.2f)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "⭐", style = MaterialTheme.typography.displayMedium)
                Column {
                    Text(text = "XP Earned", style = MaterialTheme.typography.labelLarge, color = TextGray)
                    Text(text = "+$xpEarned", style = MaterialTheme.typography.headlineMedium, color = XpGold, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Back to Games", style = MaterialTheme.typography.titleMedium)
        }
    }
}
