package com.kasiguru.ui.screens.cultural

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.ui.components.clay.CanopyBackButton
import com.kasiguru.ui.components.clay.CanopyScaffold
import com.kasiguru.ui.components.clay.GlassChip
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@Composable
fun CulturalScreen(
    onNavigateBack: () -> Unit
) {
    CanopyScaffold(
        canopyHeight = 196.dp,
        canopyContent = {
            CanopyBackButton(onClick = onNavigateBack)
            Spacer(Modifier.height(Space.sm))
            Text(
                text = "The Kasiguranin People & Language",
                style = MaterialTheme.typography.headlineMedium,
                color = OnCanopy
            )
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = "Explore the unique Agta-Dumagat contact history, linguistic affixes, and coastal heritage of northern Aurora.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnCanopy
            )
            Spacer(Modifier.weight(1f))
            GlassChip {
                Icon(
                    painter = painterResource(id = Iconsax.Location),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text("Casiguran, Aurora", style = MaterialTheme.typography.labelMedium, color = OnCanopy)
            }
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Space.gutter),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                Spacer(Modifier.height(Space.xs))

                HeritageInfoCard(
                    title = "Geographic Location",
                    description = "Casiguran is a coastal municipality situated in northern Aurora province, Luzon. It is bounded by the Sierra Madre mountain range to the west and the Pacific Ocean to the east.",
                    iconRes = Iconsax.Location,
                    accentColor = Violet
                )

                HeritageInfoCard(
                    title = "Language Family & Contact",
                    description = "Kasiguranin belongs to the Northern Luzon sub-branch of Malayo-Polynesian languages. It exhibits extensive historical contact with Casiguran Dumagat Agta, resulting in unique lexical and phonetic borrowings.",
                    iconRes = Iconsax.People,
                    accentColor = Gold
                )

                HeritageInfoCard(
                    title = "Linguistic Grammar Features",
                    description = "Kasiguranin features a rich aspectual verb system (neutral, imperfective, perfective, contemplative), unique glottal stops ʔ, long vowels ː, and predicate-initial word order.",
                    iconRes = Iconsax.Teacher,
                    accentColor = Coral
                )

                HeritageInfoCard(
                    title = "Preservation Efforts",
                    description = "Documented by UP Diliman linguistics research ('A Grammatical Sketch of Kasiguranin', 2016). KasiGuru serves to keep the language alive for future generations.",
                    iconRes = Iconsax.Courthouse,
                    accentColor = Green
                )

                Spacer(Modifier.height(Space.navBarClearance))
            }
        }
    )
}

@Composable
private fun HeritageInfoCard(
    title: String,
    description: String,
    iconRes: Int,
    accentColor: Color
) {
    // Gold and Coral measure below even the 3:1 non-text floor as a foreground on their own light
    // tint (1.65 and 2.03, measured) — DESIGN.md's "fills carry ink, never foregrounds" rule, so
    // those two get a solid fill with an ink icon instead of the tinted-chip treatment that works
    // for Violet (6.00) and Green (4.25).
    val isRewardFill = accentColor == Gold || accentColor == Coral
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(Shapes.chip)
                    .background(if (isRewardFill) accentColor else accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = if (isRewardFill) RewardInk else accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
        }

        Spacer(Modifier.height(Space.sm))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            lineHeight = 22.sp
        )
    }
}
