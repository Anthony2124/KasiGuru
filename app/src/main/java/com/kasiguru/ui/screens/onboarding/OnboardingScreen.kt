package com.kasiguru.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.ConfettiView
import com.kasiguru.ui.components.brand.JepjepAvatar
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.GlowBackdrop
import com.kasiguru.ui.theme.LocalReducedMotion
import com.kasiguru.ui.theme.Motion
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.StatusBarIcons
import com.kasiguru.ui.theme.motionTween

/**
 * First-run onboarding: Adrian's fifteen screens, Jepjep as the guide.
 *
 * A forest welcome, a two-tap wake-up (Jepjep asleep, then awake), then twelve steps in one shared
 * frame: name, what the app does, how much the learner already knows, a daily goal, the first real
 * word and its reward, reminders, an avatar and a look at the badges. The order and the Skip/Back
 * rules live in [OnboardingStep]; this function holds the answers and moves between steps.
 *
 * Every answer is in `rememberSaveable`, so rotating the phone or Android reclaiming the process
 * mid-flow resumes where the learner was, not at the welcome screen.
 *
 * Skip never skips the first word: before it, Skip jumps to it; after it, Skip finishes with
 * defaults for whatever is left.
 *
 * @param onCompleteOnboarding receives exactly what [OnboardingViewModel.completeOnboarding] persists.
 * @param onOpenLogin "I already have an account": the Account screen, where a returning learner
 *   signs in and is sent on to Learn.
 * @param viewModel only for the reminder preferences; resolves to the same instance the nav graph
 *   created for this back stack entry.
 */
@Composable
fun OnboardingScreen(
    onCompleteOnboarding: (userName: String, avatarId: Int, dailyGoalXp: Int, titleBadge: String, residentName: String) -> Unit,
    onOpenLogin: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // ── Answers. Saved, so rotation and process death resume in place. ──
    var stepOrdinal by rememberSaveable { mutableStateOf(OnboardingStep.Welcome.ordinal) }
    var name by rememberSaveable { mutableStateOf("") }
    var levelOrdinal by rememberSaveable { mutableStateOf(-1) }
    var goalOrdinal by rememberSaveable { mutableStateOf(-1) }
    var firstWordAnswer by rememberSaveable { mutableStateOf(-1) }
    var avatarOrdinal by rememberSaveable { mutableStateOf(JepjepAvatar.Default.ordinal) }
    // Null until the learner touches a switch: until then it shows what Settings has saved.
    var streakChoice by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var wordOfDayChoice by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val step = OnboardingStep.fromOrdinal(stepOrdinal)
    val level = KnowledgeLevel.entries.getOrNull(levelOrdinal)
    val goal = resolveDailyGoal(DailyGoal.entries.getOrNull(goalOrdinal), level)
    val avatar = JepjepAvatar.entries.getOrElse(avatarOrdinal) { JepjepAvatar.Default }
    val savedStreak by viewModel.streakReminders.collectAsState()
    val savedWordOfDay by viewModel.wordOfDayReminders.collectAsState()
    val streakOn = streakChoice ?: savedStreak
    val wordOfDayOn = wordOfDayChoice ?: savedWordOfDay

    // Guards against a double tap on the last button completing twice (which would grant +50 XP twice).
    var finished by remember { mutableStateOf(false) }

    fun goTo(target: OnboardingStep) {
        focusManager.clearFocus()
        stepOrdinal = target.ordinal
    }

    fun finish() {
        if (finished) return
        finished = true
        focusManager.clearFocus()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        // Resolved from the saved answers now, not from the vals above, which a button lambda held
        // by the shared frame may have captured several steps ago (see currentStep()).
        val chosenAvatar = JepjepAvatar.entries.getOrElse(avatarOrdinal) { JepjepAvatar.Default }
        val chosenGoal = resolveDailyGoal(
            DailyGoal.entries.getOrNull(goalOrdinal),
            KnowledgeLevel.entries.getOrNull(levelOrdinal)
        )
        onCompleteOnboarding(
            name.trim().ifBlank { DEFAULT_NAME },
            chosenAvatar.id,
            chosenGoal.xp,
            DEFAULT_TITLE,
            avatar.name
        )
    }

    fun advanceFrom(from: OnboardingStep) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val next = from.next
        if (next == null) finish() else goTo(next)
    }

    // These read [stepOrdinal] when called, never the [step] val above. The frame's buttons are built
    // inside an AnimatedContent entry that every framed step shares, so a lambda holding [step] keeps
    // the step it was composed on, and Next would replay "advance from Name" forever.
    fun currentStep(): OnboardingStep = OnboardingStep.fromOrdinal(stepOrdinal)

    fun advance() = advanceFrom(currentStep())

    fun goBack() {
        currentStep().previous?.let(::goTo)
    }

    fun skip() {
        val target = currentStep().skipTarget
        if (target != null) goTo(target) else finish()
    }

    // Android 13+ asks for notification permission here, when the learner has just said yes to
    // reminders, rather than on every cold start. Whatever they answer, the flow moves on.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { advanceFrom(OnboardingStep.Reminders) }

    fun allowReminders() {
        // Re-read the switches here rather than using streakOn/wordOfDayOn, for the same stale-capture
        // reason as currentStep().
        val streak = streakChoice ?: savedStreak
        val wordOfDay = wordOfDayChoice ?: savedWordOfDay
        viewModel.saveReminders(streak = streak, wordOfDay = wordOfDay)
        val needsPermission = (streak || wordOfDay) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            advanceFrom(OnboardingStep.Reminders)
        }
    }

    // "Not now" turns both off, so Settings shows what the learner chose and can ask again later.
    fun declineReminders() {
        viewModel.saveReminders(streak = false, wordOfDay = false)
        advance()
    }

    BackHandler(enabled = step != OnboardingStep.Welcome) { goBack() }

    // Light status-bar glyphs: every step is drawn on night or on a scrimmed scene.
    StatusBarIcons()

    // Resolved here because transitionSpec is not a @Composable lambda. Reduced motion cuts instantly.
    val reducedMotion = LocalReducedMotion.current
    val enterMs = if (reducedMotion) 0 else Motion.Standard
    val exitMs = if (reducedMotion) 0 else Motion.exit(Motion.Standard)

    val spots = remember { JepjepSpots() }

    OnboardingGlow(step = step, spots = spots) {
        // Top level: one scene per story moment and one shared scene for the whole frame, so moving
        // between framed steps animates only the middle while the bars stay put.
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn(tween(enterMs)) togetherWith fadeOut(tween(exitMs)) },
            contentKey = { it.stage },
            label = "OnboardingStage",
            modifier = Modifier.fillMaxSize()
        ) { shown ->
            when (shown) {
                OnboardingStep.Welcome -> WelcomeStage(
                    onGetStarted = ::advance,
                    onHaveAccount = onOpenLogin
                )
                OnboardingStep.Asleep -> StoryMoment(
                    pose = JepjepPose.Sleeping,
                    jump = false,
                    jepjepAnchor = spots.anchor(OnboardingStep.Asleep),
                    onContinue = { advanceFrom(OnboardingStep.Asleep) }
                )
                // Adrian's wake-up screen draws the arms-open jumping pose, which is the Waving art.
                OnboardingStep.Awake -> StoryMoment(
                    pose = JepjepPose.Waving,
                    jump = true,
                    jepjepAnchor = spots.anchor(OnboardingStep.Awake),
                    onContinue = { advanceFrom(OnboardingStep.Awake) }
                )
                else -> OnboardingFrame(
                    filled = shown.progressIndex + 1,
                    total = OnboardingStep.framedCount,
                    showSkip = shown.skippable,
                    onSkip = ::skip,
                    secondaryLabel = if (shown == OnboardingStep.Reminders) "Not now" else "Back",
                    onSecondary = if (shown == OnboardingStep.Reminders) ::declineReminders else ::goBack,
                    primaryLabel = when (shown) {
                        OnboardingStep.Reminders -> "Allow reminders"
                        OnboardingStep.Badges -> "Start learning"
                        else -> "Next"
                    },
                    primaryEnabled = shown.optional || firstWordAnswer >= 0,
                    onPrimary = if (shown == OnboardingStep.Reminders) ::allowReminders else ::advance
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val viewport = maxHeight
                        val art = (viewport * 0.34f).coerceIn(112.dp, 260.dp)
                        AnimatedContent(
                            targetState = shown,
                            transitionSpec = {
                                val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                (slideInHorizontally(tween(enterMs, easing = Motion.EaseOut)) { w -> dir * w / 5 } +
                                    fadeIn(tween(enterMs))) togetherWith
                                    (slideOutHorizontally(tween(exitMs, easing = Motion.EaseIn)) { w -> -dir * w / 5 } +
                                        fadeOut(tween(exitMs)))
                            },
                            label = "OnboardingStep",
                            modifier = Modifier.fillMaxSize()
                        ) { current ->
                            val ctx = StepContext(
                                artHeight = art,
                                viewportHeight = viewport,
                                jepjepAnchor = spots.anchor(current)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = Space.md, bottom = Space.lg),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (current) {
                                    OnboardingStep.Name -> NameStep(
                                        ctx = ctx,
                                        name = name,
                                        onNameChange = { name = it },
                                        onDone = { advanceFrom(OnboardingStep.Name) }
                                    )
                                    OnboardingStep.Greeting -> GreetingStep(ctx, name)
                                    OnboardingStep.Words -> WordsStep(ctx)
                                    OnboardingStep.Minutes -> MinutesStep(ctx)
                                    OnboardingStep.Games -> GamesStep(ctx)
                                    OnboardingStep.Level -> LevelStep(
                                        ctx = ctx,
                                        selected = level,
                                        onSelect = { levelOrdinal = it.ordinal }
                                    )
                                    OnboardingStep.Goal -> GoalStep(
                                        selected = goal,
                                        onSelect = { goalOrdinal = it.ordinal }
                                    )
                                    OnboardingStep.FirstWord -> FirstWordStep(
                                        ctx = ctx,
                                        answer = firstWordAnswer,
                                        onAnswer = { picked ->
                                            if (firstWordAnswer < 0) {
                                                firstWordAnswer = picked
                                                haptic.performHapticFeedback(
                                                    if (picked == FirstWord.correctIndex) HapticFeedbackType.LongPress
                                                    else HapticFeedbackType.TextHandleMove
                                                )
                                            }
                                        }
                                    )
                                    OnboardingStep.FirstWordLearned -> FirstWordLearnedStep(ctx, name)
                                    OnboardingStep.Reminders -> RemindersStep(
                                        ctx = ctx,
                                        streakOn = streakOn,
                                        wordOfDayOn = wordOfDayOn,
                                        onStreakChange = { streakChoice = it },
                                        onWordOfDayChange = { wordOfDayChoice = it }
                                    )
                                    OnboardingStep.Avatar -> AvatarStep(
                                        ctx = ctx,
                                        selected = avatar,
                                        onSelect = { avatarOrdinal = it.ordinal }
                                    )
                                    OnboardingStep.Badges -> BadgesStep()
                                    // Story moments never reach the frame.
                                    OnboardingStep.Welcome,
                                    OnboardingStep.Asleep,
                                    OnboardingStep.Awake -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }

        // One burst as the first word is celebrated. Draws nothing with reduced motion.
        if (step == OnboardingStep.FirstWordLearned) {
            key(step) { ConfettiView(modifier = Modifier.fillMaxSize()) }
        }
    }
}

/**
 * The glow behind everything, centred on wherever this step's Jepjep was measured, and gliding
 * there when the step changes. Steps without Jepjep (the goal and the badges) centre it on the page.
 * The welcome scene is opaque, so the glow only shows from the wake-up on.
 */
@Composable
private fun OnboardingGlow(
    step: OnboardingStep,
    spots: JepjepSpots,
    content: @Composable BoxScope.() -> Unit
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val center: Offset? = spots.centers[step]
    val targetX = if (center != null && bounds.width > 0f) {
        ((center.x - bounds.left) / bounds.width).coerceIn(0f, 1f)
    } else DEFAULT_GLOW_X
    val targetY = if (center != null && bounds.height > 0f) {
        ((center.y - bounds.top) / bounds.height).coerceIn(0f, 1f)
    } else DEFAULT_GLOW_Y
    val x by animateFloatAsState(targetX, motionTween(Motion.Emphasized), label = "GlowX")
    val y by animateFloatAsState(targetY, motionTween(Motion.Emphasized), label = "GlowY")

    GlowBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { bounds = Rect(it.positionInRoot(), it.size.toSize()) },
        centerX = x,
        centerY = y,
        breathe = step.celebrates,
        content = content
    )
}

// ── Defaults. Skipping applies these, so each has to be a sensible real value. ──
private const val DEFAULT_NAME = "Kasiguranin Learner"
private const val DEFAULT_TITLE = "Kasiguranin Apprentice"
private const val DEFAULT_GLOW_X = 0.5f
private const val DEFAULT_GLOW_Y = 0.45f
