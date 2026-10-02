package com.kasiguru.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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

/** Locked artwork is shown in grey, so the learner sees what they are working towards. */
private val lockedArt = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * A badge tier. With [familyId], it is that family's artwork for [tier]: in full colour once earned;
 * greyed and dimmed under a small lock while still locked. The tier is always named to TalkBack and
 * by the caller's label, so neither the frame nor the colour is the only clue.
 *
 * Without artwork (archived badges from the old catalogue), it falls back to the medal glyph: the
 * tier's colour with a lit top and a ring a shade darker, or a quiet disc with a lock.
 */
@Composable
fun StandardBadgeMedal(
    tier: BadgeTier?,
    earned: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    familyId: String? = null
) {
    val art = badgeArt(familyId, tier ?: BadgeTier.BEGINNER)
    if (art != null) {
        val label = tier?.label.orEmpty()
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(art),
                contentDescription = if (earned) "$label badge" else "Locked $label badge",
                colorFilter = if (earned) null else lockedArt,
                alpha = if (earned) 1f else 0.45f,
                modifier = Modifier.fillMaxSize()
            )
            if (!earned) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(size * 0.34f)
                        .background(Surface, CircleShape)
                        .border(1.dp, BorderHairline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(Iconsax.Lock),
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(size * 0.19f)
                    )
                }
            }
        }
        return
    }
    MedalGlyph(tier, earned, modifier, size)
}

@Composable
private fun MedalGlyph(tier: BadgeTier?, earned: Boolean, modifier: Modifier, size: Dp) {
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
