package com.kasiguru.ui.components.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.R

/** Width ÷ height of the wordmark's lettering (viewBox 793.6 × 146.6). */
private const val WordmarkAspect = 793.6f / 146.6f

/** The wordmark is illegible below this width; the brand rules set it as the floor. */
val WordmarkMinWidth: Dp = 96.dp

/**
 * The KasiGuru wordmark: white "Kasi", brand-lime "Guru", from Adrian's SVG.
 *
 * Dark backgrounds only (night, surface, the glow, or a dark scrim over scenery): the white "Kasi"
 * disappears on anything light. Keep clear space of half its height on every side. Never retype it
 * in Fredoka; in running text "KasiGuru" is plain body text.
 */
@Composable
fun KasiGuruWordmark(
    modifier: Modifier = Modifier,
    width: Dp = 162.dp,
    contentDescription: String? = "KasiGuru"
) {
    Image(
        painter = painterResource(id = R.drawable.kasiguru_wordmark),
        contentDescription = contentDescription,
        modifier = modifier
            .width(width.coerceAtLeast(WordmarkMinWidth))
            .aspectRatio(WordmarkAspect)
    )
}
