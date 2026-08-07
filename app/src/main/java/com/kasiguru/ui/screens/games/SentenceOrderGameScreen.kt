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
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.GameOverView
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SentenceOrderGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: SentenceOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sentence Order",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        if (uiState.questions.isEmpty() && !uiState.isGameFinished) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StoriesCardStart)
            }
            return@Scaffold
        }

        if (uiState.isGameFinished) {
            GameOverView(
                score = uiState.score,
                total = uiState.totalQuestions,
                xpEarned = uiState.score * 10,
                starsEarned = uiState.starsEarned,
                onFinish = onNavigateBack,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        val question = uiState.questions.getOrNull(uiState.currentQuestionIndex) ?: return@Scaffold
        val qIndex = uiState.currentQuestionIndex + 1

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
                    progress = qIndex.toFloat() / uiState.totalQuestions.toFloat(),
                    gradientColors = listOf(StoriesCardStart, StoriesCardEnd)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sentence $qIndex/${uiState.totalQuestions}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StoriesCardStart
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
                        color = TextSubtleGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${question.englishSentence}\"",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Selected Words Slot Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(24.dp),
                color = StoriesCardStart.copy(alpha = 0.3f)
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.constructedWords.isEmpty()) {
                        Text(
                            text = "Tap word blocks below in predicate-initial order",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSubtleGray
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.constructedWords.forEach { word ->
                                FilterChip(
                                    selected = true,
                                    onClick = { viewModel.deselectWord(word) },
                                    label = { Text(word, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TextHeadingBlack,
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
            Column {
                Text(
                    text = "Word Bank",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSubtleGray,
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
                                labelColor = TextHeadingBlack
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // Check Answer Button
            Button(
                onClick = { viewModel.checkAnswer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StoriesCardStart),
                shape = RoundedCornerShape(20.dp),
                enabled = uiState.constructedWords.isNotEmpty()
            ) {
                Text(
                    text = "Check Sentence Order",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHeadingBlack,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
