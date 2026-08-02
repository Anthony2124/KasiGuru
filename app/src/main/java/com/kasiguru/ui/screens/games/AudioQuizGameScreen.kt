package com.kasiguru.ui.screens.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
fun AudioQuizGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: AudioQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Audio Quiz",
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
                CircularProgressIndicator(color = MiniGamesCardEnd)
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

        val questionNum = uiState.currentQuestionIndex + 1

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
                    progress = questionNum.toFloat() / 5f,
                    gradientColors = listOf(MiniGamesCardStart, MiniGamesCardEnd)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question $questionNum/5",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MiniGamesCardStart
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

            // Audio Player Circle Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (uiState.isPlayingAudio) 1.15f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseScale"
                )

                Surface(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clickable { viewModel.playAudio() },
                    shape = CircleShape,
                    color = MiniGamesCardStart,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_volume_high),
                            contentDescription = "Play Audio",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Text(
                text = "Tap to listen & select the correct word",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSubtleGray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Option Cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.options.forEach { option ->
                    val isSelected = uiState.selectedOption == option
                    val isCorrect = isSelected && option == uiState.currentWord?.kasiguranin
                    val isWrong = isSelected && !isCorrect

                    val optionBgColor = when {
                        isCorrect -> QuestsCardStart
                        isWrong -> Error.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = uiState.selectedOption == null) {
                                viewModel.selectOption(option)
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
