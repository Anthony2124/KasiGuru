package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.domain.gamification.BadgeTier
import com.kasiguru.ui.theme.*

/** The fill each of the six tiers wears, from silver to orange. Ink text and glyphs sit on all of them. */
fun badgeTierColor(tier: BadgeTier?): Color = when (tier) {
    BadgeTier.BEGINNER -> TierSilver
    BadgeTier.LEARNER -> Color(0xFF71DAB8)
    BadgeTier.ACHIEVER -> Gold
    BadgeTier.EXPERT -> Color(0xFF7FBFFF)
    BadgeTier.MASTER -> Color(0xFFBF98F4)
    BadgeTier.LEGEND -> Color(0xFFFFAD75)
    null -> TierSilver
}

/**
 * Existing medal/lock glyphs with six labelled tiers; no customized icon artwork. An earned medal is
 * the tier's colour with a lit top and a ring a shade darker; a locked one is a quiet disc with a lock.
 */
@Composable
fun StandardBadgeMedal(tier: BadgeTier?, earned: Boolean, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val face = if (!earned) SurfaceSunken else badgeTierColor(tier)
    Box(
        modifier
            .size(size)
            .background(face, CircleShape)
            .then(
                if (earned) Modifier.background(
                    Brush.verticalGradient(0f to Color.White.copy(alpha = .28f), 0.5f to Color.Transparent),
                    CircleShape
                ) else Modifier
            )
            .border((size.value / 24f).coerceAtLeast(2f).dp, if (earned) Color.Black.copy(alpha = .18f) else BorderHairline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(if (earned) Iconsax.MedalStarBold else Iconsax.Lock),
            if (earned) "${tier?.label.orEmpty()} badge" else "Locked badge",
            tint = if (earned) RewardInk else Faint,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}
