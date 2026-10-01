package com.kasiguru.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import com.kasiguru.ui.theme.BrandLime
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Faint

/** Uses the app's existing medal icon; tier and family are labelled by the caller. */
@Composable
fun BadgeTierIcon(modifier: Modifier = Modifier, locked: Boolean = false) {
    Box(modifier,contentAlignment = Alignment.Center) {
        Icon(painterResource(if (locked) Iconsax.Lock else Iconsax.MedalStar),
            if (locked) "Locked badge" else "Earned badge", tint = if (locked) Faint else BrandLime,
            modifier = Modifier.fillMaxSize(0.55f))
    }
}
