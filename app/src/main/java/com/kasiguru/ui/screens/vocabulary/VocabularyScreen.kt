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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.PlayStatChip
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

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

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "Kasiguranin Dictionary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextHeadingBlack
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
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )
            }

            // ─── 2. Top Summary Progress Bento Hero Card (Play Purple Palette) ───
            item(span = { GridItemSpan(2) }) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PlayPurpleStart, PlayPurpleEnd)
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
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "DICTIONARY CORPUS",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = TextWhite
                                    )
                                }

                                PlayStatChip(
                                    text = "${uiState.totalLearnedCount} / ${uiState.allVocabulary.size} Learned",
                                    icon = Iconsax.BookBold,
                                    backgroundColor = PlayGoldStart,
                                    contentColor = TextHeadingBlack
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "12 Vocabulary Sections",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Explore audio-guided words, audio pronunciations, and memory reviews.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextWhite.copy(alpha = 0.9f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            KasiGuruProgressBar(
                                progress = if (uiState.allVocabulary.isNotEmpty()) {
                                    uiState.totalLearnedCount.toFloat() / uiState.allVocabulary.size.toFloat()
                                } else 0f,
                                height = 8.dp,
                                gradientColors = listOf(PlayGoldStart, PlayGoldEnd),
                                animated = true
                            )
                        }
                    }
                }
            }

            // ─── 3. Filter Chips Row ───
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
                            shape = CircleShape,
                            color = if (isSelected) PlayPurpleStart else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 2.dp else 1.dp
                        ) {
                            Text(
                                text = filterName,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) TextWhite else TextSubtleGray
                            )
                        }
                    }
                }
            }

            // ─── 4. Grid Header ───
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Categories Grid",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadingBlack,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // ─── 5. Uniform 2-Column Bento Cards with Play Palette ───
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
 * 2-Column Bento Grid Card reskinned with Play theme gradients and crisp typography.
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
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (meta.customDrawableRes != null) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = meta.customDrawableRes),
                                contentDescription = meta.name,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = meta.iconRes),
                                contentDescription = meta.name,
                                tint = TextWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "${stats.totalWords} wds",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = TextWhite
                        )
                    }
                }

                // Bottom Column: Category Title & Learned Count Subtext
                Column {
                    Text(
                        text = meta.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${stats.learnedWords} learned",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextWhite.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
