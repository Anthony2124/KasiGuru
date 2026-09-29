package com.kasiguru.ui.components.brand

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.R
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.GlowCore
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.KasiGuruDisplay
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.SurfaceSunken

/**
 * The learner's own avatar: one of Adrian's nine Jepjep avatars.
 *
 * [id] is stored permanently as `UserProgressEntity.profileIconId` and on the leaderboard as
 * `avatarIconId`, and [name] as `ProfileEntity.residentName`. Both are load-bearing: append new
 * avatars at the end with the next id, never reorder, rename or remove one.
 *
 * Ids 1–6 were once six placeholder "Casiguran residents" (all the same glyph). [fromId] and
 * [fromStored] map those old values onto avatars, so nobody who onboarded before the art arrived
 * loses their choice or crashes on an unknown name.
 */
enum class JepjepAvatar(val id: Int, @DrawableRes val res: Int, val displayName: String) {
    ADVENTURER(1, R.drawable.avatar_adventurer, "Adventurer"),
    SCHOLAR(2, R.drawable.avatar_scholar, "Scholar"),
    KING(3, R.drawable.avatar_king, "King"),
    BEACH(4, R.drawable.avatar_beach, "Beach"),
    COOL(5, R.drawable.avatar_cool, "Cool"),
    FARMER(6, R.drawable.avatar_farmer, "Farmer"),
    SUNGLASS(7, R.drawable.avatar_sunglass, "Sunglasses"),
    SLEEPY(8, R.drawable.avatar_sleepy, "Sleepy"),
    /**
     * Adrian's "IPs" avatar, in Indigenous attire. Its display name and the attire should be checked
     * with the Agta/Dumagat community of Casiguran before release; "Heritage" is a neutral stand-in.
     */
    HERITAGE(9, R.drawable.avatar_ips, "Heritage");

    companion object {
        val Default = ADVENTURER

        fun fromId(id: Int?): JepjepAvatar = entries.firstOrNull { it.id == id } ?: Default

        /** Accepts an avatar name, or one of the six legacy resident names profiles were saved with. */
        fun fromStored(value: String?): JepjepAvatar {
            if (value.isNullOrBlank()) return Default
            entries.firstOrNull { it.name == value }?.let { return it }
            return when (value) {
                "STUDENT" -> ADVENTURER
                "TEACHER" -> SCHOLAR
                "ELDER" -> KING
                "SURFER" -> BEACH
                "MUSICIAN" -> COOL
                "FARMER" -> FARMER
                "FISHERMAN" -> SUNGLASS
                "MOTHER" -> SLEEPY
                "AGTA_WOMAN" -> HERITAGE
                else -> Default
            }
        }
    }
}

/**
 * The avatar in a circle, on a soft glow so the cut-out bust reads as a portrait.
 *
 * @param level shows a small gold level chip on the lower right when not null.
 * @param selected draws the 2 dp lime selection ring used by pickers.
 */
@Composable
fun JepjepAvatarPortrait(
    avatar: JepjepAvatar,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    level: Int? = null,
    selected: Boolean = false,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(GlowCore, SurfaceSunken)))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Lime else BorderHairline,
                    shape = CircleShape
                )
        ) {
            Image(
                painter = painterResource(id = avatar.res),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = size * 0.08f)
                    .clip(CircleShape)
            )
        }
        if (level != null) {
            val chip = (size * 0.36f).coerceAtLeast(18.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(chip)
                    .clip(CircleShape)
                    .background(Gold)
                    .border(2.dp, Ground, CircleShape)
            ) {
                Text(
                    text = "$level",
                    color = RewardInk,
                    fontFamily = KasiGuruDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = (chip.value * 0.5f).sp
                )
            }
        }
    }
}

/**
 * All nine avatars in a 3 × 3 grid, as a single-choice group. Used by onboarding, Edit profile and
 * Add profile, so the choice looks and reads the same everywhere.
 */
@Composable
fun JepjepAvatarPicker(
    selected: JepjepAvatar,
    onSelect: (JepjepAvatar) -> Unit,
    modifier: Modifier = Modifier,
    portraitSize: Dp = 72.dp
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        JepjepAvatar.entries.chunked(3).forEach { row ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                row.forEach { avatar ->
                    val isSelected = avatar == selected
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(avatar) }
                        )
                    ) {
                        JepjepAvatarPortrait(avatar = avatar, size = portraitSize, selected = isSelected)
                        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                        Text(
                            text = avatar.displayName,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Lime else com.kasiguru.ui.theme.Muted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
