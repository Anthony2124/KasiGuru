package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kasiguru.domain.gamification.BadgeTier
import com.kasiguru.ui.theme.*

/** Existing medal/lock glyphs with six labelled tiers; no customized icon artwork. */
@Composable
fun StandardBadgeMedal(tier: BadgeTier?, earned: Boolean, modifier: Modifier = Modifier) {
    val face = if (!earned) SurfaceSunken else when (tier) {
        BadgeTier.BEGINNER -> TierSilver
        BadgeTier.LEARNER -> Color(0xFF71DAB8)
        BadgeTier.ACHIEVER -> Gold
        BadgeTier.EXPERT -> Color(0xFF7FBFFF)
        BadgeTier.MASTER -> Color(0xFFBF98F4)
        BadgeTier.LEGEND -> Color(0xFFFFAD75)
        null -> TierSilver
    }
    Box(modifier.size(64.dp).background(face, CircleShape).border(2.dp, if (earned) face.copy(alpha = .6f) else BorderHairline, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(if (earned) Iconsax.MedalStar else Iconsax.Lock),
            if (earned) "${tier?.label.orEmpty()} badge" else "Locked badge", tint = if (earned) RewardInk else Faint,
            modifier = Modifier.size(36.dp))
    }
}
