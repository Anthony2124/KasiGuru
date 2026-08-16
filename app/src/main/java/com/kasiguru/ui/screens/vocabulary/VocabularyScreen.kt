package com.kasiguru.ui.screens.vocabulary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.WordOfTheDayStrip
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.util.audio.AudioPlayerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("All") }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayerManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stopAudio()
        }
    }

    val filterOptions = listOf("All", "Essentials", "Food", "Animals", "Body", "Numbers", "House", "Nature")

    val displayedCategories = remember(searchQuery, selectedFilterCategory, CategoryRegistry.categories) {
        CategoryRegistry.categories.filter { meta ->
            val matchesSearch = searchQuery.isBlank() ||
                    meta.name.contains(searchQuery, ignoreCase = true) ||
                    meta.description.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedFilterCategory == "All" ||
                    meta.name.contains(selectedFilterCategory, ignoreCase = true)

            matchesSearch && matchesCategory
        }
    }

    val featuredWord = remember(uiState.allVocabulary) {
        uiState.allVocabulary.firstOrNull { it.kasiguranin.isNotBlank() && it.tagalog.isNotBlank() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "Kasiguranin Dictionary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextHeadingBlack,
                        letterSpacing = (-0.3).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowLeft),
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─── 1. Top Search Bar ───
            item(span = { GridItemSpan(2) }) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search vocabulary categories...") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = Iconsax.Search),
                            contentDescription = "Search",
                            tint = PlayPurpleStart
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = PlayPurpleStart,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }

            // ─── 2. Word of the Day Strip ───
            item(span = { GridItemSpan(2) }) {
                WordOfTheDayStrip(
                    kasiguranin = featuredWord?.kasiguranin ?: "singët",
                    translation = if (featuredWord != null) "${featuredWord.tagalog} • ${featuredWord.english}" else "langgam • ant",
                    onPlayClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (featuredWord != null) {
                            audioPlayer.playAudio(featuredWord.kasiguranin, featuredWord.audioFileName)
                        } else {
                            audioPlayer.playAudio("singët", "")
                        }
                    }
                )
            }

            // ─── 3. Top Summary Progress Hero Card ───
            item(span = { GridItemSpan(2) }) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(VocabSea, VocabSeaDark)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.White.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = "DICTIONARY CORPUS",
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextWhite,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = XpGold
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = Iconsax.BookBold),
                                            contentDescription = null,
                                            tint = CoastInk,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${uiState.totalLearnedCount} / ${uiState.allVocabulary.size} Learned",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CoastInk
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "${CategoryRegistry.categories.size} Vocabulary Categories",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                letterSpacing = (-0.3).sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Audio-guided terms, phonetic transcriptions & memory cards.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextWhite.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val corpusProgress = if (uiState.allVocabulary.isNotEmpty()) {
                                uiState.totalLearnedCount.toFloat() / uiState.allVocabulary.size.toFloat()
                            } else 0f

                            KasiGuruProgressBar(
                                progress = corpusProgress,
                                height = 6.dp,
                                gradientColors = listOf(Color.White, Color.White.copy(alpha = 0.75f)),
                                animated = true
                            )
                        }
                    }
                }
            }

            // ─── 4. Filter Chips Row ───
            item(span = { GridItemSpan(2) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filterOptions) { filterName ->
                        val isSelected = selectedFilterCategory == filterName
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedFilterCategory = filterName
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelected) PlayPurpleStart else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = filterName,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) TextWhite else CoastMuted
                            )
                        }
                    }
                }
            }

            // ─── 5. Grid Header ───
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextHeadingBlack,
                    modifier = Modifier.padding(top = 4.dp),
                    letterSpacing = (-0.2).sp
                )
            }

            // ─── 6. Uniform 2-Column Bento Cards with Casiguran Coast Gradients ───
            items(displayedCategories, key = { it.name }) { meta ->
                val stats = uiState.categoryStats[meta.name] ?: CategoryProgressStats()

                UniformBentoCard(
                    meta = meta,
                    stats = stats,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToCategory(meta.name)
                    }
                )
            }
        }
    }
}

/**
 * 2-Column Bento Grid Card reskinned with Casiguran Coast gradients and crisp typography.
 */
@Composable
private fun UniformBentoCard(
    meta: CategoryMetaData,
    stats: CategoryProgressStats,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(meta.startColor, meta.endColor)
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Translucent Icon Circle (Left) & Total Word Count Pill (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (meta.customDrawableRes != null) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = meta.customDrawableRes),
                                contentDescription = meta.name,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = meta.iconRes),
                                contentDescription = meta.name,
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.28f)
                    ) {
                        Text(
                            text = "${stats.totalWords} wds",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            fontSize = 10.sp
                        )
                    }
                }

                // Bottom Column: Category Title & Learned Count Subtext
                Column {
                    Text(
                        text = meta.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${stats.learnedWords} learned",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextWhite.copy(alpha = 0.88f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
