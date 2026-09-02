package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailBottomSheet(
    vocab: VocabularyEntity,
    onDismissRequest: () -> Unit,
    onPlayAudio: (String) -> Unit = {},
    onReportWord: ((String) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.45f)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vocab.kasiguranin,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Ink,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${vocab.english} • ${vocab.tagalog}",
                        style = MaterialTheme.typography.titleMedium,
                        color = VocabSea,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AudioPlayButton(onClick = { onPlayAudio(vocab.audioFileName) })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phonetic Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vocab.phoneticGlottal) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = CanopyTop.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Glottal Stop ʔ",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = CanopyTop
                        )
                    }
                }
                if (vocab.phoneticVowelLength) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Gold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Long Vowel ː",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldDeep
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = VocabSea.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = vocab.category,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = VocabSea
                    )
                }
            }

            // What the word means, as opposed to what it translates to. Absent entirely when no
            // definition has been written for this entry yet.
            if (vocab.meaningEnglish.isNotEmpty() || vocab.meaningTagalog.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Meaning",
                    style = MaterialTheme.typography.titleSmall,
                    color = CanopyTop,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (vocab.meaningEnglish.isNotEmpty()) {
                    Text(
                        text = vocab.meaningEnglish,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink
                    )
                }
                if (vocab.meaningTagalog.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = vocab.meaningTagalog,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Verb Aspects
            if (vocab.neutralForm.isNotEmpty()) {
                Text(
                    text = "Verb Aspect Inflections",
                    style = MaterialTheme.typography.titleSmall,
                    color = CanopyTop,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp
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
                    color = CanopyTop,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                    Spacer(modifier = Modifier.height(12.dp))
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

            if (onReportWord != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Faint.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onReportWord(vocab.kasiguranin)
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.InfoCircle),
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Report an issue with this word",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
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
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Muted)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}
