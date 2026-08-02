package com.kasiguru.ui.screens.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    onNavigateBack: () -> Unit,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val displayedVocabulary = remember(searchQuery, uiState.filteredVocabulary) {
        if (searchQuery.isBlank()) {
            uiState.filteredVocabulary
        } else {
            uiState.filteredVocabulary.filter {
                it.kasiguranin.contains(searchQuery, ignoreCase = true) ||
                        it.tagalog.contains(searchQuery, ignoreCase = true) ||
                        it.english.contains(searchQuery, ignoreCase = true)
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Kasiguranin, Tagalog, or English...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = TextSubtleGray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = TextHeadingBlack,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            // Category Chips Row
            val allCategories = listOf("All") + uiState.categories
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategories) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VocabCardStart,
                            selectedLabelColor = TextHeadingBlack,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = TextSubtleGray
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Vocabulary List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedVocabulary, key = { it.id }) { vocab ->
                    VocabularyCard(
                        vocab = vocab,
                        onMarkLearned = { viewModel.markWordAsLearned(vocab.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun VocabularyCard(
    vocab: VocabularyEntity,
    onMarkLearned: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = vocab.kasiguranin,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextHeadingBlack
                        )
                        if (vocab.category.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = VocabCardStart.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = vocab.category,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextHeadingBlack
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "English: ${vocab.english}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHeadingBlack
                    )
                    Text(
                        text = "Tagalog: ${vocab.tagalog}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSubtleGray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { /* Play audio simulation */ }) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "Listen",
                            tint = VocabCardEnd,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = onMarkLearned) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Mark as Learned",
                            tint = if (vocab.isLearned) Success else TextSubtleGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (vocab.phoneticGlottal || vocab.phoneticVowelLength || vocab.ipaNotation.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            if (vocab.phoneticGlottal) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Glottal Stop ʔ", style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = StoriesCardStart)
                                )
                            }
                            if (vocab.phoneticVowelLength) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Long Vowel ː", style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = QuestsCardStart)
                                )
                            }
                            if (vocab.ipaNotation.isNotEmpty()) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("[${vocab.ipaNotation}]", style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = HeroCardStart)
                                )
                            }
                        }
                    }

                    if (vocab.neutralForm.isNotEmpty()) {
                        Text(
                            text = "Verb Aspect Inflections:",
                            style = MaterialTheme.typography.labelMedium,
                            color = VocabCardEnd,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AspectRow("Neutral (Infinitive)", vocab.neutralForm)
                        AspectRow("Imperfective (Present)", vocab.imperfectiveForm)
                        AspectRow("Perfective (Past)", vocab.perfectiveForm)
                        AspectRow("Contemplative (Future)", vocab.contemplativeForm)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    if (vocab.exampleSentence.isNotEmpty()) {
                        Text(
                            text = "Example Sentence:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextHeadingBlack,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${vocab.exampleSentence}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = TextSubtleGray
                        )
                        if (vocab.exampleTranslation.isNotEmpty()) {
                            Text(
                                text = "(${vocab.exampleTranslation})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSubtleGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSubtleGray)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextHeadingBlack)
    }
}
