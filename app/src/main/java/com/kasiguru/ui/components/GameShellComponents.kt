package com.kasiguru.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.theme.Error
import com.kasiguru.ui.theme.GreenDeep
import com.kasiguru.ui.theme.GreenTint
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.RedDeep
import com.kasiguru.ui.theme.RedTint
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Success
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted

/**
 * Shared shell for the six mini-games. Before this, each game hand-rolled its own copy of the
 * progress/round/score header and the answer-option list — five to six near-identical
 * implementations of the same job, each free to drift from the others. One shared set here instead.
 */

/** Progress bar, "Round/Question X/Y," and score chip — was duplicated in every game screen. */
@Composable
fun GameHeader(
    label: String,
    progress: Float,
    score: Int,
    accentStart: Color,
    accentEnd: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        KasiGuruProgressBar(progress = progress, gradientColors = listOf(accentStart, accentEnd))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Surface(shape = RoundedCornerShape(16.dp), color = accentStart) {
                Text(
                    text = "Score: $score",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            }
        }
    }
}

/** An answer option's revealed state. Colour never carries this alone — see [GameOptionRow]. */
enum class GameOptionState { Idle, Wrong, Correct }

/**
 * One answer choice. Correct and wrong both get an icon alongside the colour fill, so the result
 * survives a colour-blind reading — the five games previously indicated a wrong pick by colour alone.
 */
@Composable
fun GameOptionRow(
    label: String,
    state: GameOptionState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (state) {
        GameOptionState.Correct -> Success.copy(alpha = 0.18f)
        GameOptionState.Wrong -> Error.copy(alpha = 0.18f)
        GameOptionState.Idle -> MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        shadowElevation = if (state == GameOptionState.Idle) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier.weight(1f)
            )
            when (state) {
                GameOptionState.Correct -> Icon(
                    painter = painterResource(id = Iconsax.TickCircle),
                    contentDescription = "Correct",
                    tint = Success,
                    modifier = Modifier.size(22.dp)
                )
                GameOptionState.Wrong -> Icon(
                    painter = painterResource(id = Iconsax.CloseCircle),
                    contentDescription = "Your answer, incorrect",
                    tint = Error,
                    modifier = Modifier.size(22.dp)
                )
                GameOptionState.Idle -> {}
            }
        }
    }
}

/**
 * Replaces the fixed-timer auto-advance every game used to force (1.5–2.6s, no way to read the
 * feedback first, and inconsistent with Lesson Player's own tap-to-continue in the same app).
 */
@Composable
fun GameContinueButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ClayButton(label = "Continue", onClick = onClick, modifier = modifier.fillMaxWidth())
}

/**
 * The feedback panel every mini-game now shares, ported from Lesson Player - the layer the games
 * never had. A wrong answer previously produced one option tinted red and nothing else: no
 * correction, no way to learn from it.
 *
 * Correctness is carried by icon, heading and colour together, so it never depends on colour
 * alone. [word] is optional because not every game exercise is anchored to one vocabulary entry
 * (e.g. Sentence Order works over a full sentence) - when present, its example sentence becomes a
 * teaching line; when absent, the panel still carries the verdict and the correct answer.
 */
@Composable
fun GameAnswerFeedback(
    isCorrect: Boolean,
    correctAnswer: String,
    onContinue: () -> Unit,
    word: VocabularyEntity? = null,
    modifier: Modifier = Modifier
) {
    val tint = if (isCorrect) GreenTint else RedTint
    val accent = if (isCorrect) GreenDeep else RedDeep

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.sheetTop)
            .background(tint)
            .navigationBarsPadding()
            .padding(Space.gutter)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(
                    id = if (isCorrect) Iconsax.TickCircle else Iconsax.InfoCircle
                ),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(Space.xs))
            Text(
                text = if (isCorrect) "Correct" else "Not quite",
                style = MaterialTheme.typography.headlineSmall,
                color = accent
            )
        }

        if (!isCorrect) {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = "Answer: $correctAnswer",
                style = MaterialTheme.typography.bodyLarge,
                color = Ink
            )
        }

        if (word != null && word.exampleSentence.isNotBlank()) {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = word.exampleSentence,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink
            )
            if (word.exampleTranslation.isNotBlank()) {
                Text(
                    text = word.exampleTranslation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }

        Spacer(Modifier.height(Space.md))
        ClayButton(
            label = if (isCorrect) "Continue" else "Got it",
            onClick = onContinue,
            tone = if (isCorrect) ClayButtonTone.Primary else ClayButtonTone.Reward,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Guards a mid-round exit — system/gesture back and the top bar's back icon both used to leave
 * instantly with no confirmation, silently discarding the round's progress (there was no
 * `BackHandler` in any of the six games). Returns the click handler to wire onto the top bar's back
 * icon; system back is intercepted automatically while [active] is true.
 */
@Composable
fun rememberGameExitGuard(active: Boolean, onExit: () -> Unit): () -> Unit {
    var showConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = active) { showConfirm = true }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Quit this game?") },
            text = { Text("Your progress in this round won't be saved.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onExit() }) {
                    Text("Quit", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Keep playing") }
            }
        )
    }

    return { if (active) showConfirm = true else onExit() }
}

/**
 * A real "not enough vocabulary yet" state, not a fabricated "Score: 0" loss. Aspect Builder already
 * had this; the other games faked a game-over instead — this is that same pattern, shared.
 */
@Composable
fun GameUnavailableState(
    accentColor: Color,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Not enough words for this yet",
    message: String = "We're still building out the vocabulary this game needs. Check back after the next dictionary update!"
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = Iconsax.Flash),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        ClayButton(label = "Back to games", onClick = onBack)
    }
}
