package com.kasiguru.ui.screens.stories

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.StoryPage
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalContext
import com.kasiguru.ui.components.ConfettiView
import com.kasiguru.ui.components.WordDetailBottomSheet
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.ClayCircle
import com.kasiguru.util.Constants
import com.kasiguru.util.audio.AudioPlayerManager

/**
 * Immersive on purpose, like Lesson Player and Flashcards: no canopy, no bottom nav, so reading one
 * page doesn't feel like a detour through app chrome.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StoryReaderScreen(
    onNavigateBack: () -> Unit,
    viewModel: StoryReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val audioPlayerManager = remember { AudioPlayerManager(context) }
    DisposableEffect(Unit) { onDispose { audioPlayerManager.stopAudio() } }

    // A finished story used to close the screen the instant the last page's XP landed, with no
    // acknowledgement of what was just read — a Peak-End violation despite XP genuinely being
    // awarded. This holds on the last page and shows a real completion moment instead.
    if (uiState.isFinished) {
        StoryCompleteContent(
            storyTitle = uiState.story?.title ?: "Story",
            xpEarned = Constants.XP_PER_STORY_COMPLETE,
            onDone = onNavigateBack
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Ground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Space.gutter, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = Iconsax.ArrowLeft),
                contentDescription = "Back",
                tint = Muted,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onNavigateBack)
            )
            Spacer(Modifier.width(Space.sm))
            Text(
                text = uiState.story?.title ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
                maxLines = 1
            )
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet)
            }
            return@Column
        }

        if (uiState.error != null || uiState.pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Failed to load story", color = Red)
            }
            return@Column
        }

        val page = uiState.pages[uiState.currentPageIndex]

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            modifier = Modifier.weight(1f)
        ) { targetPage ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Space.gutter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Illustration Placeholder Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(Shapes.panel)
                        .background(brush = Brush.linearGradient(colors = listOf(CanopyTop, CanopyBottom))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = targetPage.illustrationDesc,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(Space.md)
                    )
                }

                Spacer(Modifier.height(Space.lg))

                // Kasiguranin Interactive Words
                var selectedWord by remember { mutableStateOf<String?>(null) }
                var wordNotFound by remember { mutableStateOf(false) }

                // A story whose Kasiguranin has not been authored yet shows none of this rather than a
                // row of empty chips: "".split(" ") returns a single blank entry, not nothing.
                val hasKasiguranin = targetPage.kasiguranin.isNotBlank()

                if (hasKasiguranin) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    targetPage.kasiguranin.split(" ").forEach { word ->
                        SuggestionChip(
                            onClick = {
                                val vocab = viewModel.findWord(word)
                                if (vocab != null) {
                                    selectedWord = word
                                } else {
                                    wordNotFound = true
                                }
                            },
                            label = {
                                Text(
                                    text = word,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Violet,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Violet.copy(alpha = 0.12f)),
                            border = null,
                            shape = Shapes.chip
                        )
                    }
                }

                selectedWord?.let { word ->
                    val vocab = remember(word, uiState.vocabulary) { viewModel.findWord(word) }
                    if (vocab != null) {
                        WordDetailBottomSheet(
                            vocab = vocab,
                            onDismissRequest = { selectedWord = null },
                            onPlayAudio = { audioPlayerManager.playAudio(vocab.kasiguranin, vocab.audioFileName) }
                        )
                    }
                }

                if (wordNotFound) {
                    AlertDialog(
                        onDismissRequest = { wordNotFound = false },
                        title = { Text("Not in the dictionary yet") },
                        text = { Text("This word isn't in the vocabulary list yet, so there's no definition to show.") },
                        confirmButton = {
                            TextButton(onClick = { wordNotFound = false }) { Text("Got it") }
                        }
                    )
                }

                Spacer(Modifier.height(Space.md))

                // Audio Button
                Button(
                    onClick = { audioPlayerManager.playAudio(targetPage.kasiguranin, targetPage.audioFileName) },
                    colors = ButtonDefaults.buttonColors(containerColor = Violet),
                    shape = Shapes.tile
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.VolumeHigh),
                        contentDescription = "Play Audio",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(Space.xs))
                    Text("Pakinggan (Listen)", color = Color.White, fontWeight = FontWeight.Bold)
                }
                }

                if (hasKasiguranin) {
                    Spacer(Modifier.height(Space.lg))
                    HorizontalDivider(color = Faint.copy(alpha = 0.3f))
                }
                Spacer(Modifier.height(Space.lg))

                // Tagalog. Promoted to the page's own voice when no Kasiguranin sits above it, rather
                // than staying a supporting translation of something absent.
                Text(text = "Tagalog", style = MaterialTheme.typography.labelMedium, color = Muted)
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = targetPage.tagalog,
                    style = if (hasKasiguranin) {
                        MaterialTheme.typography.bodyLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = Ink,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(Space.md))

                // English Translation
                Text(text = "English", style = MaterialTheme.typography.labelMedium, color = Muted)
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = targetPage.english,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (uiState.pages.isNotEmpty()) {
            StoryBottomBar(
                currentPage = uiState.currentPageIndex + 1,
                totalPages = uiState.pages.size,
                onPrevious = viewModel::previousPage,
                onNext = viewModel::nextPage
            )
        }
    }
}

@Composable
private fun StoryCompleteContent(storyTitle: String, xpEarned: Int, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Ground)) {
        ConfettiView(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(Space.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ClayCircle(size = 96.dp, face = Gold, lipColor = GoldDeep) {
                Icon(
                    painter = painterResource(id = Iconsax.TickCircle),
                    contentDescription = null,
                    tint = RewardInk,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(Space.lg))
            Text(
                text = "Story Complete!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = "You finished \"$storyTitle\" and earned $xpEarned XP.",
                style = MaterialTheme.typography.bodyLarge,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.lg))
            ClayButton(
                label = "Continue",
                onClick = onDone,
                tone = ClayButtonTone.Reward,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StoryBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(color = Surface, tonalElevation = 8.dp, shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(horizontal = Space.gutter, vertical = Space.sm)) {
            KasiGuruProgressBar(
                progress = currentPage.toFloat() / totalPages.toFloat(),
                height = 4.dp,
                gradientColors = listOf(CanopyTop, CanopyBottom),
                animated = true
            )
            Spacer(Modifier.height(Space.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPrevious, enabled = currentPage > 1) {
                    Icon(
                        painter = painterResource(id = Iconsax.ArrowLeft),
                        contentDescription = "Previous",
                        tint = if (currentPage > 1) Ink else Faint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(Space.xxs))
                    Text("Back", color = if (currentPage > 1) Ink else Faint)
                }

                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.labelLarge,
                    color = Muted,
                    fontWeight = FontWeight.Bold
                )

                Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = Violet), shape = Shapes.pill) {
                    if (currentPage == totalPages) {
                        Text("Finish", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(Space.xxs))
                        Icon(
                            painter = painterResource(id = Iconsax.TickCircle),
                            contentDescription = "Finish",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Next", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(Space.xxs))
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowRight),
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
