package com.kasiguru.ui.screens.games

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.*

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
                title = { Text("Fill in the Blank") },
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
                CircularProgressIndicator(color = Primary)
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
                    color = Primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            KasiGuruProgressBar(
                progress = (uiState.currentQuestionIndex.toFloat() / 5f),
                gradientColors = listOf(Primary, PrimaryLight),
                height = 8.dp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // The Sentence
            Text(
                text = "Complete the sentence:",
                style = MaterialTheme.typography.titleMedium,
                color = TextDarkGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val sentence = uiState.sentenceTemplate
            val parts = sentence.split("____")
            
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = DarkSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buildAnnotatedString {
                        if (parts.isNotEmpty()) {
                            append(parts[0])
                            withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {
                                append(uiState.selectedOption ?: "_______")
                            }
                            if (parts.size > 1) {
                                append(parts[1])
                            }
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
            
            Text(
                text = "Root verb: ${uiState.currentVerb?.rootForm} (${uiState.currentVerb?.english})",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Options Grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.options.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowOptions.forEach { option ->
                            val isSelected = option == uiState.selectedOption
                            val isCorrectAnswer = option == uiState.correctAnswer
                            
                            val backgroundColor = when {
                                !isSelected && uiState.selectedOption != null && isCorrectAnswer -> Success.copy(alpha = 0.2f)
                                isSelected && uiState.isCorrect == true -> Success
                                isSelected && uiState.isCorrect == false -> Error
                                else -> MaterialTheme.colorScheme.surface
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(enabled = uiState.selectedOption == null) {
                                        viewModel.selectOption(option)
                                    },
                                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected || (uiState.selectedOption != null && isCorrectAnswer)) TextWhite else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
