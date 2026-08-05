package com.kasiguru.ui.screens.vocabulary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    val haptic = LocalHapticFeedback.current

    val displayedCategories = remember(searchQuery, CategoryRegistry.categories) {
        if (searchQuery.isBlank()) {
            CategoryRegistry.categories
        } else {
            CategoryRegistry.categories.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kasiguranin Dictionary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
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
                            tint = TextSubtleGray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = TextHeadingBlack,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )
            }

            // ─── 2. Top Summary Progress Bento Card ───
            item(span = { GridItemSpan(2) }) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(HeroCardStart, HeroCardEnd)
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
                                    color = Color.White.copy(alpha = 0.45f)
                                ) {
                                    Text(
                                        text = "CORPUS OVERVIEW",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextHeadingBlack
                                    )
                                }

                                Text(
                                    text = "${uiState.totalLearnedCount} / ${uiState.allVocabulary.size} Learned",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextHeadingBlack
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "12 Vocabulary Sections",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = TextHeadingBlack
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Tap any Bento section below to view full word list & audio practice.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextHeadingBlack.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            KasiGuruProgressBar(
                                progress = if (uiState.allVocabulary.isNotEmpty()) {
                                    uiState.totalLearnedCount.toFloat() / uiState.allVocabulary.size.toFloat()
                                } else 0f,
                                height = 6.dp,
                                gradientColors = listOf(VocabCardStart, VocabCardEnd),
                                animated = true
                            )
                        }
                    }
                }
            }

            // ─── 3. Grid Header ───
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Categories Grid",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadingBlack,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // ─── 4. Uniform 2-Column Bento Cards (Exact match to reference image 8942cb8e-7aff-4720-86ca-09dc8fdafd7a.jpg) ───
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
 * 1:1 Pixel-Matched Bento Grid Card matching reference screenshot 8942cb8e-7aff-4720-86ca-09dc8fdafd7a.jpg:
 * - 2-Column equal size layout (height 145dp, corner radius 28dp)
 * - Top-left: White circular icon badge (48dp)
 * - Top-right: Word count number
 * - Bottom-left: Bold Category Title & "X learned" subtext
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
            .height(145.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(meta.startColor, meta.endColor)
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Icon Circle (Left) & Total Word Count (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (meta.customDrawableRes != null) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = meta.customDrawableRes),
                                contentDescription = meta.name,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = meta.iconRes),
                                contentDescription = meta.name,
                                tint = TextHeadingBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Text(
                        text = "${stats.totalWords}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                }

                // Bottom Column: Category Name (Bold) & Learned Count Subtext
                Column {
                    Text(
                        text = meta.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextHeadingBlack,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${stats.learnedWords} learned",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHeadingBlack.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
