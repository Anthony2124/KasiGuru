package com.kasiguru.ui.screens.vocabulary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.WordVerificationDialog
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.util.audio.AudioPlayerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryName: String,
    onNavigateBack: () -> Unit,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var verifyingWord by remember { mutableStateOf<VocabularyEntity?>(null) }
    var unlearningWord by remember { mutableStateOf<VocabularyEntity?>(null) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioPlayerManager = remember { AudioPlayerManager(context) }

    val meta = remember(categoryName) { CategoryRegistry.getMeta(categoryName) }

    LaunchedEffect(categoryName) {
        viewModel.selectCategory(categoryName)
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayerManager.stopAudio()
        }
    }

    val categoryWords = remember(searchQuery, uiState.filteredVocabulary) {
        val baseList = uiState.allVocabulary.filter { it.category.equals(categoryName, ignoreCase = true) }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.kasiguranin.contains(searchQuery, ignoreCase = true) ||
                        it.tagalog.contains(searchQuery, ignoreCase = true) ||
                        it.english.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalWords = categoryWords.size
    val learnedCount = categoryWords.count { it.isLearned }

    // Word Verification Dialog (Stricter Mechanics)
    verifyingWord?.let { word ->
        WordVerificationDialog(
            targetWord = word,
            allWords = uiState.allVocabulary,
            onSuccess = {
                viewModel.markWordAsLearned(word.id)
                verifyingWord = null
            },
            onDismiss = { verifyingWord = null }
        )
    }

    // Unlearn Reset Dialog
    unlearningWord?.let { word ->
        AlertDialog(
            onDismissRequest = { unlearningWord = null },
            title = { Text("Reset Learned Word?") },
            text = { Text("Reset '${word.kasiguranin}' back to unlearned so you can review it again?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unmarkWordAsLearned(word.id)
                        unlearningWord = null
                    }
                ) {
                    Text("Reset Word", color = Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { unlearningWord = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        categoryName,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── 1. Category Hero Banner ───
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(meta.startColor, meta.endColor)))
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = meta.iconRes),
                                        contentDescription = null,
                                        tint = TextWhite,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = PlayGoldStart
                                ) {
                                    Text(
                                        text = "$learnedCount / $totalWords Learned",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = TextHeadingBlack
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = meta.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = meta.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextWhite.copy(alpha = 0.9f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            KasiGuruProgressBar(
                                progress = if (totalWords > 0) learnedCount.toFloat() / totalWords.toFloat() else 0f,
                                height = 8.dp,
                                gradientColors = listOf(PlayGoldStart, PlayGoldEnd),
                                animated = true
                            )
                        }
                    }
                }
            }

            // ─── 2. Search Bar ───
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search in $categoryName...") },
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

            // ─── 3. Words List Header ───
            item {
                Text(
                    text = "Vocabulary List ($totalWords words)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadingBlack
                )
            }

            // ─── 4. Vocabulary Word Cards List ───
            items(categoryWords, key = { it.id }) { vocab ->
                CategoryWordCard(
                    vocab = vocab,
                    audioPlayerManager = audioPlayerManager,
                    onMarkLearned = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (vocab.isLearned) {
                            unlearningWord = vocab
                        } else {
                            verifyingWord = vocab
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryWordCard(
    vocab: VocabularyEntity,
    audioPlayerManager: AudioPlayerManager,
    onMarkLearned: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isExpanded = !isExpanded
            },
        shape = RoundedCornerShape(22.dp),
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
                    Text(
                        text = vocab.kasiguranin,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
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
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            audioPlayerManager.playAudio(vocab.kasiguranin, vocab.audioFileName)
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.VolumeHigh),
                            contentDescription = "Listen",
                            tint = VocabCardEnd,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = onMarkLearned) {
                        Icon(
                            painter = painterResource(id = Iconsax.TickCircle),
                            contentDescription = "Verify Mastery",
                            tint = if (vocab.isLearned) Success else TextSubtleGray,
                            modifier = Modifier.size(24.dp)
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
