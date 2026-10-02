package com.kasiguru.ui.screens.onboarding

import com.kasiguru.ui.components.KasiGuruTextField
import com.kasiguru.ui.components.StandardBadgeMedal
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.domain.gamification.BadgeCatalog
import com.kasiguru.domain.gamification.BadgeTier
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepAvatar
import com.kasiguru.ui.components.brand.JepjepAvatarPicker
import com.kasiguru.ui.components.brand.JepjepHello
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.brand.KasiGuruWordmark
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.LimeText
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.KasiguraninHeadword
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.LocalReducedMotion
import com.kasiguru.ui.theme.Motion
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Olive
import com.kasiguru.ui.theme.OnLime
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Scenery
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.motionTween

/** Longest display name the field accepts: room for a full name, short enough for a leaderboard row. */
internal const val MAX_NAME_LENGTH = 30

// ─────────────────────────────────────────────────────────────────────────────
// 1 · Welcome: the forest, Jepjep, the wordmark
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The one screen without the glow: Adrian's forest, full bleed, with Jepjep jumping in the clearing
 * and the wordmark on a dark scrim so the white "Kasi" stays legible over the scenery.
 */
@Composable
internal fun WelcomeStage(
    onGetStarted: () -> Unit,
    onHaveAccount: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Ground)) {
        Image(
            painter = painterResource(id = Scenery.Forest.res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Scrim: light at the top (just enough for the status bar icons), then night under the text.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Ground.copy(alpha = 0.35f),
                        0.12f to Color.Transparent,
                        0.45f to Ground.copy(alpha = 0.35f),
                        0.72f to Ground.copy(alpha = 0.8f),
                        1f to Ground.copy(alpha = 0.92f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // As tall as the clearing allows, never so tall he dwarfs the wordmark.
                val height = (maxHeight * 0.85f).coerceAtMost(320.dp)
                if (height >= 96.dp) {
                    Jepjep(
                        pose = JepjepPose.Celebrating,
                        height = height,
                        breathe = true
                    )
                }
            }

            Column(
                modifier = Modifier.readable(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KasiGuruWordmark(width = 220.dp)
                Spacer(Modifier.height(Space.md))
                Text(
                    text = "A gamified mobile learning app for the preservation and learning of the Kasiguranin language.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Space.xl))
                ClayButton(
                    label = "Get started",
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.sm))
                ClayButton(
                    label = "I already have an account",
                    onClick = onHaveAccount,
                    tone = ClayButtonTone.Quiet,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.md))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2–3 · The wake-up: Jepjep asleep, then awake. Tap anywhere.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A full-screen moment on the glow. The whole screen is the button, and says so.
 *
 * @param jump Jepjep hops once as he arrives (the wake-up). Skipped with reduced motion.
 * @param animatedHello draw Adrian's animated waving Jepjep ([JepjepHello]) instead of [pose].
 */
@Composable
internal fun StoryMoment(
    pose: JepjepPose,
    jump: Boolean,
    jepjepAnchor: Modifier,
    onContinue: () -> Unit,
    animatedHello: Boolean = false
) {
    val reducedMotion = LocalReducedMotion.current
    val hop = remember { Animatable(0f) }
    LaunchedEffect(jump, reducedMotion) {
        if (jump && !reducedMotion) {
            hop.animateTo(-1f, tween(durationMillis = 260, easing = Motion.EaseOut))
            hop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Continue",
                role = Role.Button,
                onClick = onContinue
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val height = (maxHeight * 0.42f).coerceIn(160.dp, 340.dp)
        if (animatedHello) {
            // The SVG's canvas has room for his hop and the sparkles, so it draws a little taller.
            JepjepHello(
                height = height * 1.25f,
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(jepjepAnchor)
            )
        } else {
            Jepjep(
                pose = pose,
                height = height,
                breathe = true,
                contentDescription = pose.description,
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(jepjepAnchor)
                    // After the anchor, so the glow stays put while he hops.
                    .graphicsLayer { translationY = hop.value * 56.dp.toPx() }
            )
        }
        Text(
            text = "Tap anywhere to continue",
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .readable()
                .padding(bottom = Space.xl)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4 · Name
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun NameStep(
    ctx: StepContext,
    name: String,
    onNameChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Text(
        text = highlighted("Magandang aldew! I'm Jepjep.", "Jepjep"),
        style = MaterialTheme.typography.titleLarge,
        color = Ink,
        textAlign = TextAlign.Center,
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.md))
    Jepjep(
        pose = JepjepPose.WithBackpack,
        height = ctx.artHeight,
        breathe = true,
        modifier = ctx.jepjepAnchor
    )
    Spacer(Modifier.height(Space.lg))
    StepTitle(
        text = highlighted("What should I call you?"),
        textAlign = TextAlign.Center,
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.xs))
    StepBody(
        text = highlighted("This is the name you'll see in the app and on the leaderboard."),
        textAlign = TextAlign.Center,
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.lg))
    KasiGuruTextField(
        value = name,
            label = { Text("Display name") },
        onValueChange = { onNameChange(it.take(MAX_NAME_LENGTH)) },
        placeholder = { Text("Enter your name") },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.readable()
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 5 · Nice to meet you
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Jepjep peeks in from the right edge, big, as in Adrian's design: his head fills the right half of
 * the screen, his far eye and the door edge of the artwork run off past the edge, and his body is
 * cropped by the buttons below. Sized from the screen's width rather than its height, so the crop
 * looks the same on a short phone and a tall one.
 */
@Composable
internal fun GreetingStep(ctx: StepContext, name: String) {
    val trimmed = name.trim()
    StepTitle(
        text = if (trimmed.isEmpty()) highlighted("Nice to meet you!", "you!")
        else highlighted("Nice to meet you, $trimmed!", "$trimmed!"),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.sm))
    StepBody(
        text = highlighted("I'll show you what we'll do together."),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.lg))

    // Whatever is left of the frame below the text; never so little that he shrinks to a sticker.
    val boxHeight = (ctx.viewportHeight - 160.dp).coerceAtLeast(300.dp)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(boxHeight)
            .clipToBounds()
            // He runs on behind the buttons rather than stopping at a hard edge above them.
            .fadeOutBottom(solidUntil = 0.82f)
    ) {
        // The artwork is 369 x 720, the head spanning roughly its left 80 % and the door edge at
        // ~90 %. At 0.84 of the screen's width the head fills the right half like the reference.
        val artWidth = maxWidth * 0.84f
        val artHeight = artWidth * (720f / 369f)
        // Drawn with both dimensions fixed rather than through Jepjep(height = ...): given only a
        // height, an Image keeps the bitmap's own pixel width, so this 369 px pose never drew wider
        // than ~140 dp however tall it was asked to be - which is why he looked small here.
        Image(
            painter = painterResource(id = JepjepPose.Peeking.res),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = maxWidth * 0.40f)
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(width = artWidth, height = artHeight)
                .then(ctx.jepjepAnchor)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6–8 · What we'll do together
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Jepjep cropped by a box the width of the screen and faded out at the bottom, as the value screens
 * draw him: large, partly off the edge, the headline taking over where he fades.
 */
@Composable
private fun CroppedJepjep(
    pose: JepjepPose,
    boxHeight: Dp,
    artHeight: Dp,
    alignment: Alignment,
    offsetX: Dp,
    anchor: Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(boxHeight)
            .clipToBounds()
            .fadeOutBottom()
    ) {
        Jepjep(
            pose = pose,
            height = artHeight,
            breathe = true,
            modifier = Modifier
                .align(alignment)
                .offset(x = offsetX)
                .then(anchor)
        )
    }
}

@Composable
internal fun WordsStep(ctx: StepContext) {
    CroppedJepjep(
        pose = JepjepPose.Reading,
        boxHeight = ctx.artHeight,
        artHeight = ctx.artHeight * 1.3f,
        alignment = Alignment.TopEnd,
        offsetX = ctx.artHeight * 0.12f,
        anchor = ctx.jepjepAnchor
    )
    Spacer(Modifier.height(Space.md))
    StepTitle(
        text = highlighted("Learn words from Casiguran.", "Casiguran."),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.sm))
    // 1,202 words in 12 categories in the seeded dictionary, every one with a Tagalog and an English
    // meaning. Rounded down so the line stays true as words are added.
    StepBody(
        text = highlighted(
            "Over 1,200 words across 12 topics, each with its Tagalog and English meaning.",
            "1,200 words"
        ),
        modifier = Modifier.readable()
    )
}

@Composable
internal fun MinutesStep(ctx: StepContext) {
    CroppedJepjep(
        pose = JepjepPose.Listening,
        boxHeight = ctx.artHeight,
        artHeight = ctx.artHeight * 1.3f,
        alignment = Alignment.TopStart,
        offsetX = -(ctx.artHeight * 0.1f),
        anchor = ctx.jepjepAnchor
    )
    Spacer(Modifier.height(Space.md))
    StepTitle(
        text = highlighted("A few minutes a day", "minutes"),
        textAlign = TextAlign.End,
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.sm))
    StepBody(
        text = highlighted(
            "Short lessons, and reviews that bring words back right before you'd forget them.",
            "Short lessons", "reviews"
        ),
        textAlign = TextAlign.End,
        modifier = Modifier.readable()
    )
}

@Composable
internal fun GamesStep(ctx: StepContext) {
    CroppedJepjep(
        pose = JepjepPose.PlayingAGame,
        boxHeight = ctx.artHeight,
        artHeight = ctx.artHeight * 1.2f,
        alignment = Alignment.TopCenter,
        offsetX = 0.dp,
        anchor = ctx.jepjepAnchor
    )
    Spacer(Modifier.height(Space.md))
    StepTitle(
        text = highlighted("Play games, read stories", "games"),
        textAlign = TextAlign.Center,
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.sm))
    // Eight mini-games on the Practice tab and ten seeded stories, each page in Tagalog and English.
    StepBody(
        text = highlighted(
            "8 mini-games, and 10 folk stories with Tagalog and English alongside.",
            "8 mini-games", "10 folk stories"
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier.readable()
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 9 · How much do you know
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun LevelStep(
    ctx: StepContext,
    selected: KnowledgeLevel?,
    onSelect: (KnowledgeLevel) -> Unit
) {
    Row(
        modifier = Modifier.readable(),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            StepTitle(text = highlighted("How much Kasiguranin do you know?", "Kasiguranin"))
            Spacer(Modifier.height(Space.xs))
            StepBody(text = highlighted("This helps Jepjep suggest a daily goal."))
        }
        Spacer(Modifier.width(Space.xs))
        Jepjep(
            pose = JepjepPose.Thinking,
            height = (ctx.artHeight * 0.75f).coerceAtLeast(96.dp),
            breathe = true,
            modifier = ctx.jepjepAnchor
        )
    }
    Spacer(Modifier.height(Space.lg))
    Column(
        modifier = Modifier
            .readable()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        KnowledgeLevel.entries.forEach { level ->
            OptionPill(
                label = level.label,
                state = if (level == selected) PillState.Selected else PillState.Idle,
                onClick = { onSelect(level) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 10 · Daily goal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun GoalStep(selected: DailyGoal, onSelect: (DailyGoal) -> Unit) {
    StepTitle(
        text = highlighted("How much time each day?", "time"),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.xs))
    StepBody(
        text = highlighted("Pick what fits your day. The XP you earn fills your daily goal."),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.lg))
    Column(
        modifier = Modifier
            .readable()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        DailyGoal.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                row.forEach { goal ->
                    GoalTile(
                        goal = goal,
                        selected = goal == selected,
                        onClick = { onSelect(goal) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** A rounded-square goal tile: minutes first, then the name, then what it means in XP. */
@Composable
private fun GoalTile(
    goal: DailyGoal,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fill by animateColorAsState(
        targetValue = if (selected) Olive else Surface,
        animationSpec = motionTween(Motion.Quick),
        label = "GoalFill"
    )
    Box(
        modifier = modifier
            .heightIn(min = 120.dp)
            .clip(Shapes.tile)
            .background(fill)
            .border(if (selected) 2.dp else 1.dp, if (selected) Lime else BorderHairline, Shapes.tile)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(Space.md),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${goal.minutes} min",
                style = MaterialTheme.typography.headlineLarge,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = goal.label,
                style = MaterialTheme.typography.bodyLarge,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = "${goal.xp} XP a day",
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Muted else Faint,
                textAlign = TextAlign.Center
            )
        }
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd)) {
                OptionIcon(res = Iconsax.TickCircle, contentDescription = null, tint = Lime)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 11 · The first word
// ─────────────────────────────────────────────────────────────────────────────

/**
 * One real word, answered once. There is no play button: the seeded entry carries no recording, and
 * the audio player offers nothing for a word without one, so a button here could only do nothing.
 */
@Composable
internal fun FirstWordStep(
    ctx: StepContext,
    answer: Int,
    onAnswer: (Int) -> Unit
) {
    val answered = answer >= 0
    StepTitle(
        text = highlighted("Your first word", "first"),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.xs))
    StepBody(
        text = highlighted("You saw it in Jepjep's greeting. What does it mean?"),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.md))
    // Jepjep stands behind the word card: cut off at the card's top edge, no fade.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ctx.artHeight * 0.8f)
            .clipToBounds()
    ) {
        Jepjep(
            pose = JepjepPose.Pointing,
            height = ctx.artHeight * 1.05f,
            breathe = true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(ctx.jepjepAnchor)
        )
    }
    WordCard(subtitle = FirstWord.IPA, subtitleColor = Faint)
    Spacer(Modifier.height(Space.md))
    Row(
        modifier = Modifier
            .readable()
            .height(IntrinsicSize.Min)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        FirstWord.options.forEachIndexed { index, option ->
            val state = when {
                !answered -> PillState.Idle
                index == FirstWord.correctIndex -> PillState.Correct
                index == answer -> PillState.Wrong
                else -> PillState.Idle
            }
            OptionPill(
                label = option,
                state = state,
                enabled = !answered,
                onClick = { onAnswer(index) },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
    if (answered) {
        Spacer(Modifier.height(Space.md))
        StepBody(
            text = if (answer == FirstWord.correctIndex) {
                highlighted("That's it: aldew is the day, and the sun.", "aldew")
            } else {
                highlighted("Aldew is the day, and the sun. Water is danom.", "Aldew")
            },
            modifier = Modifier.readable()
        )
    }
}

/** The word on a dark card, in the headword style every word screen uses. */
@Composable
private fun WordCard(subtitle: String, subtitleColor: Color) {
    Column(
        modifier = Modifier
            .readable()
            .clip(Shapes.tile)
            .background(Surface)
            .border(1.dp, BorderHairline, Shapes.tile)
            .padding(vertical = Space.lg, horizontal = Space.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = FirstWord.WORD,
            style = KasiguraninHeadword,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = subtitleColor,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 12 · First word learned
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The reward. "+50 XP" and "Day 1 streak" are what completing onboarding grants (see
 * `UserProgressDao.completeOnboarding`), shown as reward chips so they don't read as a button.
 */
@Composable
internal fun FirstWordLearnedStep(ctx: StepContext, name: String) {
    val trimmed = name.trim()
    StepTitle(
        text = highlighted(
            if (trimmed.isEmpty()) "Congrats on your first word!" else "Congrats on your first word, $trimmed!",
            "first word"
        ),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.md))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ctx.artHeight)
            .clipToBounds()
    ) {
        Jepjep(
            pose = JepjepPose.Celebrating,
            height = ctx.artHeight * 1.12f,
            breathe = true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(ctx.jepjepAnchor)
        )
    }
    WordCard(subtitle = FirstWord.MEANING, subtitleColor = Muted)
    Spacer(Modifier.height(Space.lg))
    Row(
        modifier = Modifier.readable(),
        horizontalArrangement = Arrangement.spacedBy(Space.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RewardChip(text = "+50 XP", iconRes = Iconsax.StarBold, fill = Gold)
        RewardChip(text = "Day 1 streak", iconRes = Iconsax.FlashBold, fill = Coral)
    }
}

/** A flat reward chip: bright fill, dark ink, an icon. No lip and no press state, because it isn't a button. */
@Composable
private fun RewardChip(text: String, @DrawableRes iconRes: Int, fill: Color) {
    Row(
        modifier = Modifier
            .clip(Shapes.chip)
            .background(fill)
            .padding(horizontal = Space.sm, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = RewardInk,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Space.xxs))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = RewardInk)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 13 · Reminders
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The same two reminders Settings lists, with the same icons, so turning one on here is visibly the
 * same switch as there. "Allow reminders" saves them and asks Android for permission.
 */
@Composable
internal fun RemindersStep(
    ctx: StepContext,
    streakOn: Boolean,
    wordOfDayOn: Boolean,
    onStreakChange: (Boolean) -> Unit,
    onWordOfDayChange: (Boolean) -> Unit
) {
    Jepjep(
        pose = JepjepPose.Sitting,
        height = ctx.artHeight * 0.85f,
        breathe = true,
        modifier = ctx.jepjepAnchor
    )
    Spacer(Modifier.height(Space.md))
    StepTitle(
        text = highlighted("Let Jepjep remind you", "remind"),
        textAlign = TextAlign.Center,
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.lg))
    Column(
        modifier = Modifier.readable(),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        ReminderCard(
            title = "Protect your streak",
            subtitle = "A nudge before your streak runs out",
            iconRes = Iconsax.Flash,
            tint = Coral,
            checked = streakOn,
            onCheckedChange = onStreakChange
        )
        ReminderCard(
            title = "Word of the day",
            subtitle = "One Kasiguranin word to notice each day",
            iconRes = Iconsax.Book,
            tint = Lime,
            checked = wordOfDayOn,
            onCheckedChange = onWordOfDayChange
        )
    }
}

@Composable
private fun ReminderCard(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    tint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.tile)
            .background(Surface)
            .border(1.dp, BorderHairline, Shapes.tile)
            // The whole card is the switch, so the target is the card, not the 32 dp track.
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(Shapes.chip)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        Spacer(Modifier.width(Space.xs))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                // A dark thumb on the lime track: white on lime is 2.3:1.
                checkedThumbColor = OnLime,
                checkedTrackColor = Lime,
                checkedBorderColor = Lime,
                uncheckedThumbColor = Faint,
                uncheckedTrackColor = SurfaceSunken,
                uncheckedBorderColor = BorderHairline
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 14 · Avatar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun AvatarStep(
    ctx: StepContext,
    selected: JepjepAvatar,
    onSelect: (JepjepAvatar) -> Unit
) {
    StepTitle(
        text = highlighted("Choose your avatar", "avatar"),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.xs))
    StepBody(
        text = highlighted("Pick who you'll learn as. You can change it later."),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.lg))
    BoxWithConstraints(Modifier.readable()) {
        // Three across with room for the names; on a short screen, small enough that all nine fit.
        val byWidth = (maxWidth - Space.lg) / 3
        val byHeight = ((ctx.viewportHeight - 200.dp) / 3 - 28.dp).coerceAtLeast(72.dp)
        val portrait = minOf(96.dp, byWidth, byHeight)
        JepjepAvatarPicker(
            selected = selected,
            onSelect = onSelect,
            portraitSize = portrait,
            modifier = Modifier
                .fillMaxWidth()
                .then(ctx.jepjepAnchor)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 15 · Badges
// ─────────────────────────────────────────────────────────────────────────────

/** One badge on the onboarding wall: the family, the tier shown, and what earning that tier takes. */
private data class BadgePreview(val familyId: String, val tier: BadgeTier, val condition: (Int) -> String)

/**
 * Six of the eleven families, one at each tier from Beginner to Legend, so the wall shows the
 * whole ladder a badge climbs as well as the range of things that earn one: words, streaks,
 * stories, lessons, games and level. Thresholds are read from [BadgeCatalog], so they cannot drift.
 */
private val badgePreviews = listOf(
    BadgePreview("word_explorer", BadgeTier.BEGINNER) { n -> if (n == 1) "Master your first word" else "Master $n words" },
    BadgePreview("consistent_learner", BadgeTier.LEARNER) { n -> "Keep a $n-day streak" },
    BadgePreview("story_reader", BadgeTier.ACHIEVER) { n -> "Read $n stories" },
    BadgePreview("lesson_pathfinder", BadgeTier.EXPERT) { n -> "Finish $n lessons" },
    BadgePreview("precision_player", BadgeTier.MASTER) { n -> "Play $n perfect levels" },
    BadgePreview("journey_rank", BadgeTier.LEGEND) { n -> "Reach level $n" }
)

@Composable
internal fun BadgesStep() {
    StepTitle(
        text = highlighted("Earn badges as you learn", "badges"),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.xs))
    StepBody(
        text = highlighted("Every badge climbs six tiers, from Beginner to Legend. Learn, read and play to rise."),
        modifier = Modifier.readable()
    )
    Spacer(Modifier.height(Space.lg))
    Column(
        modifier = Modifier.readable(),
        verticalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        badgePreviews.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                row.forEach { badge ->
                    BadgeTile(
                        badge = badge,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

/** The tier's artwork, the family, what it takes, and the tier named in text, never by frame alone. */
@Composable
private fun BadgeTile(badge: BadgePreview, modifier: Modifier = Modifier) {
    val family = BadgeCatalog.families.first { it.id == badge.familyId }
    Column(
        modifier = modifier
            .clip(Shapes.tile)
            .background(Surface)
            .border(1.dp, BorderHairline, Shapes.tile)
            .padding(horizontal = Space.xs, vertical = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StandardBadgeMedal(tier = badge.tier, earned = true, size = 72.dp, familyId = family.id)
        Spacer(Modifier.height(Space.xs))
        Text(
            text = family.name,
            style = MaterialTheme.typography.titleSmall,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = badge.condition(family.thresholds[badge.tier.ordinal]),
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(Space.xs))
        Text(
            text = badge.tier.label,
            style = MaterialTheme.typography.labelMedium,
            color = LimeText,
            textAlign = TextAlign.Center
        )
    }
}
