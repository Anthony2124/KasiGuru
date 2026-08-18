package com.kasiguru.ui.screens.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
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
import com.kasiguru.util.audio.AudioPlayerManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioQuizGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: AudioQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayerManager(context) }
    var isPlaying by remember { mutableStateOf(false) }
    val exitGuard = rememberGameExitGuard(
        active = !uiState.isLoading && !uiState.isGameOver && !uiState.isUnavailable,
        onExit = onNavigateBack
    )

    DisposableEffect(Unit) {
        onDispose { audioPlayer.stopAudio() }
    }

    // Plays automatically on every new question, matching the pattern every other audio control in
    // the app already follows (Lesson Player, Flashcards, the Dictionary).
    LaunchedEffect(uiState.currentWord) {
        val word = uiState.currentWord ?: return@LaunchedEffect
        isPlaying = true
        audioPlayer.playAudio(word.kasiguranin, word.audioFileName)
        delay(700)
        isPlaying = false
    }

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
                accentColor = MiniGamesCardEnd,
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

        val questionNum = uiState.currentQuestionIndex + 1
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
                    label = "Question $questionNum/${uiState.totalQuestions}",
                    progress = questionNum.toFloat() / uiState.totalQuestions.toFloat(),
                    score = uiState.score,
                    accentStart = MiniGamesCardStart,
                    accentEnd = MiniGamesCardEnd
                )

                // Audio Player Circle Button
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isPlaying) 1.15f else 1f,
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
                            .clickable {
                                val word = uiState.currentWord ?: return@clickable
                                isPlaying = true
                                audioPlayer.playAudio(word.kasiguranin, word.audioFileName)
                            },
                        shape = CircleShape,
                        color = MiniGamesCardStart,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = Iconsax.VolumeHigh),
                                contentDescription = "Play the word again",
                                tint = TextHeadingBlack,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Tap to listen again & select the correct word",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSubtleGray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

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
}
