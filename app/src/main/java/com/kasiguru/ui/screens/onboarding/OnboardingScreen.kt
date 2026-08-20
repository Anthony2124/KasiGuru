package com.kasiguru.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.kasiguru.ui.components.CasiguranAvatarPortrait
import com.kasiguru.ui.components.CasiguranResident
import com.kasiguru.ui.components.ConfettiView
import com.kasiguru.ui.components.MascotOwlSlot
import com.kasiguru.ui.components.OnboardingDotStepper
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.ClaySurface
import com.kasiguru.ui.components.clay.GlassChip
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.CanopyBottom
import com.kasiguru.ui.theme.CanopyTop
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.LocalDarkMode
import com.kasiguru.ui.theme.StatusBarIcons
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.Red
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletTint

/**
 * First-run wizard. Not a [com.kasiguru.ui.components.clay.CanopyScaffold] host — a wizard's steps
 * don't split into "who you are" versus "the work" the way a tab root does — but step 1's hero and
 * step 6's reward panel each draw on canopy/clay tokens directly, so the flow still reads as the same
 * app a returning learner lands in on Learn.
 */
@Composable
fun OnboardingScreen(
    onCompleteOnboarding: (userName: String, avatarId: Int, dailyGoalXp: Int, motivation: String, startingLevel: String, titleBadge: String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val haptic = LocalHapticFeedback.current

    // User selections
    var motivation by remember { mutableStateOf("Cultural Heritage") }
    var skillLevel by remember { mutableStateOf("Complete Beginner") }
    var warmUpAnswer by remember { mutableStateOf<String?>(null) }
    var showConfetti by remember { mutableStateOf(false) }
    var dailyGoalXp by remember { mutableIntStateOf(100) } // Default 10 mins
    var userName by remember { mutableStateOf("Kasiguranin Learner") }
    var selectedAvatarId by remember { mutableIntStateOf(1) }
    var selectedTitleBadge by remember { mutableStateOf("Kasiguranin Apprentice") }

    BackHandler(enabled = step > 1) { step -= 1 }

    // The wizard draws Ground behind the status bar. Icons were forced light app-wide on the premise
    // that every screen opened with the violet canopy, which was never true here - white glyphs on
    // #F1EEFF measure about 1.05:1 and were simply invisible.
    StatusBarIcons(dark = !LocalDarkMode.current)

    Box(modifier = Modifier.fillMaxSize().background(Ground).statusBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Space.gutter, vertical = Space.sm)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(Space.xl), contentAlignment = Alignment.Center) {
                    if (step > 1) {
                        IconButton(onClick = { step -= 1 }) {
                            Icon(
                                painter = painterResource(id = Iconsax.ArrowLeft),
                                contentDescription = "Back",
                                tint = Muted
                            )
                        }
                    }
                }
                OnboardingDotStepper(
                    total = 6,
                    current = step - 1,
                    activeColor = Violet,
                    modifier = Modifier.weight(1f).padding(horizontal = Space.sm),
                )
                Spacer(Modifier.size(Space.xl))
            }

            Spacer(modifier = Modifier.height(Space.sm))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "OnboardingStepTransition",
                modifier = Modifier.weight(1f)
            ) { currentStep ->
                when (currentStep) {
                    1 -> Step1Welcome(onNext = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        step = 2
                    })
                    2 -> Step2Motivation(
                        selected = motivation,
                        onSelect = { motivation = it },
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            step = 3
                        }
                    )
                    3 -> Step3SkillAndWarmUp(
                        selectedLevel = skillLevel,
                        onLevelSelect = { skillLevel = it },
                        warmUpAnswer = warmUpAnswer,
                        onAnswerSelect = { answer ->
                            warmUpAnswer = answer
                            if (answer == "Good Day") {
                                showConfetti = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            step = 4
                        }
                    )
                    4 -> Step4DailyGoal(
                        selectedXp = dailyGoalXp,
                        onSelectXp = { dailyGoalXp = it },
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            step = 5
                        }
                    )
                    5 -> Step5ProfileSetup(
                        userName = userName,
                        onNameChange = { userName = it },
                        selectedAvatarId = selectedAvatarId,
                        onAvatarSelect = { selectedAvatarId = it },
                        selectedTitleBadge = selectedTitleBadge,
                        onTitleSelect = { selectedTitleBadge = it },
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            step = 6
                        }
                    )
                    6 -> Step6SummaryAndBonus(
                        userName = userName,
                        dailyGoalXp = dailyGoalXp,
                        motivation = motivation,
                        onStartJourney = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCompleteOnboarding(
                                userName.ifBlank { "Kasiguranin Learner" },
                                selectedAvatarId,
                                dailyGoalXp,
                                motivation,
                                skillLevel,
                                selectedTitleBadge
                            )
                        }
                    )
                }
            }
        }

        if (showConfetti) {
            ConfettiView(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun Step1Welcome(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.panel)
                    .background(Brush.verticalGradient(listOf(CanopyTop, CanopyBottom)))
                    .padding(Space.lg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MascotOwlSlot(size = 88.dp)
                    Spacer(modifier = Modifier.height(Space.sm))
                    GlassChip {
                        Text(
                            text = "Casiguran, Aurora",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnCanopy
                        )
                    }
                    Spacer(modifier = Modifier.height(Space.md))
                    Text(
                        text = "Magandang Aldew!",
                        style = MaterialTheme.typography.displayMedium,
                        color = OnCanopy,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Space.xxs))
                    Text(
                        text = "Welcome to KasiGuru",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnCanopy
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space.xl))

            Text(
                text = "Preserve & Learn Kasiguranin",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Space.sm))
            Text(
                text = "Join thousands of learners discovering the indigenous language, stories, and culture of Casiguran, Aurora through bite-sized lessons.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Space.sm)
            )
        }

        ClayButton(label = "Get started", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Step2Motivation(
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    val options = listOf(
        MotivationOption("Cultural Heritage", "Connecting with my roots & family history", Iconsax.Teacher, Violet),
        MotivationOption("Family & Relatives", "Talking with elders & relatives", Iconsax.People, Gold),
        MotivationOption("Travel & Exploration", "Visiting Casiguran, Aurora", Iconsax.Global, Coral),
        MotivationOption("Linguistic Interest", "Exploring indigenous Philippine languages", Iconsax.BookBold, Violet)
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            StepHeading("Why are you learning Kasiguranin?", "We'll shape your daily recommendations around your goal.")
            Spacer(modifier = Modifier.height(Space.lg))
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                options.forEach { item ->
                    SelectableRow(
                        title = item.title,
                        subtitle = item.subtitle,
                        iconRes = item.iconRes,
                        accent = item.accentColor,
                        isSelected = selected == item.title,
                        onClick = { onSelect(item.title) }
                    )
                }
            }
        }
        ClayButton(label = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

private data class MotivationOption(val title: String, val subtitle: String, val iconRes: Int, val accentColor: Color)

@Composable
private fun Step3SkillAndWarmUp(
    selectedLevel: String,
    onLevelSelect: (String) -> Unit,
    warmUpAnswer: String?,
    onAnswerSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    val levels = listOf(
        "Complete Beginner" to "I know 0 words — start from scratch",
        "Some Basics" to "I know a few greetings & common words",
        "Intermediate" to "I can follow simple sentences"
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            StepHeading("What is your skill level?", null)
            Spacer(modifier = Modifier.height(Space.md))
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                levels.forEach { (title, subtitle) ->
                    SelectableRow(
                        title = title,
                        subtitle = subtitle,
                        isSelected = selectedLevel == title,
                        onClick = { onLevelSelect(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space.lg))

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "What does \"Magandang Aldew\" mean?",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Space.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.sm)
                    ) {
                        val isCorrect = warmUpAnswer == "Good Day"
                        val isWrong = warmUpAnswer == "Goodbye"
                        WarmUpChoice(
                            label = if (isCorrect) "Correct!" else "Good Day",
                            state = if (isCorrect) WarmUpState.Correct else WarmUpState.Idle,
                            onClick = { onAnswerSelect("Good Day") },
                            modifier = Modifier.weight(1f)
                        )
                        WarmUpChoice(
                            label = "Goodbye",
                            state = if (isWrong) WarmUpState.Wrong else WarmUpState.Idle,
                            onClick = { onAnswerSelect("Goodbye") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        ClayButton(label = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

private enum class WarmUpState { Idle, Correct, Wrong }

@Composable
private fun WarmUpChoice(label: String, state: WarmUpState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val face = when (state) {
        WarmUpState.Idle -> VioletTint
        WarmUpState.Correct -> Green
        WarmUpState.Wrong -> Red
    }
    val labelColor = if (state == WarmUpState.Idle) Violet else Color.White
    Box(
        modifier = modifier
            .clip(Shapes.tile)
            .background(face)
            .clickable(onClick = onClick)
            .padding(vertical = Space.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = labelColor)
    }
}

@Composable
private fun Step4DailyGoal(
    selectedXp: Int,
    onSelectXp: (Int) -> Unit,
    onNext: () -> Unit
) {
    val goals = listOf(
        GoalOption(50, "Casual", "5 mins / day", "50 XP", Iconsax.Flash, Gold),
        GoalOption(100, "Regular", "10 mins / day", "100 XP", Iconsax.StarBold, Violet),
        GoalOption(150, "Serious", "15 mins / day", "150 XP", Iconsax.BookBold, Coral),
        GoalOption(200, "Intense", "20 mins / day", "200 XP", Iconsax.Cup, Gold)
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            StepHeading("Pick your daily learning target", "You can change this anytime in settings.")
            Spacer(modifier = Modifier.height(Space.lg))
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                goals.forEach { item ->
                    SelectableRow(
                        title = item.title,
                        subtitle = item.time,
                        iconRes = item.iconRes,
                        accent = item.accentColor,
                        isSelected = selectedXp == item.xp,
                        onClick = { onSelectXp(item.xp) },
                        trailing = {
                            Box(
                                modifier = Modifier.clip(Shapes.chip).background(item.accentColor.copy(alpha = 0.16f))
                                    .padding(horizontal = Space.xs, vertical = Space.xxs)
                            ) {
                                Text(
                                    text = item.xpTarget,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = item.accentColor
                                )
                            }
                        }
                    )
                }
            }
        }
        ClayButton(label = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

private data class GoalOption(val xp: Int, val title: String, val time: String, val xpTarget: String, val iconRes: Int, val accentColor: Color)

private val onboardingAvatars = listOf(
    CasiguranResident.STUDENT,
    CasiguranResident.TEACHER,
    CasiguranResident.ELDER,
    CasiguranResident.SURFER,
    CasiguranResident.MUSICIAN,
    CasiguranResident.FARMER
)

@Composable
private fun Step5ProfileSetup(
    userName: String,
    onNameChange: (String) -> Unit,
    selectedAvatarId: Int,
    onAvatarSelect: (Int) -> Unit,
    selectedTitleBadge: String,
    onTitleSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    val titles = listOf("Kasiguranin Apprentice", "Cultural Explorer", "Linguistic Scholar")

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            StepHeading("Customize your profile", "Choose your avatar and display name for the leaderboard.")
            Spacer(modifier = Modifier.height(Space.md))

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

            Spacer(modifier = Modifier.height(Space.lg))
            Text(text = "Choose your avatar", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(modifier = Modifier.height(Space.sm))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                onboardingAvatars.forEachIndexed { index, resident ->
                    val avatarId = index + 1
                    val isSelected = selectedAvatarId == avatarId
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(
                                if (isSelected) Modifier.border(3.dp, Violet, CircleShape).padding(2.dp)
                                else Modifier.padding(2.dp)
                            )
                    ) {
                        CasiguranAvatarPortrait(
                            resident = resident,
                            size = 48.dp,
                            showLevelRing = false,
                            onClick = { onAvatarSelect(avatarId) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space.lg))
            Text(text = "Choose your starting title", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(modifier = Modifier.height(Space.sm))

            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                titles.forEach { title ->
                    val isSelected = selectedTitleBadge == title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shapes.tile)
                            .background(if (isSelected) Violet else VioletTint)
                            .clickable { onTitleSelect(title) }
                            .padding(horizontal = Space.md, vertical = Space.sm)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) Color.White else Violet
                        )
                    }
                }
            }
        }
        ClayButton(label = "Create profile", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Step6SummaryAndBonus(
    userName: String,
    dailyGoalXp: Int,
    motivation: String,
    onStartJourney: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Your journey begins",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Space.lg))

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
                    Text(
                        text = "+50 XP awarded",
                        style = MaterialTheme.typography.headlineMedium,
                        color = RewardInk
                    )
                    Spacer(modifier = Modifier.height(Space.xxs))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = Iconsax.FlashBold),
                            contentDescription = null,
                            tint = RewardInk,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(Space.xxs))
                        Text(
                            text = "Day 1 streak lit",
                            style = MaterialTheme.typography.titleMedium,
                            color = RewardInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space.lg))

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Your personalized learning plan", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(modifier = Modifier.height(Space.md))
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    SummaryRow(Iconsax.Profile, "Learner name", userName)
                    SummaryRow(Iconsax.Teacher, "Primary motivation", motivation)
                    SummaryRow(Iconsax.FlashBold, "Daily target", "$dailyGoalXp XP / day")
                    SummaryRow(Iconsax.BookBold, "Estimated growth", "~35 words in week 1")
                }
            }
        }
        ClayButton(
            label = "Start learning",
            onClick = onStartJourney,
            tone = ClayButtonTone.Reward,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SummaryRow(iconRes: Int, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Space.xs))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = Ink, textAlign = TextAlign.End)
    }
}

@Composable
private fun StepHeading(title: String, subtitle: String?) {
    Text(text = title, style = MaterialTheme.typography.headlineMedium, color = Ink)
    if (subtitle != null) {
        Spacer(modifier = Modifier.height(Space.xxs))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

/**
 * Shared row for every single-select list in the wizard (motivation, skill level, daily goal): an
 * optional accent icon chip, a title/subtitle pair, and either a caller-supplied trailing slot or the
 * default selected checkmark. One composable so the three lists read as one control, not three.
 */
@Composable
private fun SelectableRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    accent: Color = Violet,
    trailing: (@Composable () -> Unit)? = null
) {
    SoftCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isSelected) Modifier.border(2.dp, Violet, Shapes.tile) else Modifier),
        shape = Shapes.tile,
        color = if (isSelected) VioletTint else com.kasiguru.ui.theme.Surface,
        elevation = if (isSelected) 2.dp else 6.dp,
        onClick = onClick,
        contentPadding = PaddingValues(Space.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Box(
                    modifier = Modifier.size(44.dp).clip(Shapes.chip).background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
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
