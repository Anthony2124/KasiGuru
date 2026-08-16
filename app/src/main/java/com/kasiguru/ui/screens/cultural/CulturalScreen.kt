package com.kasiguru.ui.screens.cultural

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CulturalScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "Kasiguranin Heritage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextHeadingBlack,
                        letterSpacing = (-0.3).sp
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
                shadowElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(StoriesDusk, PlayPurpleStart)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = XpGold
                        ) {
                            Text(
                                text = "CASIGURAN, AURORA",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = CoastInk,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "The Kasiguranin People & Language",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Explore the unique Agta-Dumagat contact history, linguistic affixes, and coastal heritage of northern Aurora.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Heritage Cards
            HeritageInfoCard(
                title = "Geographic Location",
                description = "Casiguran is a coastal municipality situated in northern Aurora province, Luzon. It is bounded by the Sierra Madre mountain range to the west and the Pacific Ocean to the east.",
                iconRes = Iconsax.Location,
                accentColor = VocabSea
            )

            HeritageInfoCard(
                title = "Language Family & Contact",
                description = "Kasiguranin belongs to the Northern Luzon sub-branch of Malayo-Polynesian languages. It exhibits extensive historical contact with Casiguran Dumagat Agta, resulting in unique lexical and phonetic borrowings.",
                iconRes = Iconsax.People,
                accentColor = XpGold
            )

            HeritageInfoCard(
                title = "Linguistic Grammar Features",
                description = "Kasiguranin features a rich aspectual verb system (neutral, imperfective, perfective, contemplative), unique glottal stops ʔ, long vowels ː, and predicate-initial word order.",
                iconRes = Iconsax.Teacher,
                accentColor = PlayPurpleStart
            )

            HeritageInfoCard(
                title = "Preservation Efforts",
                description = "Documented by UP Diliman linguistics research ('A Grammatical Sketch of Kasiguranin', 2016). KasiGuru serves to keep the language alive for future generations.",
                iconRes = Iconsax.Courthouse,
                accentColor = GamesCoral
            )
        }
    }
}

@Composable
private fun HeritageInfoCard(
    title: String,
    description: String,
    iconRes: Int,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoastInk,
                    letterSpacing = (-0.2).sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = CoastMuted,
                lineHeight = 22.sp
            )
        }
    }
}
