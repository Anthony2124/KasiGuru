package com.kasiguru.ui.screens.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.components.AudioPlayButton
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet
import com.kasiguru.util.audio.AudioPlayerManager

/**
 * A single word, as its own pushed destination. Reached from `vocabulary/{wordId}` — the route a
 * backend push notification links into (functions/send_push.js) — as well as any other future
 * direct link to one word, distinct from [WordDetailBottomSheet] which is a same-screen overlay.
 */
@Composable
fun VocabularyDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: VocabularyDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val audioPlayerManager = remember { AudioPlayerManager(context) }
    DisposableEffect(Unit) { onDispose { audioPlayerManager.stopAudio() } }

    GroundScaffold(
        // The headword, not the literal string "Word". This screen is what a push notification
        // deep-links into, and it used to open without ever naming the word it was about.
        title = uiState.word?.kasiguranin ?: "Word",
        onBack = onNavigateBack,
        pattern = GroundPattern.Orbs,
        compactTitle = true,
        content = {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Violet)
                }
                uiState.notFound -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This word couldn't be found.", color = Muted, style = MaterialTheme.typography.bodyLarge)
                }
                else -> uiState.word?.let { vocab ->
                    VocabularyDetailBody(vocab = vocab, onPlayAudio = {
                        audioPlayerManager.playAudio(vocab.kasiguranin, vocab.audioFileName)
                    })
                }
            }
        }
    )
}

@Composable
private fun VocabularyDetailBody(vocab: VocabularyEntity, onPlayAudio: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.gutter, vertical = Space.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${vocab.english} • ${vocab.tagalog}",
                style = MaterialTheme.typography.titleMedium,
                color = Muted,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            AudioPlayButton(onClick = onPlayAudio)
        }

        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            if (vocab.phoneticGlottal) {
                DetailBadge("Glottal Stop ʔ")
            }
            if (vocab.phoneticVowelLength) {
                DetailBadge("Long Vowel ː")
            }
            DetailBadge(vocab.category)
            if (vocab.partOfSpeech.isNotEmpty()) {
                DetailBadge(vocab.partOfSpeech)
            }
        }

        if (vocab.ipaNotation.isNotEmpty()) {
            Spacer(Modifier.height(Space.xs))
            Text(
                text = "[${vocab.ipaNotation}]",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
        }

        // What the word means, as opposed to what it translates to. Hidden entirely when no
        // definition has been written yet, so an un-backfilled entry reads as a shorter card
        // rather than an empty heading.
        if (vocab.meaningEnglish.isNotEmpty() || vocab.meaningTagalog.isNotEmpty()) {
            Spacer(Modifier.height(Space.md))
            Text(
                text = "Meaning",
                style = MaterialTheme.typography.titleSmall,
                color = Violet,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(Space.xxs))
            if (vocab.meaningEnglish.isNotEmpty()) {
                Text(
                    text = vocab.meaningEnglish,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink
                )
            }
            if (vocab.meaningTagalog.isNotEmpty()) {
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = vocab.meaningTagalog,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
        }

        Spacer(Modifier.height(Space.md))
        HorizontalDivider(color = Faint)
        Spacer(Modifier.height(Space.md))

        if (vocab.neutralForm.isNotEmpty()) {
            Text(
                text = "Verb Aspect Inflections",
                style = MaterialTheme.typography.titleSmall,
                color = Violet,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(Space.xs))
            DetailRow("Root", vocab.rootForm)
            DetailRow("Neutral", vocab.neutralForm)
            DetailRow("Imperfective (Present)", vocab.imperfectiveForm)
            DetailRow("Perfective (Past)", vocab.perfectiveForm)
            DetailRow("Contemplative (Future)", vocab.contemplativeForm)
        }

        if (vocab.exampleSentence.isNotEmpty()) {
            Spacer(Modifier.height(Space.md))
            Text(
                text = "Example Sentence",
                style = MaterialTheme.typography.titleSmall,
                color = Violet,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = "\"${vocab.exampleSentence}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = Ink
            )
            Text(
                text = vocab.exampleTranslation,
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )

            if (vocab.exampleSentence2.isNotEmpty()) {
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = "\"${vocab.exampleSentence2}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = Ink
                )
                Text(
                    text = vocab.exampleTranslation2,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }
    }
}

@Composable
private fun DetailBadge(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = Violet.copy(alpha = 0.12f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Violet
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Muted)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}
