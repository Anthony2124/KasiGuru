package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.RewardInk

/**
 * The "hear this word's pronunciation" control: a solid Gold circle. Before this existed, the same
 * button was hand-rolled with slightly different sizes, backgrounds (`XpGold` vs `Gold`) and icon
 * tints (`CoastInk` vs `RewardInk`) at every call site — [WordDetailBottomSheet], the flashcard deck,
 * the dictionary's word cards, and [WordVerificationDialog] among them. Not used for the Lesson
 * Player's `ListenAndChoose` exercise (a `ClayFab`, since audio there *is* the whole prompt) or Audio
 * Quiz's pulsing replay button (a distinct gameplay mechanic, not a decoration) — both of those are
 * deliberately larger and already share their own components.
 */
@Composable
fun AudioPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescription: String = "Play pronunciation"
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Gold)
    ) {
        Icon(
            painter = painterResource(id = Iconsax.VolumeHigh),
            contentDescription = contentDescription,
            tint = RewardInk,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}
