package com.kasiguru.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.ui.components.CoastPillButton
import com.kasiguru.ui.components.ConfettiView
import com.kasiguru.ui.components.MascotOwlSlot
import com.kasiguru.ui.components.OnboardingDotStepper
import com.kasiguru.ui.components.PillVariant
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

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

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Top stepper
                OnboardingDotStepper(
                    total = 6,
                    current = step - 1,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

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
}

@Composable
private fun Step1Welcome(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF2D1B69), PlayPurpleStart)))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MascotOwlSlot(size = 96.dp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = CircleShape,
                            color = XpGold
                        ) {
                            Text(
                                text = "CASIGURAN, AURORA",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = CoastInk
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Magandang Aldew!",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = TextWhite,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Welcome to KasiGuru",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Preserve & Learn Kasiguranin",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = TextHeadingBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Join thousands of learners in discovering the indigenous language, stories, and culture of Casiguran, Aurora through bite-sized games.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSubtleGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        CoastPillButton(
            label = "Get started",
            onClick = onNext,
            variant = PillVariant.Gold,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Step2Motivation(
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    val options = listOf(
        MotivationOption("Cultural Heritage", "Connecting with my roots & family history", Iconsax.Teacher, PlayPurpleStart),
        MotivationOption("Family & Relatives", "Talking with elders & relatives", Iconsax.People, PlayGoldStart),
        MotivationOption("Travel & Exploration", "Visiting Casiguran, Aurora", Iconsax.Global, PlayPinkStart),
        MotivationOption("Linguistic Interest", "Exploring indigenous Philippine languages", Iconsax.BookBold, PlayPurpleStart)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Why are you learning Kasiguranin?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = TextHeadingBlack
            )
            Text(
                text = "We will customize your daily recommendations based on your goal.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSubtleGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            options.forEach { item ->
                val isSelected = selected == item.title
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSelect(item.title) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) item.accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.5.dp, item.accentColor) else null,
                    shadowElevation = if (isSelected) 0.dp else 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = item.accentColor,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = null,
                                    tint = TextHeadingBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = TextHeadingBlack
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSubtleGray
                            )
                        }
                    }
                }
            }
        }

        CoastPillButton(label = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
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
        "Complete Beginner" to "I know 0 words — start from scratch 🐣",
        "Some Basics" to "I know a few greetings & common words 🌿",
        "Intermediate" to "I can follow simple sentences 💬"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "What is your skill level?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = TextHeadingBlack
            )

            Spacer(modifier = Modifier.height(16.dp))

            levels.forEach { (title, subtitle) ->
                val isSelected = selectedLevel == title
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onLevelSelect(title) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) PlayGoldStart.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PlayGoldStart) else null,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextHeadingBlack
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSubtleGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Warm-Up Challenge Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PlayPinkStart
                    ) {
                        Text(
                            text = "QUICK WARM-UP",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = TextHeadingBlack
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "What does \"Magandang Aldew\" mean?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextHeadingBlack,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isCorrect = warmUpAnswer == "Good Day"
                        val isWrong = warmUpAnswer == "Goodbye"

                        Button(
                            onClick = { onAnswerSelect("Good Day") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCorrect) Success else PlayGoldStart
                            )
                        ) {
                            Text(
                                text = if (isCorrect) "Correct!" else "Good Day",
                                color = TextHeadingBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { onAnswerSelect("Goodbye") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isWrong) Error else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "Goodbye",
                                color = TextHeadingBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        CoastPillButton(label = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Step4DailyGoal(
    selectedXp: Int,
    onSelectXp: (Int) -> Unit,
    onNext: () -> Unit
) {
    val goals = listOf(
        GoalOption(50, "Casual", "5 mins / day", "50 XP", Iconsax.Flash, PlayGoldStart),
        GoalOption(100, "Regular", "10 mins / day", "100 XP", Iconsax.StarBold, PlayPurpleStart),
        GoalOption(150, "Serious", "15 mins / day", "150 XP", Iconsax.BookBold, PlayPinkStart),
        GoalOption(200, "Intense", "20 mins / day", "200 XP", Iconsax.Cup, PlayGoldStart)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Pick your daily learning target",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = TextHeadingBlack
            )
            Text(
                text = "You can change your daily commitment goal anytime in settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSubtleGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            goals.forEach { item ->
                val isSelected = selectedXp == item.xp
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSelectXp(item.xp) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) item.accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.5.dp, item.accentColor) else null,
                    shadowElevation = if (isSelected) 0.dp else 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = item.accentColor,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = item.iconRes),
                                        contentDescription = null,
                                        tint = TextHeadingBlack,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = TextHeadingBlack
                                )
                                Text(
                                    text = item.time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSubtleGray
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = item.accentColor
                        ) {
                            Text(
                                text = item.xpTarget,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = TextHeadingBlack
                            )
                        }
                    }
                }
            }
        }

        CoastPillButton(label = "Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

private data class GoalOption(val xp: Int, val title: String, val time: String, val xpTarget: String, val iconRes: Int, val accentColor: Color)

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
    val avatars = listOf(1, 2, 3, 4, 5, 6)
    val titles = listOf("Kasiguranin Apprentice", "Cultural Explorer", "Linguistic Scholar")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Customize Your Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = TextHeadingBlack
            )
            Text(
                text = "Choose your avatar and display name for the leaderboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSubtleGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Display Name Input
            OutlinedTextField(
                value = userName,
                onValueChange = onNameChange,
                label = { Text("Display Name / Handle", fontWeight = FontWeight.Bold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PlayPurpleStart,
                    unfocusedBorderColor = TextSubtleGray.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Choose Profile Avatar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = TextHeadingBlack
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                avatars.forEach { avatarId ->
                    val isSelected = selectedAvatarId == avatarId
                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable { onAvatarSelect(avatarId) },
                        shape = CircleShape,
                        color = if (isSelected) PlayPurpleStart else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, PlayGoldStart) else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (avatarId) {
                                    1 -> "🐣"
                                    2 -> "🌿"
                                    3 -> "🌊"
                                    4 -> "🦅"
                                    5 -> "👑"
                                    else -> "⭐"
                                },
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Choose Starting Title Badge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = TextHeadingBlack
            )

            Spacer(modifier = Modifier.height(10.dp))

            titles.forEach { title ->
                val isSelected = selectedTitleBadge == title
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onTitleSelect(title) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) PlayPurpleStart.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PlayPurpleStart) else null
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = TextHeadingBlack
                    )
                }
            }
        }

        CoastPillButton(label = "Create profile", onClick = onNext, modifier = Modifier.fillMaxWidth())
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
                fontWeight = FontWeight.Black,
                color = TextHeadingBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(PlayGoldStart, PlayGoldEnd)))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = "WELCOME BONUS",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = TextHeadingBlack
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "+50 XP Awarded!",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = TextHeadingBlack
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "🔥 1-Day Streak Flame Lit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextHeadingBlack.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Personalized Plan Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Your personalized learning plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextHeadingBlack
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Learner Name: $userName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHeadingBlack,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Primary Motivation: $motivation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSubtleGray
                    )
                    Text(
                        text = "• Daily Target: $dailyGoalXp XP / day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSubtleGray
                    )
                    Text(
                        text = "• Estimated Growth: ~35 Kasiguranin words in Week 1",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHeadingBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        CoastPillButton(
            label = "Start learning",
            onClick = onStartJourney,
            variant = PillVariant.Gold,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
