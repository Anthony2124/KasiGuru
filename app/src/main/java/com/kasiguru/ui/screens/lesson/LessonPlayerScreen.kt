package com.kasiguru.ui.screens.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.domain.lesson.Exercise
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.ClayFab
import com.kasiguru.ui.components.clay.TagChip
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.GreenDeep
import com.kasiguru.ui.theme.GreenTint
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.KasiguraninHeadword
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Red
import com.kasiguru.ui.theme.RedDeep
import com.kasiguru.ui.theme.RedTint
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.TrackNeutral
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletDeep
import com.kasiguru.ui.theme.VioletTint
import com.kasiguru.util.audio.AudioPlayerManager

/**
 * One lesson, one exercise at a time.
 *
 * Immersive on purpose: no top app bar, no bottom nav, nothing but the question and one action. The
 * old game screens carried full app chrome around a quiz, which made a five-second answer feel like a
 * detour through the app rather than a step in a lesson.
 */
@Composable
fun LessonPlayerScreen(
    onExit: () -> Unit,
    onFinished: () -> Unit,
    viewModel: LessonPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val audioPlayer = remember { AudioPlayerManager(context) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { audioPlayer.stopAudio() }
    }

    // Feedback is felt before it is read.
    LaunchedEffect(uiState.isCorrect) {
        when (uiState.isCorrect) {
            true -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            false -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            null -> Unit
        }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize().background(Ground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Violet)
        }
        return
    }

    if (uiState.isComplete) {
        LessonCompleteScreen(
            xpAwarded = uiState.xpAwarded,
            accuracy = uiState.accuracy,
            words = uiState.wordsCovered,
            onContinue = onFinished
        )
        return
    }

    val exercise = uiState.current ?: return

    var showExitConfirm by remember { mutableStateOf(false) }
    BackHandler { showExitConfirm = true }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Leave this lesson?") },
            text = { Text("Your progress in this lesson won't be saved.") },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false; onExit() }) {
                    Text("Leave", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Keep going") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .statusBarsPadding()
    ) {
        // ── Header: exit and progress ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = Iconsax.ArrowLeft),
                contentDescription = "Leave lesson",
                tint = Muted,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = { showExitConfirm = true })
            )
            Spacer(Modifier.width(Space.sm))
            LessonProgressBar(
                fraction = uiState.progressFraction,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Question ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            Spacer(Modifier.height(Space.md))
            Text(
                text = exercise.instruction,
                style = MaterialTheme.typography.titleMedium,
                color = Muted
            )
            Spacer(Modifier.height(Space.md))

            ExercisePrompt(
                exercise = exercise,
                onPlayAudio = {
                    audioPlayer.playAudio(exercise.word.kasiguranin, exercise.word.audioFileName)
                }
            )

            Spacer(Modifier.height(Space.lg))

            exercise.options.forEach { option ->
                AnswerOption(
                    label = option,
                    isSelected = uiState.selectedOption == option,
                    isRevealedCorrect = uiState.hasAnswered && option == exercise.answer,
                    isRevealedWrong = uiState.hasAnswered &&
                        uiState.selectedOption == option &&
                        option != exercise.answer,
                    enabled = !uiState.hasAnswered,
                    onClick = { viewModel.selectOption(option) }
                )
                Spacer(Modifier.height(Space.sm))
            }
            Spacer(Modifier.height(Space.lg))
        }

        // ── Action area: the check button, replaced by feedback once answered ──
        LessonActionArea(
            hasAnswered = uiState.hasAnswered,
            isCorrect = uiState.isCorrect == true,
            canCheck = uiState.selectedOption != null,
            correctAnswer = exercise.answer,
            word = exercise.word,
            onCheck = viewModel::check,
            onContinue = viewModel::advance
        )
    }
}

/**
 * Pulled out of [LessonPlayerScreen] so `AnimatedVisibility` resolves to the plain overload rather
 * than the `ColumnScope` extension the enclosing column would otherwise supply.
 */
@Composable
private fun LessonActionArea(
    hasAnswered: Boolean,
    isCorrect: Boolean,
    canCheck: Boolean,
    correctAnswer: String,
    word: com.kasiguru.data.local.entity.VocabularyEntity,
    onCheck: () -> Unit,
    onContinue: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = !hasAnswered,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(100))
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .navigationBarsPadding()
                    .padding(Space.gutter)
            ) {
                ClayButton(
                    label = "Check",
                    onClick = onCheck,
                    enabled = canCheck,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        AnimatedVisibility(
            visible = hasAnswered,
            enter = slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(220)),
            exit = slideOutVertically(tween(140)) { it } + fadeOut(tween(140))
        ) {
            AnswerFeedback(
                isCorrect = isCorrect,
                correctAnswer = correctAnswer,
                word = word,
                onContinue = onContinue
            )
        }
    }
}

/**
 * The feedback panel — the layer the old mini-games never had. A wrong answer there produced one line
 * of text below the options and no way to learn from it.
 *
 * Correctness is carried by icon, heading and colour together, so it never depends on colour alone.
 */
@Composable
private fun AnswerFeedback(
    isCorrect: Boolean,
    correctAnswer: String,
    word: com.kasiguru.data.local.entity.VocabularyEntity,
    onContinue: () -> Unit
) {
    val tint = if (isCorrect) GreenTint else RedTint
    val accent = if (isCorrect) GreenDeep else RedDeep

    Column(
        modifier = Modifier
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

        // A teaching line, not just a verdict: the example sentence is the only place the learner
        // sees the word doing a job.
        if (word.exampleSentence.isNotBlank()) {
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

/** The prompt, which differs by exercise type. */
@Composable
private fun ExercisePrompt(exercise: Exercise, onPlayAudio: () -> Unit) {
    when (exercise) {
        is Exercise.ChooseTranslation -> {
            Text(
                text = exercise.prompt,
                style = KasiguraninHeadword,
                color = Ink
            )
            if (exercise.promptIsKasiguranin && exercise.word.ipaNotation.isNotBlank()) {
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = "[${exercise.word.ipaNotation}]",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Faint
                )
            }
        }

        is Exercise.ListenAndChoose -> {
            // Audio is the whole prompt, so the control is large and centred rather than an
            // afterthought beside text.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ClayFab(
                    onClick = onPlayAudio,
                    contentDescription = "Play the word again",
                    size = 92.dp
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.VolumeHigh),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        is Exercise.FillBlank -> {
            Text(
                text = exercise.sentenceWithBlank,
                style = MaterialTheme.typography.headlineMedium,
                color = Ink
            )
            if (exercise.translation.isNotBlank()) {
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = exercise.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
        }

        is Exercise.ChooseAspect -> {
            Text(text = exercise.word.kasiguranin, style = KasiguraninHeadword, color = Ink)
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xxs)) {
                TagChip(label = "verb")
                TagChip(label = exercise.aspectLabel)
            }
        }
    }
}

/**
 * Answer option. Selected, correct and wrong are three visually distinct states, and the revealed
 * states also change the border weight so the difference survives a colour-blind reading.
 */
@Composable
private fun AnswerOption(
    label: String,
    isSelected: Boolean,
    isRevealedCorrect: Boolean,
    isRevealedWrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        isRevealedCorrect -> GreenTint
        isRevealedWrong -> RedTint
        isSelected -> VioletTint
        else -> Surface
    }
    val borderColor = when {
        isRevealedCorrect -> Green
        isRevealedWrong -> Red
        isSelected -> Violet
        else -> TrackNeutral
    }
    val borderWidth = if (isSelected || isRevealedCorrect || isRevealedWrong) 2.dp else 1.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.tile)
            .background(background)
            .border(borderWidth, borderColor, Shapes.tile)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = Space.md, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        if (isRevealedCorrect) {
            Icon(
                painter = painterResource(id = Iconsax.TickCircle),
                contentDescription = "Correct answer",
                tint = Green,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LessonProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "LessonProgress"
    )
    Box(
        modifier = modifier
            .height(10.dp)
            .clip(Shapes.pill)
            .background(TrackNeutral)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(10.dp)
                .clip(Shapes.pill)
                .background(Violet)
        )
    }
}
