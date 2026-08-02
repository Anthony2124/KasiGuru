package com.kasiguru.ui.screens.stories

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
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
import com.kasiguru.R
import com.kasiguru.data.local.entity.StoryPage
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.theme.*

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.components.WordDetailBottomSheet

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StoryReaderScreen(
    onNavigateBack: () -> Unit,
    viewModel: StoryReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle completion navigation
    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.story?.title ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.pages.isNotEmpty()) {
                StoryBottomBar(
                    currentPage = uiState.currentPageIndex + 1,
                    totalPages = uiState.pages.size,
                    onPrevious = viewModel::previousPage,
                    onNext = viewModel::nextPage
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StoriesCardStart)
            }
            return@Scaffold
        }

        if (uiState.error != null || uiState.pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Failed to load story", color = Error)
            }
            return@Scaffold
        }

        val page = uiState.pages[uiState.currentPageIndex]

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            modifier = Modifier.padding(padding)
        ) { targetPage ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Illustration Placeholder Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(StoriesCardStart, StoriesCardEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = targetPage.illustrationDesc,
                        color = TextHeadingBlack,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Kasiguranin Interactive Words
                var selectedVocab by remember { mutableStateOf<VocabularyEntity?>(null) }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    targetPage.kasiguranin.split(" ").forEach { word ->
                        SuggestionChip(
                            onClick = {
                                selectedVocab = VocabularyEntity(
                                    kasiguranin = word.replace(Regex("[^a-zA-ZáéíóúÁÉÍÓÚñÑ]"), ""),
                                    tagalog = "Tap for meaning",
                                    english = "Word in story context",
                                    rootForm = word.lowercase(),
                                    category = "Story Vocabulary"
                                )
                            },
                            label = {
                                Text(
                                    text = word,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextHeadingBlack,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = HeroCardStart),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                if (selectedVocab != null) {
                    WordDetailBottomSheet(
                        vocab = selectedVocab!!,
                        onDismissRequest = { selectedVocab = null }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Audio Button
                Button(
                    onClick = { /* Play Audio */ },
                    colors = ButtonDefaults.buttonColors(containerColor = StoriesCardStart),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_volume_high),
                        contentDescription = "Play Audio",
                        tint = TextHeadingBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pakinggan (Listen)", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(24.dp))

                // Tagalog Translation
                Text(
                    text = "Tagalog",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSubtleGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = targetPage.tagalog,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextHeadingBlack,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // English Translation
                Text(
                    text = "English",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSubtleGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = targetPage.english,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = TextSubtleGray,
                    textAlign = TextAlign.Center
                )
            }
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
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            KasiGuruProgressBar(
                progress = currentPage.toFloat() / totalPages.toFloat(),
                height = 4.dp,
                gradientColors = listOf(StoriesCardStart, StoriesCardEnd),
                animated = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPrevious,
                    enabled = currentPage > 1
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_left),
                        contentDescription = "Previous",
                        tint = if (currentPage > 1) TextHeadingBlack else TextSubtleGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", color = if (currentPage > 1) TextHeadingBlack else TextSubtleGray)
                }

                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSubtleGray,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = StoriesCardStart)
                ) {
                    if (currentPage == totalPages) {
                        Text("Finish", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Finish",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Next", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Next",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
