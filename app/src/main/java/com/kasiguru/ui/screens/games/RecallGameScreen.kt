package com.kasiguru.ui.screens.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.GameAnswerFeedback
import com.kasiguru.ui.components.GameHeader
import com.kasiguru.ui.components.GameHintButton
import com.kasiguru.ui.components.HintLanguages
import com.kasiguru.ui.components.hintFor
import com.kasiguru.ui.components.GameOverView
import com.kasiguru.ui.components.GameUnavailableState
import com.kasiguru.ui.components.RecallAnswerField
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.rememberGameExitGuard
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.KasiguraninHeadword
import com.kasiguru.ui.theme.MiniGamesCardEnd
import com.kasiguru.ui.theme.MiniGamesCardStart
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet
import com.kasiguru.util.RecallMatch

/**
 * Word Recall — meaning in, Kasiguranin out.
 *
 * Every other exercise in the app is recognition: the answer is already on screen and the learner
 * picks it. This is the one screen that asks them to produce the word, so the layout gives the
 * meaning the weight a headword normally gets and puts nothing else in front of them — no options
 * to eliminate against, and nothing that shows the spelling before they commit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecallGameScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNextLevel: ((Int) -> Unit)? = null,
    viewModel: RecallGameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exitGuard = rememberGameExitGuard(
        active = !uiState.isLoading && !uiState.isGameOver && !uiState.isUnavailable,
        onExit = onNavigateBack
    )

    GroundScaffold(
        title = "Word Recall",
        onBack = exitGuard,
        // Close, not back: this quits a round in progress, and exitGuard asks before discarding.
        navIcon = Iconsax.CloseCircle,
        // No texture while playing — the exercise is the only thing that should read as content.
        pattern = GroundPattern.None,
        compactTitle = true,
        content = {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Violet)
                }
                return@GroundScaffold
            }

            if (uiState.isUnavailable) {
                GameUnavailableState(
                    accentColor = MiniGamesCardEnd,
                    onBack = onNavigateBack,
                    modifier = Modifier
                )
                return@GroundScaffold
            }

            if (uiState.isGameOver) {
                GameOverView(
                    score = uiState.score,
                    total = uiState.totalQuestions,
                    xpEarned = uiState.finalXp,
                    starsEarned = uiState.starsEarned,
                    reviewItems = uiState.reviewItems,
                    onFinish = onNavigateBack,
                    onNextLevel = if (uiState.nextLevel != null && onNavigateToNextLevel != null) {
                        { onNavigateToNextLevel(uiState.nextLevel!!) }
                    } else null,
                    modifier = Modifier
                )
                return@GroundScaffold
            }

            val questionNum = uiState.currentQuestionIndex + 1
            val word = uiState.currentWord

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Space.gutter),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    GameHeader(
                        label = "Word $questionNum/${uiState.totalQuestions}",
                        progress = questionNum.toFloat() / uiState.totalQuestions.toFloat(),
                        score = uiState.score,
                        accentStart = MiniGamesCardStart,
                        accentEnd = MiniGamesCardEnd
                    )

                    Spacer(Modifier.height(Space.lg))

                    Text(
                        text = "Write this in Kasiguranin",
                        style = MaterialTheme.typography.labelLarge,
                        color = Muted,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(Space.xs))

                    Text(
                        text = uiState.promptMeaning,
                        style = KasiguraninHeadword,
                        color = Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(Space.lg))

                    RecallAnswerField(
                        value = uiState.typedAnswer,
                        enabled = !uiState.hasAnswered,
                        onValueChange = viewModel::updateTypedAnswer,
                        onSubmit = viewModel::submit
                    )

                    // Safe in both languages here: the answer is the Kasiguranin headword, which
                    // neither definition contains.
                    if (!uiState.hasAnswered) {
                        Spacer(Modifier.height(Space.xs))
                        GameHintButton(
                            hint = hintFor(word, HintLanguages.Both),
                            revealed = uiState.hintRevealed,
                            onReveal = { viewModel.revealHint() }
                        )
                    }
                }

                if (uiState.hasAnswered && word != null) {
                    val match = uiState.match
                    GameAnswerFeedback(
                        isCorrect = match != RecallMatch.Wrong,
                        correctAnswer = word.kasiguranin,
                        word = word,
                        // A near miss is its own verdict. Calling it "Correct" and moving on would
                        // let the misspelling harden; calling it wrong would deny a real retrieval.
                        headline = if (match == RecallMatch.Close) "Almost" else null,
                        correction = if (match == RecallMatch.Close) {
                            "It is spelled ${word.kasiguranin}"
                        } else {
                            null
                        },
                        onContinue = { viewModel.nextQuestion() }
                    )
                } else {
                    ClayButton(
                        label = "Check",
                        onClick = viewModel::submit,
                        enabled = uiState.typedAnswer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}
