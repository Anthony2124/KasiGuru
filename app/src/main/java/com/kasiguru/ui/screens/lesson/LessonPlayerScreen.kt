package com.kasiguru.ui.screens.lesson

import com.kasiguru.ui.theme.RedText
import com.kasiguru.ui.theme.GreenText
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.kasiguru.ui.theme.Scenery
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.LocalReducedMotion
import com.kasiguru.ui.components.SegmentedProgress
import com.kasiguru.ui.components.FeedbackPreferencesViewModel
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepPose
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.domain.lesson.Exercise
import com.kasiguru.ui.components.GameAnswerFeedback
import com.kasiguru.ui.components.RecallAnswerField
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.ClayFab
import com.kasiguru.ui.components.clay.TagChip
import com.kasiguru.ui.theme.Olive
import com.kasiguru.ui.theme.OnLime
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
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.LimeLip
import com.kasiguru.ui.theme.LimeTint
import com.kasiguru.util.audio.AudioPlayerManager
import com.kasiguru.ui.components.tapSounds

/**
 * One lesson, one exercise at a time.
 *
 * Immersive on purpose: no top app bar, no bottom nav, nothing but the question and one action. The
 * old game screens carried full app chrome around a quiz, which made a five-second answer feel like a
 * detour through the app rather than a step in a lesson.
 *
 * Top to bottom: quit, progress and the combo; the instruction as the heading; Jepjep asking the
 * prompt from a speech bubble; the answers; Check. Jepjep reacts to the verdict in place, so the
 * feedback panel carries no second mascot.
 */
@Composable
fun LessonPlayerScreen(
    onExit: () -> Unit,
    onFinished: (Boolean) -> Unit,
    feedbackPreferences: FeedbackPreferencesViewModel = hiltViewModel(),
    viewModel: LessonPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticsEnabled by feedbackPreferences.hapticsEnabled.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val audioPlayer = remember { AudioPlayerManager(context) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { audioPlayer.stopAudio() }
    }

    // Feedback is felt before it is read. The answer sound comes from GameAnswerFeedback.
    LaunchedEffect(uiState.isCorrect) {
        if (hapticsEnabled) when (uiState.isCorrect) {
            true -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            false -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            null -> Unit
        }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize().background(Ground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Lime)
        }
        return
    }

    if (uiState.isComplete) {
        LessonCompleteScreen(
            xpAwarded = uiState.xpAwarded,
            accuracy = uiState.accuracy,
            words = uiState.wordsCovered,
            onContinue = { onFinished(uiState.showStreakPage) }
        )
        return
    }

    val exercise = uiState.current ?: return

    var showExitConfirm by remember { mutableStateOf(false) }
    BackHandler { showExitConfirm = true }

    if (showExitConfirm) {
        AlertDialog(modifier = Modifier.tapSounds(), 
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Leave this lesson?") },
            text = { Text("Your progress in this lesson won't be saved.") },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false; onExit() }) {
                    Text("Leave", color = RedText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Keep going") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
    ) {
    // The section's scene, faded into the night behind the header: where in Casiguran this lesson
    // lives, without a strip of its own taking the space the question needs.
    Box(Modifier.fillMaxWidth().height(220.dp)) {
        Image(
            painterResource(Scenery.forSection(viewModel.sectionId).res), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.32f }
        )
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0f to Ground.copy(alpha = 0.35f),
                    1f to Ground
                )
            )
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ── Header: quit, progress and the combo ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Space.xs, end = Space.gutter, top = Space.xs, bottom = Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(Shapes.pill)
                    .clickable(onClickLabel = "Leave lesson", onClick = { showExitConfirm = true }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.CloseCircle),
                    contentDescription = "Leave lesson",
                    tint = Muted,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(Space.xs))
            SegmentedProgress(
                completed = uiState.solvedCount, total = uiState.totalExercises,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Space.sm))
            ComboCounter(combo = uiState.combo)
        }

        // ── Question ──
        //
        // The instruction, then Jepjep "asking" the prompt from a speech bubble, then the answers
        // directly above the Check button where the thumb already is. The old scenery strip and its
        // fixed "Take your time" line pushed the prompt halfway down the screen and said nothing
        // about the question.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Space.gutter)
        ) {
          Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
          ) {
            Spacer(Modifier.height(Space.sm))
            Text(
                text = exercise.instruction,
                style = MaterialTheme.typography.headlineSmall,
                color = Ink
            )
            Spacer(Modifier.height(Space.lg))

            if (exercise !is Exercise.MatchPairs) {
                PromptWithJepjep(
                    // One Jepjep on screen: he reacts here instead of again in the feedback panel.
                    pose = when (uiState.isCorrect) {
                        true -> JepjepPose.Encouraging
                        false -> JepjepPose.Confused
                        null -> if (exercise is Exercise.ListenAndChoose) JepjepPose.Listening else JepjepPose.Pointing
                    }
                ) {
                    ExercisePrompt(
                        exercise = exercise,
                        onPlayAudio = { audioPlayer.playWord(exercise.word) }
                    )
                }
            }
          }

          Spacer(Modifier.height(Space.md))

          // The answers sit directly above Check. Measured before the question region, so they keep
          // the room they need and the question scrolls if a long one runs short of space.
          Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
          ) {
            if (exercise is Exercise.TypeWord) {
                RecallAnswerField(
                    value = uiState.selectedOption.orEmpty(),
                    enabled = !uiState.hasAnswered,
                    onValueChange = viewModel::updateTypedAnswer,
                    onSubmit = viewModel::check
                )
            } else if (exercise is Exercise.SentenceBuild) {
                SentenceBuilder(
                    exercise = exercise,
                    enabled = !uiState.hasAnswered,
                    // The built sentence lands in the same slot a chosen option would, so
                    // checking and grading need no special case for this shape.
                    onSentenceChanged = viewModel::updateTypedAnswer
                )
            } else if (exercise is Exercise.MatchPairs) {
                MatchPairsBoard(
                    exercise = exercise,
                    enabled = !uiState.hasAnswered,
                    // Self-grading: finishing the board *is* the correct answer, so it fills the
                    // answer slot with what the grader expects rather than inventing a second
                    // path through checking.
                    onSolved = { viewModel.selectOption(exercise.answer) }
                )
            } else {
                // A 2 x 2 grid only when every option is a single short word; anything longer
                // reads as a list, one full-width row each, so a gloss never wraps mid-phrase.
                val grid = exercise.options.size == 4 && exercise.options.all { it.length <= 12 }
                val optionContent: @Composable (Int, String, Modifier) -> Unit = { index, option, modifier ->
                    AnswerOption(label = option, number = index + 1, isSelected = uiState.selectedOption == option,
                        isRevealedCorrect = uiState.hasAnswered && option == exercise.answer,
                        isRevealedWrong = uiState.hasAnswered && uiState.selectedOption == option && option != exercise.answer,
                        enabled = !uiState.hasAnswered, onClick = { viewModel.selectOption(option) }, modifier = modifier)
                }
                if (grid) {
                    exercise.options.chunked(2).forEachIndexed { row, options ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                            options.forEachIndexed { col, option -> optionContent(row * 2 + col, option, Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(Space.sm))
                    }
                } else {
                    exercise.options.forEachIndexed { index, option ->
                        optionContent(index, option, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(Space.sm))
                    }
                }
            }
            Spacer(Modifier.height(Space.xs))
          }
        }

        // ── Action area: the check button, replaced by feedback once answered ──
        LessonActionArea(
            hasAnswered = uiState.hasAnswered,
            isCorrect = uiState.isCorrect,
            // Blank input must not be submittable: for a typed prompt selectedOption holds the
            // live text, which is "" before the learner types anything rather than null.
            canCheck = !uiState.selectedOption.isNullOrBlank(),
            correctAnswer = exercise.answer,
            word = exercise.word,
            // What the learner actually chose, once it has been looked up. Arrives a moment after
            // the verdict rather than with it, so the panel is never waiting on a query.
            remediation = uiState.remediation,
            onCheck = viewModel::check,
            onContinue = viewModel::advance,
            onPlayAudio = { audioPlayer.playWord(exercise.word) }
        )
    }
    }
}

/**
 * Pulled out of [LessonPlayerScreen] so `AnimatedVisibility` resolves to the plain overload rather
 * than the `ColumnScope` extension the enclosing column would otherwise supply.
 */
@Composable
private fun LessonActionArea(
    hasAnswered: Boolean,
    isCorrect: Boolean?,
    canCheck: Boolean,
    correctAnswer: String,
    word: com.kasiguru.data.local.entity.VocabularyEntity,
    remediation: String?,
    onCheck: () -> Unit,
    onContinue: () -> Unit,
    onPlayAudio: () -> Unit
) {
    // The panel keeps the verdict it opened with while it slides away. By the time it exits the view
    // model has already cleared isCorrect and moved to the next exercise, so reading live values
    // there would flash the "wrong" state -- and the next question's answer -- on every Continue.
    val held = remember { HeldFeedback() }
    if (isCorrect != null) {
        held.content = FeedbackContent(isCorrect, correctAnswer, word, remediation)
    }

    Box(Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = !hasAnswered,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(100))
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Space.gutter)
                    .padding(top = Space.xs, bottom = Space.md)
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
                fadeIn(tween(if (LocalReducedMotion.current) 0 else 220)),
            exit = slideOutVertically(tween(140)) { it } + fadeOut(tween(140))
        ) {
            val shown = held.content ?: return@AnimatedVisibility
            GameAnswerFeedback(
                isCorrect = shown.isCorrect,
                correctAnswer = shown.correctAnswer,
                word = shown.word,
                correction = shown.remediation,
                onContinue = onContinue,
                onPlayAudio = onPlayAudio
            )
        }
    }
}

private data class FeedbackContent(
    val isCorrect: Boolean,
    val correctAnswer: String,
    val word: com.kasiguru.data.local.entity.VocabularyEntity,
    val remediation: String?
)

/** Plain holder rather than state: it only needs to outlive the exit animation, not trigger one. */
private class HeldFeedback {
    var content: FeedbackContent? = null
}

/** The prompt, which differs by exercise type. */
@Composable
private fun ExercisePrompt(exercise: Exercise, onPlayAudio: () -> Unit) {
    when (exercise) {
        is Exercise.TypeWord -> {
            // The meaning is the prompt; the Kasiguranin word is what the learner must produce, so
            // nothing on screen may show it before they commit an answer.
            Text(
                text = exercise.promptMeaning,
                style = MaterialTheme.typography.headlineSmall,
                color = Ink
            )
        }

        is Exercise.ChooseTranslation -> {
            Text(
                text = exercise.prompt,
                style = if (exercise.promptIsKasiguranin) KasiguraninHeadword
                else MaterialTheme.typography.headlineSmall,
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
                    size = 72.dp
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.VolumeHigh),
                        contentDescription = null,
                        tint = OnLime,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        is Exercise.FillBlank -> {
            Text(
                text = exercise.sentenceWithBlank,
                style = MaterialTheme.typography.headlineSmall,
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

        is Exercise.SentenceBuild -> {
            // The meaning is the prompt; the Kasiguranin is what the learner assembles below.
            Text(
                text = exercise.translation,
                style = MaterialTheme.typography.headlineSmall,
                color = Ink
            )
        }

        is Exercise.MatchPairs -> {
            // No prompt: the two columns below are the whole exercise, and a line of text above them
            // would only repeat the instruction already shown.
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
    number: Int,
    isSelected: Boolean,
    isRevealedCorrect: Boolean,
    isRevealedWrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = remember { Animatable(0f) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(isRevealedCorrect, isRevealedWrong) {
        if (!reduced && (isRevealedCorrect || isRevealedWrong)) {
            listOf(1f, -1f, .5f, 0f).forEach { motion.animateTo(it, tween(60)) }
        }
    }
    val background = when {
        isRevealedCorrect -> GreenTint
        isRevealedWrong -> RedTint
        isSelected -> Olive
        else -> Surface
    }
    val borderColor = when {
        isRevealedCorrect -> Green
        isRevealedWrong -> Red
        isSelected -> Lime
        else -> TrackNeutral
    }
    val borderWidth = if (isSelected || isRevealedCorrect || isRevealedWrong) 2.dp else 1.dp

    Row(
        modifier = modifier
            .heightIn(min = 60.dp)
            .graphicsLayer {
                translationX = if (isRevealedWrong) motion.value * 7.dp.toPx() else 0f
                scaleX = 1f + if (isRevealedCorrect) motion.value * .04f else 0f
                scaleY = scaleX
            }
            .clip(Shapes.tile)
            .background(background)
            .border(borderWidth, borderColor, Shapes.tile)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = Space.md, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A key in the corner, as on a quiz card: something to point at besides the words, and a
        // second cue for the selected state that does not rely on colour.
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(Shapes.chip)
                .background(if (isSelected || isRevealedCorrect || isRevealedWrong) borderColor else TrackNeutral),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected || isRevealedCorrect || isRevealedWrong) OnLime else Muted
            )
        }
        Spacer(Modifier.width(Space.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (isSelected && !isRevealedCorrect && !isRevealedWrong) OnCanopy else Ink,
            modifier = Modifier.weight(1f)
        )
        if (isRevealedCorrect) {
            Icon(
                painter = painterResource(id = Iconsax.TickCircle),
                contentDescription = "Correct answer",
                tint = GreenText,
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
                .background(Lime)
        )
    }
}

/**
 * Jepjep with the prompt in a speech bubble beside him: he is the one asking. The bubble's tail
 * points at him, and the bubble takes every dp he does not, so a long sentence wraps inside it.
 */
@Composable
private fun PromptWithJepjep(pose: JepjepPose, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Jepjep(pose = pose, height = 112.dp)
        Spacer(Modifier.width(Space.xxs))
        Box(Modifier.weight(1f)) {
            // The tail: a small rotated square tucked under the bubble's lower-left corner.
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 22.dp)
                    .size(16.dp)
                    .graphicsLayer { rotationZ = 45f; translationX = -7.dp.toPx() }
                    .background(Surface)
                    .border(1.dp, TrackNeutral)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Space.sm)
                    .clip(Shapes.tile)
                    .background(Surface)
                    .border(1.dp, TrackNeutral, Shapes.tile)
                    .padding(horizontal = Space.md, vertical = Space.md)
            ) {
                content()
            }
        }
    }
}

/** "3 in a row" with a flame, from the third correct answer on. Holds its width otherwise, so the progress bar does not jump. */
@Composable
private fun ComboCounter(combo: Int) {
    AnimatedVisibility(visible = combo >= 3, enter = fadeIn(tween(150)), exit = fadeOut(tween(100))) {
        Row(
            modifier = Modifier
                .clip(Shapes.pill)
                .background(com.kasiguru.ui.theme.AmberTint)
                .padding(horizontal = Space.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = Iconsax.FlashBold),
                contentDescription = null,
                tint = com.kasiguru.ui.theme.Coral,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(Space.xxs))
            Text(
                text = "$combo in a row",
                style = MaterialTheme.typography.labelLarge,
                color = Ink
            )
        }
    }
}
