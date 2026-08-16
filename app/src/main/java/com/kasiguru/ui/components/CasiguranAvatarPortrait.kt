package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

/**
 * 9 "Mga Residente ng Casiguran" Resident Character Avatars (Moodboard 4.png)
 */
enum class CasiguranResident(val title: String, val role: String, val iconRes: Int) {
    ELDER("Apo Dumagat", "Cultural Guide & Storyteller", R.drawable.ic_profile_outline),
    FISHERMAN("Mang Mateo", "Coastal & Fishing Vocab", R.drawable.ic_profile_outline),
    TEACHER("Ma'am Elena", "KasiGuru Main Educator", R.drawable.ic_profile_outline),
    STUDENT("Juan", "Young Learner", R.drawable.ic_profile_outline),
    AGTA_WOMAN("Nana Maria", "Indigenous Heritage Specialist", R.drawable.ic_profile_outline),
    SURFER("Casiguran Surfer", "Mini-Games & Action Host", R.drawable.ic_profile_outline),
    FARMER("Mang Ben", "Agriculture & Nature Guide", R.drawable.ic_profile_outline),
    MOTHER("Ina Ligaya", "Family & Home Vocab", R.drawable.ic_profile_outline),
    MUSICIAN("Kiko", "Audio & Pronunciation Master", R.drawable.ic_profile_outline)
}

@Composable
fun CasiguranAvatarPortrait(
    resident: CasiguranResident = CasiguranResident.TEACHER,
    size: Dp = 54.dp,
    showLevelRing: Boolean = true,
    level: Int = 1,
    onClick: () -> Unit = {}
) {
    val ringGradient = Brush.sweepGradient(
        listOf(
            NudgeBurple,
            NudgeBink,
            NudgeFlame,
            NudgeGold,
            NudgeBurple
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clickable { onClick() }
    ) {
        // Outer Gradient Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (showLevelRing) ringGradient else Brush.linearGradient(listOf(NudgeBurple, NudgeBink)))
                .padding(3.dp)
        ) {
            // Inner Avatar Surface
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    painter = painterResource(id = resident.iconRes),
                    contentDescription = resident.title,
                    tint = NudgeBurple,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }

        // Level Pill Indicator
        if (showLevelRing) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(NudgeFlame)
                    .border(1.5.dp, Color.White, CircleShape)
            ) {
                Text(
                    text = "$level",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
