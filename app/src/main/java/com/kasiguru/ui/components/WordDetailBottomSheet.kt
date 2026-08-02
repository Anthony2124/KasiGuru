package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailBottomSheet(
    vocab: VocabularyEntity,
    onDismissRequest: () -> Unit,
    onPlayAudio: (String) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = DarkSurface,
        scrimColor = DarkBackground.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vocab.kasiguranin,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${vocab.english} / ${vocab.tagalog}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Secondary
                    )
                }

                IconButton(
                    onClick = { onPlayAudio(vocab.audioFileName) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Play Pronunciation",
                        tint = TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phonetic Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                SuggestionChip(
                    onClick = {},
                    label = { Text(vocab.category, style = MaterialTheme.typography.labelSmall) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DarkSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Verb Aspects
            if (vocab.neutralForm.isNotEmpty()) {
                Text(
                    text = "Verb Aspect Inflections",
                    style = MaterialTheme.typography.titleSmall,
                    color = Accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                AspectDetailRow("Root", vocab.rootForm)
                AspectDetailRow("Neutral", vocab.neutralForm)
                AspectDetailRow("Imperfective (Present)", vocab.imperfectiveForm)
                AspectDetailRow("Perfective (Past)", vocab.perfectiveForm)
                AspectDetailRow("Contemplative (Future)", vocab.contemplativeForm)
            }

            if (vocab.exampleSentence.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Example Sentence",
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryLight,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${vocab.exampleSentence}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = TextWhite
                )
                Text(
                    text = vocab.exampleTranslation,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AspectDetailRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextGray)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextWhite, fontWeight = FontWeight.SemiBold)
        }
    }
}
