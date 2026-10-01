package com.kasiguru.ui.screens.vocabulary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.ClayFab
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.GreenText
import com.kasiguru.ui.theme.GreenTint
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.KasiguraninHeadword
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.LimeText
import com.kasiguru.ui.theme.LimeTint
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnLime
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.util.audio.AudioPlayerManager

/**
 * One word as a full dictionary entry: the screen a learner reaches by tapping a word in the
 * Library, and the route a push notification links into (`vocabulary/{wordId}`).
 *
 * Laid out the way a printed dictionary lays out an entry, top to bottom:
 *  - the headword, large, with the play button beside it;
 *  - the pronunciation in IPA and the part of speech on one line, then the labels (category,
 *    glottal stop, long vowel);
 *  - the sense: the English gloss in bold, its definition, then the Tagalog gloss and definition;
 *  - the examples, each sentence over its translation;
 *  - for verbs, the aspect forms as a table;
 *  - a note on what the pronunciation labels mean;
 *  - "Same meaning": other recorded words sharing a gloss, then "More from" the same category;
 *  - the learner's own record of the word, and the report link.
 *
 * Everything shown is already in the corpus; sections with nothing recorded are left out rather than
 * printed empty.
 */
@Composable
fun VocabularyDetailScreen(
    onNavigateBack: () -> Unit,
    onReportWord: ((String) -> Unit)? = null,
    onOpenWord: ((Int) -> Unit)? = null,
    viewModel: VocabularyDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allWords by viewModel.allWords.collectAsState()

    val context = LocalContext.current
    val audioPlayerManager = remember { AudioPlayerManager(context) }
    DisposableEffect(Unit) { onDispose { audioPlayerManager.stopAudio() } }

    GroundScaffold(
        title = uiState.word?.kasiguranin ?: "Word",
        onBack = onNavigateBack,
        pattern = GroundPattern.None,
        content = {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Lime)
                }
                uiState.notFound -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This word couldn't be found.", color = Muted, style = MaterialTheme.typography.bodyLarge)
                }
                else -> uiState.word?.let { vocab ->
                    val related = remember(vocab.id, allWords) { relatedWords(vocab, allWords) }
                    DictionaryEntry(
                        vocab = vocab,
                        related = related,
                        onPlayAudio = { audioPlayerManager.playWord(vocab) },
                        onToggleLearned = viewModel::markWordAsLearned,
                        onReportWord = onReportWord,
                        onOpenWord = onOpenWord
                    )
                }
            }
        }
    )
}

/** Words to look at next: [sameMeaning] shares a gloss with this one, [sameCategory] sits beside it in the dictionary. */
internal data class RelatedWords(
    val sameMeaning: List<VocabularyEntity>,
    val sameCategory: List<VocabularyEntity>
) {
    val isEmpty get() = sameMeaning.isEmpty() && sameCategory.isEmpty()
}

/**
 * Words sharing this one's English or Tagalog gloss (a dictionary's synonyms), up to four, and up to
 * four more from the same category. Only recorded words are offered.
 */
internal fun relatedWords(word: VocabularyEntity, all: List<VocabularyEntity>): RelatedWords {
    fun glosses(w: VocabularyEntity) = (w.english.split(',', ';') + w.tagalog.split(',', ';'))
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()
    val mine = glosses(word)
    val others = all.filter { it.id != word.id && it.kasiguranin.isNotBlank() }
        .distinctBy { it.kasiguranin.lowercase() }
    val sameMeaning = others.filter { glosses(it).any(mine::contains) }.take(4)
    val sameCategory = others.filter { it.category == word.category && it !in sameMeaning }.take(4)
    return RelatedWords(sameMeaning, sameCategory)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryEntry(
    vocab: VocabularyEntity,
    related: RelatedWords,
    onPlayAudio: () -> Unit,
    onToggleLearned: () -> Unit,
    onReportWord: ((String) -> Unit)?,
    onOpenWord: ((Int) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = Space.xs, bottom = Space.navBarClearance)
    ) {
        // ── Headword ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = vocab.kasiguranin,
                    style = KasiguraninHeadword,
                    color = Ink,
                    modifier = Modifier.semantics { heading() }
                )
                val pronunciationLine = listOfNotNull(
                    vocab.ipaNotation.takeIf { it.isNotBlank() }?.let { "[$it]" },
                    vocab.partOfSpeech.takeIf { it.isNotBlank() }?.lowercase()
                )
                if (pronunciationLine.isNotEmpty()) {
                    Spacer(Modifier.height(Space.xxs))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        vocab.ipaNotation.takeIf { it.isNotBlank() }?.let {
                            Text("[$it]", style = MaterialTheme.typography.bodyLarge, color = Muted)
                        }
                        if (pronunciationLine.size == 2) {
                            Text("  ·  ", style = MaterialTheme.typography.bodyLarge, color = Faint)
                        }
                        vocab.partOfSpeech.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it.lowercase(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic,
                                color = LimeText
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(Space.sm))
            ClayFab(onClick = onPlayAudio, contentDescription = "Play pronunciation", size = 56.dp) {
                Icon(
                    painter = painterResource(id = Iconsax.VolumeHigh),
                    contentDescription = null,
                    tint = OnLime,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ── Labels ──
        Spacer(Modifier.height(Space.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            if (vocab.category.isNotBlank()) EntryLabel(vocab.category)
            if (vocab.phoneticGlottal) EntryLabel("Glottal stop ʔ")
            if (vocab.phoneticVowelLength) EntryLabel("Long vowel ː")
            if (vocab.isLearned) EntryLabel("Learned", tint = GreenTint, ink = GreenText)
        }

        EntryDivider()

        // ── The sense ──
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Lime),
                contentAlignment = Alignment.Center
            ) {
                Text("1", style = MaterialTheme.typography.labelLarge, color = OnLime)
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = vocab.english.ifBlank { vocab.meaningEnglish },
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink
                )
                if (vocab.meaningEnglish.isNotBlank() && vocab.meaningEnglish != vocab.english) {
                    Spacer(Modifier.height(Space.xxs))
                    Text(
                        text = vocab.meaningEnglish,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink
                    )
                }
                if (vocab.tagalog.isNotBlank() || vocab.meaningTagalog.isNotBlank()) {
                    Spacer(Modifier.height(Space.md))
                    LanguageLabel("Tagalog")
                    Spacer(Modifier.height(Space.xxs))
                    if (vocab.tagalog.isNotBlank()) {
                        Text(vocab.tagalog, style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                    if (vocab.meaningTagalog.isNotBlank()) {
                        Text(vocab.meaningTagalog, style = MaterialTheme.typography.bodyMedium, color = Muted)
                    }
                }
            }
        }

        // ── Examples ──
        val examples = listOf(
            vocab.exampleSentence to vocab.exampleTranslation,
            vocab.exampleSentence2 to vocab.exampleTranslation2
        ).filter { it.first.isNotBlank() }
        if (examples.isNotEmpty()) {
            EntryDivider()
            SectionTitle(if (examples.size == 1) "Example" else "Examples")
            Spacer(Modifier.height(Space.sm))
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                examples.forEach { (sentence, translation) -> ExampleQuote(sentence, translation) }
            }
        }

        // ── Verb forms ──
        val forms = listOf(
            "Root" to vocab.rootForm,
            "Neutral" to vocab.neutralForm,
            "Imperfective · present" to vocab.imperfectiveForm,
            "Perfective · past" to vocab.perfectiveForm,
            "Contemplative · future" to vocab.contemplativeForm
        ).filter { it.second.isNotBlank() }
        if (vocab.neutralForm.isNotBlank() && forms.isNotEmpty()) {
            EntryDivider()
            SectionTitle("Verb forms")
            Spacer(Modifier.height(Space.sm))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(Shapes.tile)
                    .border(1.dp, BorderHairline, Shapes.tile)
            ) {
                forms.forEachIndexed { index, (label, value) ->
                    if (index > 0) HorizontalDivider(color = BorderHairline)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) SurfaceSunken else androidx.compose.ui.graphics.Color.Transparent)
                            .padding(horizontal = Space.md, vertical = Space.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.weight(1f))
                        Text(value, style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                }
            }
        }

        // ── Pronunciation notes ──
        if (vocab.phoneticGlottal || vocab.phoneticVowelLength) {
            EntryDivider()
            SectionTitle("Pronunciation")
            Spacer(Modifier.height(Space.xs))
            if (vocab.phoneticGlottal) {
                NoteLine("ʔ", "Glottal stop: a short catch in the throat, as in the middle of \"uh-oh\".")
            }
            if (vocab.phoneticVowelLength) {
                NoteLine("ː", "Long vowel: the marked vowel is held a little longer.")
            }
        }

        // ── See also ──
        if (!related.isEmpty && onOpenWord != null) {
            if (related.sameMeaning.isNotEmpty()) {
                EntryDivider()
                SectionTitle("Same meaning")
                Spacer(Modifier.height(Space.sm))
                WordLinkList(related.sameMeaning, onOpenWord)
            }
            if (related.sameCategory.isNotEmpty()) {
                EntryDivider()
                SectionTitle("More from ${vocab.category}")
                Spacer(Modifier.height(Space.sm))
                WordLinkList(related.sameCategory, onOpenWord)
            }
        }

        // ── Your record of this word ──
        EntryDivider()
        SoftCard(modifier = Modifier.fillMaxWidth(), shape = Shapes.tile, border = BorderHairline) {
            Text("Your progress", style = MaterialTheme.typography.titleSmall, color = Ink)
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = when {
                    vocab.isLearned && vocab.nextReviewDate.isNotBlank() ->
                        "Learned · reviewed ${vocab.timesReviewed} ${if (vocab.timesReviewed == 1) "time" else "times"} · next review ${vocab.nextReviewDate}"
                    vocab.isLearned -> "Learned · in your review deck"
                    else -> "Not in your review deck yet. Mark it learned to start reviewing it."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            if (!vocab.isLearned) {
                Spacer(Modifier.height(Space.sm))
                ClayButton(
                    label = "Mark as learned",
                    onClick = onToggleLearned,
                    tone = ClayButtonTone.Quiet,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (onReportWord != null) {
            Spacer(Modifier.height(Space.sm))
            TextButton(
                onClick = { onReportWord(vocab.kasiguranin) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.InfoCircle),
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Report an issue with this word",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }
    }
}

@Composable
private fun EntryDivider() {
    Spacer(Modifier.height(Space.lg))
    HorizontalDivider(color = BorderHairline)
    Spacer(Modifier.height(Space.lg))
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Ink,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun LanguageLabel(text: String) {
    Text(text = text.uppercase(), style = MaterialTheme.typography.labelMedium, color = LimeText)
}

@Composable
private fun EntryLabel(
    text: String,
    tint: androidx.compose.ui.graphics.Color = LimeTint,
    ink: androidx.compose.ui.graphics.Color = LimeText
) {
    Box(
        modifier = Modifier
            .clip(Shapes.pill)
            .background(tint)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = ink)
    }
}

/** A sentence over its translation, set off by a lime rule like a quotation in a printed dictionary. */
@Composable
private fun ExampleQuote(sentence: String, translation: String) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(Shapes.pill)
                .background(Lime)
        )
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(sentence, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic, color = Ink)
            if (translation.isNotBlank()) {
                Text(translation, style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        }
    }
}

@Composable
private fun NoteLine(symbol: String, text: String) {
    Row(Modifier.padding(vertical = Space.xxs), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(28.dp)
                .clip(Shapes.chip)
                .background(SurfaceSunken),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, style = MaterialTheme.typography.titleMedium, color = Ink)
        }
        Spacer(Modifier.width(Space.sm))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.weight(1f))
    }
}

/** Other entries as rows: the headword, its glosses, and a chevron. Each opens that entry. */
@Composable
private fun WordLinkList(words: List<VocabularyEntity>, onOpenWord: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Shapes.tile)
            .border(1.dp, BorderHairline, Shapes.tile)
    ) {
        words.forEachIndexed { index, word ->
            if (index > 0) HorizontalDivider(color = BorderHairline)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Open ${word.kasiguranin}") { onOpenWord(word.id) }
                    .padding(horizontal = Space.md, vertical = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(word.kasiguranin, style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text(
                        listOf(word.english, word.tagalog).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        maxLines = 1
                    )
                }
                Icon(painterResource(Iconsax.ArrowRight), null, tint = Faint, modifier = Modifier.size(16.dp))
            }
        }
    }
}
