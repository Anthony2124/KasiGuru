package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.R
import com.kasiguru.ui.theme.*

enum class BadgeTier(val colorStart: Color, val colorEnd: Color, val border: Color) {
    WOOD(BadgeWood, Color(0xFF7C5230), Color(0xFFD4A373)),
    BRONZE(BadgeBronze, Color(0xFFA0522D), Color(0xFFE6C280)),
    SILVER(BadgeSilver, Color(0xFF708090), Color(0xFFFFFFFF)),
    GOLD(BadgeGold, Color(0xFFFF9F1C), Color(0xFFFFF275)),
    EMERALD(BadgeEmerald, Color(0xFF10B981), Color(0xFFA7F3D0)),
    CROWN(BadgeCrownPurple, Color(0xFF6B21A8), Color(0xFFF472B6))
}

@Composable
fun GamifiedBadge3D(
    title: String,
    levelText: String = "Lvl 1",
    tier: BadgeTier = BadgeTier.GOLD,
    size: Dp = 72.dp,
    unlocked: Boolean = true,
    onClick: () -> Unit = {}
) {
    val bgGradient = if (unlocked) {
        Brush.verticalGradient(listOf(tier.colorStart, tier.colorEnd))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgGradient)
                .border(2.5.dp, if (unlocked) tier.border else Color.White, CircleShape)
        ) {
            Icon(
                painter = painterResource(id = if (tier == BadgeTier.CROWN) R.drawable.ic_crown_badge_2d else R.drawable.ic_star_shield_2d),
                contentDescription = title,
                tint = Color.Unspecified,
                modifier = Modifier.size(size * 0.65f)
            )

            // Lvl Pill Ribbon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (unlocked) NudgeFlame else Color(0xFF64748B))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = levelText,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (unlocked) StaticTextHeadingBlack else TextSubtleGray
        )
    }
}
