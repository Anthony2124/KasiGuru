package com.kasiguru.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.R
import com.kasiguru.ui.theme.*

data class OnboardingPage(
    val iconRes: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            iconRes = R.drawable.ic_book_outline,
            title = "Welcome to KasiGuru",
            subtitle = "Preserving the Kasiguranin Language",
            description = "Discover the unique Northern Agta language spoken in Casiguran, Aurora. Learn authentic vocabulary, grammar, and stories.",
            accentColor = HeroCardStart
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_game_outline,
            title = "Learn Through Play",
            subtitle = "Interactive Mini-Games & Stories",
            description = "Master verb aspects, word order, and vocabulary through fun challenges, games, and interactive storybooks.",
            accentColor = MiniGamesCardStart
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_volume_high,
            title = "Authentic Phonetics",
            subtitle = "IPA Notation & Audio Practice",
            description = "Practice glottal stops, vowel length, and accurate pronunciation with audio controls and IPA phonetics.",
            accentColor = StoriesCardStart
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    var selectedGoal by remember { mutableIntStateOf(10) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar (Skip Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinishOnboarding) {
                    Text("Skip", color = TextSubtleGray, fontWeight = FontWeight.Bold)
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val item = pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = item.accentColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = null,
                                tint = TextHeadingBlack,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = TextHeadingBlack,
                        textAlign = TextAlign.Center,
                        fontSize = 28.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextHeadingBlack,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSubtleGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    if (page == pages.size - 1) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Set your daily goal:",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextHeadingBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(5, 10, 15).forEach { goal ->
                                FilterChip(
                                    selected = selectedGoal == goal,
                                    onClick = { selectedGoal = goal },
                                    label = { Text("$goal words/day", fontWeight = FontWeight.Medium) },
                                    leadingIcon = if (selectedGoal == goal) {
                                        { Icon(painter = painterResource(id = R.drawable.ic_tick_circle), contentDescription = null, modifier = Modifier.size(16.dp), tint = TextHeadingBlack) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HeroCardStart,
                                        selectedLabelColor = TextHeadingBlack
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Pager Indicator & Next/Get Started Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (pagerState.currentPage == index) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (pagerState.currentPage == index) TextHeadingBlack else TextSubtleGray.copy(alpha = 0.3f))
                        )
                    }
                }

                Button(
                    onClick = onFinishOnboarding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pages[pagerState.currentPage].accentColor)
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.size - 1) "Start Learning Kasiguranin 🚀" else "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                }
            }
        }
    }
}
