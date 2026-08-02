package com.kasiguru.ui.screens.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioQuizGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: AudioQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Quiz") },
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
                CircularProgressIndicator(color = Accent)
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
                    color = Accent
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            KasiGuruProgressBar(
                progress = (uiState.currentQuestionIndex.toFloat() / 5f),
                gradientColors = listOf(Accent, AccentLight),
                height = 8.dp
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Audio Player Button
            val scale by if (uiState.isPlayingAudio) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
            } else {
                remember { mutableFloatStateOf(1f) }
            }

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (uiState.isPlayingAudio) Accent else DarkSurfaceVariant)
                    .clickable(enabled = !uiState.isPlayingAudio) { viewModel.playAudio() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = "Play Pronunciation",
                    tint = if (uiState.isPlayingAudio) TextWhite else Accent,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tap to listen",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Options
            uiState.options.forEach { option ->
                val isSelected = option == uiState.selectedOption
                val isCorrectAnswer = option == uiState.currentWord?.kasiguranin
                
                val backgroundColor = when {
                    !isSelected && uiState.selectedOption != null && isCorrectAnswer -> Success.copy(alpha = 0.2f)
                    isSelected && uiState.isCorrect == true -> Success
                    isSelected && uiState.isCorrect == false -> Error
                    else -> MaterialTheme.colorScheme.surface
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
                            color = if (isSelected || (!isSelected && uiState.selectedOption != null && isCorrectAnswer)) TextWhite else MaterialTheme.colorScheme.onSurface,
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
