package com.kasiguru.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.kasiguru.ui.theme.Scenery
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Surface

/**
 * A game's play area set over one of Adrian's Casiguran scenes. The scene shows through a Ground wash
 * that is just strong enough for large Ink text (the game header) to keep 3:1 over the brightest sky;
 * small text laid over it should sit on a [backdropPill]. Light text loses contrast against a bright
 * illustration far faster than dark text does, hence the heavy wash.
 */
@Composable
fun CasiguranBackdrop(
    scene: Scenery,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val (top, bottom) = 0.70f to 0.80f
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = scene.res),
            contentDescription = null, // decorative
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
