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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.R
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    onNavigateBack: () -> Unit,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                            painter = painterResource(id = R.drawable.ic_arrow_left),
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
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VocabCardEnd)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Categories Row
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
                items(uiState.filteredVocabulary, key = { it.id }) { vocab ->
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (vocab.isLearned) QuestsCardStart.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vocab.kasiguranin,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextHeadingBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${vocab.english} / ${vocab.tagalog}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSubtleGray
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { /* Play Audio */ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(HeroCardStart.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_volume_high),
                            contentDescription = "Play Pronunciation",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onMarkLearned,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tick_circle),
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
                            color = TextHeadingBlack
                        )
                        Text(
                            text = vocab.exampleTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSubtleGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectRow(label: String, form: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSubtleGray)
        Text(text = form, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextHeadingBlack)
    }
}
