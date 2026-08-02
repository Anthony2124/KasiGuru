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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasiguranin Dictionary") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryContainer,
                            selectedLabelColor = PrimaryDark
                        )
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (vocab.isLearned) LightSurfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vocab.kasiguranin,
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${vocab.english} / ${vocab.tagalog}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { /* TODO: Play Audio */ },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Play Pronunciation",
                            tint = Primary
                        )
                    }
                    
                    IconButton(
                        onClick = onMarkLearned,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (vocab.isLearned) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Mark as Learned",
                            tint = if (vocab.isLearned) Success else TextGray
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
                                    label = { Text("Glottal Stop ʔ", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            if (vocab.phoneticVowelLength) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Long Vowel ː", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            if (vocab.ipaNotation.isNotEmpty()) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("[${vocab.ipaNotation}]", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    if (vocab.neutralForm.isNotEmpty()) {
                        Text(
                            text = "Verb Aspect Inflections:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Accent,
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
                            color = Primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${vocab.exampleSentence}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = vocab.exampleTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
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
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextGray)
        Text(text = form, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
