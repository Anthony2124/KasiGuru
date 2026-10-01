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

/** Uses the app's existing medal icon; tier and family are labelled by the caller. */
@Composable
fun BadgeTierIcon(modifier: Modifier = Modifier, locked: Boolean = false) {
    Box(modifier.alpha(if(locked) 0.35f else 1f),contentAlignment = Alignment.Center) {
        Icon(painterResource(Iconsax.MedalStar),null,tint = BrandLime,
            modifier = Modifier.fillMaxSize(0.55f))
    }
}
