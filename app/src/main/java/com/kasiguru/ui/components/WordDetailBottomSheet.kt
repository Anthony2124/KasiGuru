package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    onPlayAudio: (String) -> Unit = {}
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
                        color = CoastInk,
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

                IconButton(
                    onClick = { onPlayAudio(vocab.audioFileName) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(XpGold)
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.VolumeHigh),
                        contentDescription = "Play Pronunciation",
                        tint = CoastInk,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phonetic Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vocab.phoneticGlottal) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = PlayPurpleStart.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Glottal Stop ʔ",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = PlayPurpleStart
                        )
                    }
                }
                if (vocab.phoneticVowelLength) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = XpGold.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Long Vowel ː",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = XpGoldDark
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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Verb Aspects
            if (vocab.neutralForm.isNotEmpty()) {
                Text(
                    text = "Verb Aspect Inflections",
                    style = MaterialTheme.typography.titleSmall,
                    color = PlayPurpleStart,
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
                    color = PlayPurpleStart,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${vocab.exampleSentence}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = CoastInk
                )
                Text(
                    text = vocab.exampleTranslation,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoastMuted
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
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = CoastMuted)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = CoastInk, fontWeight = FontWeight.SemiBold)
        }
    }
}
