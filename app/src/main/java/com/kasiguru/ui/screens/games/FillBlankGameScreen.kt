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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.GameOverView
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillBlankGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: FillBlankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Fill in the Blank",
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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = QuestsCardEnd)
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

        val questionNumber = uiState.currentQuestionIndex + 1

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
                    progress = questionNumber.toFloat() / 5f,
                    gradientColors = listOf(QuestsCardStart, QuestsCardEnd)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question $questionNumber/5",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = QuestsCardStart
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

            // Sentence Card
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
                        text = "Complete the Sentence:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSubtleGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val rawTemplate = uiState.sentenceTemplate
                    val parts = rawTemplate.split("___")
                    val filledText = buildAnnotatedString {
                        append(parts.getOrElse(0) { "" })
                        withStyle(style = SpanStyle(color = TextHeadingBlack, fontWeight = FontWeight.Bold)) {
                            append(uiState.selectedOption ?: " _____ ")
                        }
                        append(parts.getOrElse(1) { "" })
                    }

                    Text(
                        text = filledText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextHeadingBlack,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    )

                    if (uiState.currentVerb != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "(${uiState.currentVerb?.english})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSubtleGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Option Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.options.forEach { option ->
                    val isSelected = uiState.selectedOption == option
                    val isCorrect = isSelected && option == uiState.correctAnswer
                    val isWrong = isSelected && option != uiState.correctAnswer

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
                                    painter = painterResource(id = Iconsax.TickCircle),
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
