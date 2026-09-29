package com.kasiguru.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.kasiguru.ui.theme.Scenery
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Surface

/**
 * A game's play area set over one of Adrian's Casiguran scenes, shown at full strength.
 *
 * There is no wash over the scene. One used to dim it to a quarter of its strength so text could sit
 * on bare sky; now nothing does. Every piece of text over a backdrop sits on a surface of its own - a
 * card, a clay control or a [backdropPill] - because light text on a bright illustration fails
 * contrast long before dark text would.
 */
@Composable
fun CasiguranBackdrop(
    scene: Scenery,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = scene.res),
            contentDescription = null, // decorative
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        content()
    }
}

/**
 * A near-opaque Surface pill that keeps text readable over a [CasiguranBackdrop].
 * Pass [Surface]; it is a parameter only because theme colours are read in composition.
 */
fun Modifier.backdropPill(surface: Color): Modifier = this
    .clip(Shapes.pill)
    .background(surface.copy(alpha = 0.9f))
