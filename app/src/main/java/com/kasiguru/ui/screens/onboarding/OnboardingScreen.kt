package com.kasiguru.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.components.CasiguranResident
import com.kasiguru.ui.components.ConfettiView
import com.kasiguru.ui.components.OnboardingDotStepper
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.ClaySurface
import com.kasiguru.ui.components.clay.GlassChip
import com.kasiguru.ui.components.clay.GroundBackButton
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.components.clay.groundTexture
import com.kasiguru.ui.theme.CanopyBottom
import com.kasiguru.ui.theme.CanopyTop
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.KasiguraninHeadword
import com.kasiguru.ui.theme.LocalDarkMode
import com.kasiguru.ui.theme.LocalReducedMotion
import com.kasiguru.ui.theme.Motion
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.Red
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.StatusBarIcons
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.Touch
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletTint

/**
 * First-run setup. Four steps, ~45 seconds, and the learner can leave after the first one.
 *
 * Its job is not to teach the app — the guided tour on Learn does that — but to get someone to the one
 * moment that proves the app is worth the space on their phone: reading a Kasiguranin word and getting
 * it right. That moment (step 2) is the centre of the flow, not a quiz buried in the middle of it.
 *
 * It stays inside the Violet Sheet system: the [Ground] shell with its drawn [GroundPattern], every
 * window inset honoured, one scrollable body per step with the primary action pinned above the
 * gesture bar, and clay reserved for the buttons and the reward panel. The step-1 hero is one of the
 * two sanctioned canopy/glass callers in the app.
 *
 * @param onCompleteOnboarding receives exactly what [OnboardingViewModel.completeOnboarding] persists —
 *   nothing is collected here that is then dropped on the way out.
 * @param heroArt optional illustration for the welcome hero. Adrian authors this; with it null the
 *   canopy gradient plus the Kasiguranin greeting is the finished state.
 */
@Composable
fun OnboardingScreen(
    onCompleteOnboarding: (userName: String, avatarId: Int, dailyGoalXp: Int, titleBadge: String, residentName: String) -> Unit,
    @DrawableRes heroArt: Int? = null
) {
    var step by remember { mutableIntStateOf(1) }
    val haptic = LocalHapticFeedback.current

    var dailyGoalXp by remember { mutableIntStateOf(DEFAULT_GOAL_XP) }
    var userName by remember { mutableStateOf(DEFAULT_NAME) }
    var tasteChoice by remember { mutableStateOf<String?>(null) }

    // Step transition durations, resolved here so the AnimatedContent transitionSpec below — which is
    // a plain lambda, not a @Composable one — can close over them. Reduced motion collapses them to
    // an instant cut.
    val stepEnterMs = if (LocalReducedMotion.current) 0 else Motion.Standard
    val stepExitMs = if (LocalReducedMotion.current) 0 else Motion.exit(Motion.Standard)

    fun finish() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onCompleteOnboarding(
            userName.ifBlank { DEFAULT_NAME },
            DEFAULT_AVATAR_ID,
            dailyGoalXp,
            DEFAULT_TITLE,
            onboardingAvatars[DEFAULT_AVATAR_ID - 1].name
        )
    }

    fun advance() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        step += 1
    }

    BackHandler(enabled = step > 1) { step -= 1 }

    // The wizard draws Ground behind the status bar; its glyphs follow the theme, the same way every
    // Ground screen's do. They were once forced light app-wide, which left them invisible here on the
    // light lavender.
    StatusBarIcons(dark = !LocalDarkMode.current)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .groundTexture(GroundPattern.Arcs, "onboarding")
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Header: back + progress ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(Touch.minTarget), contentAlignment = Alignment.Center) {
                    if (step > 1) GroundBackButton(onClick = { step -= 1 })
                }
                OnboardingDotStepper(
                    total = TOTAL_STEPS,
                    current = step - 1,
                    activeColor = Violet,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Space.sm)
                )
                Spacer(Modifier.size(Touch.minTarget))
            }

            // ── Body: scrolls; content only, no layout tricks ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.gutter)
                    .padding(top = Space.sm, bottom = Space.md)
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val dir = if (targetState >= initialState) 1 else -1
                        (slideInHorizontally(tween(stepEnterMs)) { w -> dir * w / 5 } +
                            fadeIn(tween(stepEnterMs))) togetherWith
                            (slideOutHorizontally(tween(stepExitMs)) { w -> -dir * w / 5 } +
                                fadeOut(tween(stepExitMs)))
                    },
                    label = "OnboardingStep"
                ) { current ->
                    when (current) {
                        1 -> StepWelcome(heroArt = heroArt)
                        2 -> StepTaste(
                            choice = tasteChoice,
                            onChoose = { picked ->
                                if (tasteChoice == null) {
                                    tasteChoice = picked
                                    haptic.performHapticFeedback(
                                        if (picked == TASTE_ANSWER) HapticFeedbackType.LongPress
                                        else HapticFeedbackType.TextHandleMove
                                    )
                                }
                            }
                        )
                        3 -> StepDailyGoal(
                            selectedXp = dailyGoalXp,
                            onSelectXp = { dailyGoalXp = it }
                        )
                        4 -> StepReady(
                            userName = userName,
                            onNameChange = { userName = it },
                            dailyGoalXp = dailyGoalXp
                        )
                    }
                }
            }

            // ── Pinned action, clear of the nav bar and the keyboard ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.gutter)
                    .padding(top = Space.sm, bottom = Space.sm)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    1 -> {
                        ClayButton(
                            label = "Get started",
                            onClick = ::advance,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SkipLink(onClick = ::finish)
                    }
                    2 -> ClayButton(
                        label = "Continue",
                        onClick = ::advance,
                        enabled = tasteChoice != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    3 -> ClayButton(
                        label = "Continue",
                        onClick = ::advance,
                        modifier = Modifier.fillMaxWidth()
                    )
                    4 -> ClayButton(
                        label = "Start learning",
                        onClick = ::finish,
                        tone = ClayButtonTone.Reward,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // One burst, on the correct answer only, and gone by the next step.
        if (step == 2 && tasteChoice == TASTE_ANSWER) {
            ConfettiView(modifier = Modifier.fillMaxSize())
        }
    }
}

// ── Defaults. Skipping setup applies every one of these, so each has to be a sensible real value. ──
private const val TOTAL_STEPS = 4
private const val DEFAULT_GOAL_XP = 100
private const val DEFAULT_NAME = "Kasiguranin Learner"
private const val DEFAULT_TITLE = "Kasiguranin Apprentice"
private const val DEFAULT_AVATAR_ID = 1

/**
 * The order here is load-bearing: a 1-based position in this list is stored permanently as
 * `UserProgressEntity.profileIconId`. Append new residents at the end only — never reorder or remove
 * one already shipped. The wizard no longer shows a picker (six identical placeholder glyphs was worse
 * than none); it assigns [DEFAULT_AVATAR_ID], and this list stays so a real picker can return here
 * once Adrian's avatar art exists.
 */
private val onboardingAvatars = listOf(
    CasiguranResident.STUDENT,
    CasiguranResident.TEACHER,
    CasiguranResident.ELDER,
    CasiguranResident.SURFER,
    CasiguranResident.MUSICIAN,
    CasiguranResident.FARMER
)

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — Welcome
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepWelcome(@DrawableRes heroArt: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.panel)
                .background(Brush.verticalGradient(listOf(CanopyTop, CanopyBottom)))
                .padding(horizontal = Space.lg, vertical = Space.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (heroArt != null) {
                    Image(
                        painter = painterResource(id = heroArt),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                    Spacer(Modifier.height(Space.md))
                }
                GlassChip {
                    Text(
                        text = "Casiguran, Aurora",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnCanopy
                    )
                }
                Spacer(Modifier.height(Space.md))
                Text(
                    text = "Magandang Aldew!",
                    style = KasiguraninHeadword,
                    color = OnCanopy,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "Welcome to KasiGuru",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnCanopy
                )
            }
        }

        Spacer(Modifier.height(Space.lg))

        Text(
            text = "Learn and help keep Kasiguranin, the indigenous language of Casiguran, Aurora — a few minutes a day, one small set at a time.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Space.sm)
        )
        Spacer(Modifier.height(Space.sm))
        Text(
            text = "About a minute to set up.",
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — First success: one real word
// ─────────────────────────────────────────────────────────────────────────────

// Verbatim from the seeded corpus (DatabaseSeeder.kt: aldew=sun line ~2984, danom=water line ~4168).
// Featured rather than random so it pays off the "Magandang Aldew" the learner just read on step 1.
private const val TASTE_WORD = "aldew"
private const val TASTE_ANSWER = "Sun, day"
private const val TASTE_DISTRACTOR = "Water"

@Composable
private fun StepTaste(choice: String?, onChoose: (String) -> Unit) {
    val answered = choice != null
    val correct = choice == TASTE_ANSWER

    Column {
        StepHeading(
            "Your first word",
            "You just saw it in the greeting. Tap what it means."
        )
        Spacer(Modifier.height(Space.lg))

        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = TASTE_WORD, style = KasiguraninHeadword, color = Ink)
                Spacer(Modifier.height(Space.lg))
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.fillMaxWidth()) {
                    TasteChoice(
                        label = TASTE_ANSWER,
                        state = if (answered) ChoiceState.Correct else ChoiceState.Idle,
                        enabled = !answered,
                        onClick = { onChoose(TASTE_ANSWER) }
                    )
                    TasteChoice(
                        label = TASTE_DISTRACTOR,
                        state = if (choice == TASTE_DISTRACTOR) ChoiceState.Wrong else ChoiceState.Idle,
                        enabled = !answered,
                        onClick = { onChoose(TASTE_DISTRACTOR) }
                    )
                }
            }
        }

        if (answered) {
            Spacer(Modifier.height(Space.md))
            Text(
                text = if (correct) {
                    "Yes — aldew is the sun, the day. That is one word learned. A daily set is about seven."
                } else {
                    "Close — aldew means the sun, the day. You will see it again soon."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
        }
    }
}

private enum class ChoiceState { Idle, Correct, Wrong }

@Composable
private fun TasteChoice(
    label: String,
    state: ChoiceState,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val face = when (state) {
        ChoiceState.Idle -> VioletTint
        ChoiceState.Correct -> Green
        ChoiceState.Wrong -> Red
    }
    val fg = if (state == ChoiceState.Idle) Violet else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.tile)
            .background(face)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = Touch.minTarget)
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = fg,
            modifier = Modifier.weight(1f)
        )
        when (state) {
            ChoiceState.Correct -> Icon(
                painter = painterResource(id = Iconsax.TickCircle),
                contentDescription = "Correct",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            ChoiceState.Wrong -> Icon(
                painter = painterResource(id = Iconsax.CloseCircle),
                contentDescription = "Not this one",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            ChoiceState.Idle -> Unit
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3 — Daily goal
// ─────────────────────────────────────────────────────────────────────────────

private data class GoalOption(
    val xp: Int,
    val title: String,
    val time: String,
    val xpTarget: String,
    @DrawableRes val iconRes: Int,
    val accent: Color
)

@Composable
private fun StepDailyGoal(selectedXp: Int, onSelectXp: (Int) -> Unit) {
    val goals = listOf(
        GoalOption(50, "Casual", "5 mins / day", "50 XP", Iconsax.Flash, Gold),
        GoalOption(100, "Regular", "10 mins / day", "100 XP", Iconsax.StarBold, Violet),
        GoalOption(150, "Serious", "15 mins / day", "150 XP", Iconsax.BookBold, Coral),
        GoalOption(200, "Intense", "20 mins / day", "200 XP", Iconsax.Cup, Gold)
    )

    Column {
        StepHeading(
            "Pick a daily target",
            "It sets your daily-goal ring. You can change it anytime in Settings."
        )
        Spacer(Modifier.height(Space.lg))
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            goals.forEach { g ->
                SelectableRow(
                    title = g.title,
                    subtitle = g.time,
                    iconRes = g.iconRes,
                    accent = g.accent,
                    isSelected = selectedXp == g.xp,
                    onClick = { onSelectXp(g.xp) },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .clip(Shapes.chip)
                                .background(g.accent.copy(alpha = 0.16f))
                                .padding(horizontal = Space.xs, vertical = Space.xxs)
                        ) {
                            Text(
                                text = g.xpTarget,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (g.accent == Gold || g.accent == Coral) Ink else g.accent
                            )
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4 — You're set
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepReady(
    userName: String,
    onNameChange: (String) -> Unit,
    dailyGoalXp: Int
) {
    Column {
        StepHeading("You're set", "This is the name other learners see on the leaderboard.")
        Spacer(Modifier.height(Space.md))

        OutlinedTextField(
            value = userName,
            onValueChange = onNameChange,
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.tile,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet,
                unfocusedBorderColor = Muted.copy(alpha = 0.4f),
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
                focusedLabelColor = Violet,
                cursorColor = Violet
            )
        )

        Spacer(Modifier.height(Space.lg))

        ClaySurface(
            face = Gold,
            lipColor = GoldDeep,
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.panel,
            contentPadding = PaddingValues(Space.lg)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "+50 XP", style = MaterialTheme.typography.displaySmall, color = RewardInk)
                Spacer(Modifier.height(Space.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = Iconsax.FlashBold),
                        contentDescription = null,
                        tint = RewardInk,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(Space.xxs))
                    Text(
                        text = "Day 1 streak started",
                        style = MaterialTheme.typography.titleMedium,
                        color = RewardInk
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.md))

        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                SummaryRow(Iconsax.FlashBold, "Daily target", "$dailyGoalXp XP / day")
                SummaryRow(Iconsax.BookBold, "First week", "~35 words")
            }
        }
    }
}

@Composable
private fun SummaryRow(@DrawableRes iconRes: Int, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(Space.xs))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = Ink, textAlign = TextAlign.End)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared bits
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepHeading(title: String, subtitle: String?) {
    Text(text = title, style = MaterialTheme.typography.headlineMedium, color = Ink)
    if (subtitle != null) {
        Spacer(Modifier.height(Space.xxs))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
private fun SkipLink(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = Touch.minTarget)
            .clip(Shapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Skip setup",
            style = MaterialTheme.typography.labelLarge,
            color = Muted
        )
    }
}

/**
 * Shared single-select row for the daily-goal list: an accent icon chip, a title/subtitle pair, and a
 * caller-supplied trailing slot or the default checkmark.
 */
@Composable
private fun SelectableRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    accent: Color = Violet,
    trailing: (@Composable () -> Unit)? = null
) {
    SoftCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isSelected) Modifier.border(2.dp, Violet, Shapes.tile) else Modifier),
        shape = Shapes.tile,
        color = if (isSelected) VioletTint else Surface,
        elevation = if (isSelected) 2.dp else 6.dp,
        onClick = onClick,
        contentPadding = PaddingValues(Space.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(Shapes.chip)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(Space.sm))
            }
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            when {
                trailing != null -> {
                    Spacer(Modifier.width(Space.xs))
                    trailing()
                }
                isSelected -> {
                    Spacer(Modifier.width(Space.xs))
                    Icon(
                        painter = painterResource(id = Iconsax.TickCircle),
                        contentDescription = "Selected",
                        tint = Violet,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
