package com.kasiguru.ui.screens.cultural

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CulturalScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kasiguranin Heritage",
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
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
                                colors = listOf(StoriesCardStart, StoriesCardEnd)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = "Casiguran, Aurora",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextHeadingBlack
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "The Kasiguranin People & Language",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = TextHeadingBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Explore the unique Agta-Dumagat contact history, linguistic affixes, and coastal heritage of northern Aurora.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHeadingBlack.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Heritage Cards
            HeritageInfoCard(
                title = "Geographic Location",
                description = "Casiguran is a coastal municipality situated in the northern part of Aurora province, Luzon. It is bounded by the Sierra Madre mountain range to the west and the Pacific Ocean to the east.",
                icon = Icons.Rounded.LocationOn,
                cardBg = HeroCardStart
            )

            HeritageInfoCard(
                title = "Language Family & Contact",
                description = "Kasiguranin belongs to the Northern Luzon sub-branch of Malayo-Polynesian languages. It exhibits extensive historical contact with Casiguran Dumagat Agta, resulting in unique lexical and phonetic borrowings.",
                icon = Icons.Rounded.Groups,
                cardBg = VocabCardStart
            )

            HeritageInfoCard(
                title = "Linguistic Grammar Features",
                description = "Kasiguranin features a rich aspectual verb system (neutral, imperfective, perfective, contemplative), unique glottal stops ʔ, long vowels ː, and predicate-initial word order.",
                icon = Icons.Rounded.HistoryEdu,
                cardBg = QuestsCardStart
            )

            HeritageInfoCard(
                title = "Preservation Efforts",
                description = "Documented by UP Diliman linguistics research ('A Grammatical Sketch of Kasiguranin', 2016). KasiGuru serves to keep the language alive for future generations.",
                icon = Icons.Rounded.AccountBalance,
                cardBg = MiniGamesCardStart
            )
        }
    }
}

@Composable
private fun HeritageInfoCard(
    title: String,
    description: String,
    icon: ImageVector,
    cardBg: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cardBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextHeadingBlack,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadingBlack
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSubtleGray,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
