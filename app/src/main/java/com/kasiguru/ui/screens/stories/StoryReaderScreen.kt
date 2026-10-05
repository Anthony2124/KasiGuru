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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.kasiguru.data.local.entity.StoryPage
import com.kasiguru.ui.components.ErrorDialog
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
import androidx.compose.runtime.LaunchedEffect
import com.kasiguru.util.audio.LocalSoundEffects
import com.kasiguru.util.audio.Sfx
import com.kasiguru.ui.components.tapSounds

/** The languages a page can be read in, in the order the switch offers them. */
private enum class PageLanguage(val label: String) { Kasiguranin("Kasiguranin"), Tagalog("Tagalog"), English("English") }

private fun StoryPage.textIn(language: PageLanguage): String = when (language) {
    PageLanguage.Kasiguranin -> kasiguranin
    PageLanguage.Tagalog -> tagalog
    PageLanguage.English -> english
}

/**
 * Immersive on purpose, like Lesson Player and Flashcards: no canopy, no bottom nav, so reading one
 * page doesn't feel like a detour through app chrome.
 *
 * Laid out as a picture book: a slim header (close, title, page count, a bar of page segments), the
 * illustration, then the page's text in one language at a time with a switch above it. The old page
 * stacked all three languages under each other, so the eye had to find its place again on every
 * page; one language at a time reads like a book, and the switch is one tap away for a word the
 * learner wants to check. The choice carries from page to page.
 *
 * There is no narration control. No recorded narration exists for the stories, and a Listen button
 * that played nothing promised something the app could not do. Single words still have their
 * pronunciation in the word sheet.
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

    uiState.error?.let { errorMsg ->
        ErrorDialog(
            message = errorMsg,
            onDismiss = {
                viewModel.clearError()
                onNavigateBack()
            }
        )
    }

    // A finished story used to close the screen the instant the last page's XP landed, with no
    // acknowledgement of what was just read — a Peak-End violation despite XP genuinely being
    // awarded. This holds on the last page and shows a real completion moment instead.
    if (uiState.isFinished) {
        StoryCompleteContent(
            storyTitle = uiState.story?.title ?: "Story",
            xpEarned = uiState.finalXp,
            onDone = onNavigateBack
        )
        return
    }

    // Which language the learner is reading in. Saved, so it survives rotation and carries across pages.
    var languageOrdinal by rememberSaveable { mutableStateOf(-1) }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var wordNotFound by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Ground)) {
        // ── Header: close, title, count, and the page segments ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Space.xs, end = Space.gutter, top = Space.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(id = Iconsax.CloseCircle),
                        contentDescription = "Close story",
                        tint = Muted
                    )
                }
                Text(
                    text = uiState.story?.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.pages.isNotEmpty()) {
                    Text(
                        text = "Page ${uiState.currentPageIndex + 1} of ${uiState.pages.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Muted
                    )
                }
            }
            if (uiState.pages.isNotEmpty()) {
                com.kasiguru.ui.components.SegmentedProgress(
                    completed = uiState.currentPageIndex + 1,
                    total = uiState.pages.size,
                    modifier = Modifier.fillMaxWidth().padding(start = Space.sm, top = Space.xxs)
                )
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Lime)
            }
            return@Column
        }

        if (uiState.error != null || uiState.pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (uiState.error == null) {
                    Text("No pages available", color = Muted)
                }
            }
            return@Column
        }

        val page = uiState.pages[uiState.currentPageIndex]

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                slideInHorizontally { width -> width / 4 } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width / 4 } + fadeOut()
            },
            modifier = Modifier.weight(1f),
            label = "StoryPage"
        ) { targetPage ->
            // Only the languages this page actually has; a story without authored Kasiguranin
            // simply opens in Tagalog.
            val available = PageLanguage.entries.filter { targetPage.textIn(it).isNotBlank() }
            val language = PageLanguage.entries.getOrNull(languageOrdinal)?.takeIf { it in available }
                ?: available.firstOrNull()
                ?: PageLanguage.Tagalog

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.gutter, vertical = Space.md)
            ) {
                // The page's illustration. Square, because that is the shape the artwork is
                // authored and stored in -- the admin centre-crops every upload to 1:1 -- so any
                // other ratio here would either letterbox it or crop it a second time.
                //
                // The gradient stays underneath rather than behind a loading spinner: it is the
                // finished state for a page with no picture, not a placeholder waiting to be
                // replaced, which is what DESIGN.md asks of every art slot.
                val pageImage = uiState.pageImages[targetPage.imageId]
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // Square, but never taller than about 40 % of the screen, so the text starts
                    // above the fold on a short phone.
                    val side = minOf(maxWidth, 340.dp)
                    Box(
                        modifier = Modifier
                            .size(side)
                            .clip(Shapes.panel)
                            .background(brush = Brush.linearGradient(colors = listOf(CanopyTop, CanopyBottom))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pageImage != null) {
                            AsyncImage(
                                model = pageImage,
                                // The description was written as an art brief; it doubles as the alt text.
                                contentDescription = targetPage.illustrationDesc.takeIf { it.isNotBlank() },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = targetPage.illustrationDesc,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(Space.md)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Space.lg))

                if (available.size > 1) {
                    com.kasiguru.ui.components.clay.SegmentedToggle(
                        options = available.map { it.label },
                        selectedIndex = available.indexOf(language).coerceAtLeast(0),
                        onSelect = { languageOrdinal = available[it].ordinal },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Space.md))
                }

                // The page's text, in the chosen language, set for reading rather than scanning.
                if (language == PageLanguage.Kasiguranin) {
                    // Every word can be looked up: set as running text, each word a quiet link
                    // with a dotted rule beneath, instead of a wall of chips.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(Space.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        targetPage.kasiguranin.split(" ").filter { it.isNotBlank() }.forEach { word ->
                            Text(
                                text = word,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Ink,
                                modifier = Modifier
                                    .clip(Shapes.chip)
                                    .clickable(onClickLabel = "Look up $word") {
                                        if (viewModel.findWord(word) != null) selectedWord = word else wordNotFound = true
                                    }
                                    .drawBehind {
                                        val y = size.height - 2.dp.toPx()
                                        drawLine(
                                            color = Lime,
                                            start = androidx.compose.ui.geometry.Offset(0f, y),
                                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                                            strokeWidth = 2.dp.toPx(),
                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                                floatArrayOf(3.dp.toPx(), 3.dp.toPx())
                                            )
                                        )
                                    }
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = "Tap a word to look it up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Faint
                    )
                } else {
                    Text(
                        text = targetPage.textIn(language),
                        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 32.sp),
                        fontWeight = FontWeight.Medium,
                        fontStyle = if (language == PageLanguage.English && available.size > 1) FontStyle.Italic else FontStyle.Normal,
                        color = Ink
                    )
                }
            }
        }

        selectedWord?.let { word ->
            val vocab = remember(word, uiState.vocabulary) { viewModel.findWord(word) }
            if (vocab != null) {
                WordDetailBottomSheet(
                    vocab = vocab,
                    onDismissRequest = { selectedWord = null },
                    onPlayAudio = { audioPlayerManager.playWord(vocab) }
                )
            }
        }

        if (wordNotFound) {
            AlertDialog(modifier = Modifier.tapSounds(), 
                onDismissRequest = { wordNotFound = false },
                title = { Text("Not in the dictionary yet") },
                text = { Text("This word isn't in the vocabulary list yet, so there's no definition to show.") },
                confirmButton = {
                    TextButton(onClick = { wordNotFound = false }) { Text("Got it") }
                }
            )
        }

        StoryBottomBar(
            currentPage = uiState.currentPageIndex + 1,
            totalPages = uiState.pages.size,
            onPrevious = viewModel::previousPage,
            onNext = viewModel::nextPage
        )
    }
}

@Composable
private fun StoryCompleteContent(storyTitle: String, xpEarned: Int, onDone: () -> Unit) {
    val sounds = LocalSoundEffects.current
    LaunchedEffect(Unit) { sounds?.play(Sfx.Complete) }
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

/** Previous as a quiet round button, Next (or Finish) as the one lime action. The progress lives in the header. */
@Composable
fun StoryBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Space.gutter, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val canGoBack = currentPage > 1
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(Shapes.pill)
                .background(SurfaceSunken)
                .clickable(enabled = canGoBack, onClickLabel = "Previous page", onClick = onPrevious),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = Iconsax.ArrowLeft),
                contentDescription = "Previous page",
                tint = if (canGoBack) Ink else Faint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(Space.sm))
        ClayButton(
            label = if (currentPage == totalPages) "Finish story" else "Next page",
            onClick = onNext,
            modifier = Modifier.weight(1f)
        )
    }
}
