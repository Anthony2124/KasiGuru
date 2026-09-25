package com.kasiguru.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.kasiguru.ui.theme.CasiguranPhoto
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.LocalDarkMode
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface

/**
 * A game's play area set over a photo of Casiguran. The photo shows through a Ground wash that is
 * just strong enough for large Ink text (the game header) to keep 3:1 over the brightest sky; small
 * text laid over it should sit on a [BackdropPill]. Dark mode needs a heavier wash, because light
 * text loses contrast against a bright photo far faster than dark text does.
 */
@Composable
fun CasiguranBackdrop(
    photo: CasiguranPhoto,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val (top, bottom) = if (LocalDarkMode.current) 0.70f to 0.80f else 0.50f to 0.62f
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = photo.imageRes),
            contentDescription = null, // decorative; the screen's credit line names the place
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Ground.copy(alpha = top), Ground.copy(alpha = bottom))))
        )
        content()
    }
}

/**
 * A near-opaque Surface pill that keeps small or Muted text readable over a [CasiguranBackdrop].
 * Pass [Surface]; it is a parameter only because theme colours are read in composition.
 */
fun Modifier.backdropPill(surface: Color): Modifier = this
    .clip(Shapes.pill)
    .background(surface.copy(alpha = 0.9f))

/** The photo credit its CC BY-SA licence requires wherever the photo is shown. */
@Composable
fun PhotoCredit(photo: CasiguranPhoto, modifier: Modifier = Modifier) {
    Text(
        text = "Photo: ${photo.place} · ${photo.credit}",
        style = MaterialTheme.typography.labelSmall,
        color = Muted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .backdropPill(Surface)
            .padding(horizontal = Space.sm, vertical = Space.xxs)
    )
}
