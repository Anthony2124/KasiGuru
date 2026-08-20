package com.kasiguru.ui.components.clay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.CanopyBottom
import com.kasiguru.ui.theme.CanopyTop
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.VioletShadow

/** How far the sheet rides up into the canopy. One value, so every screen overlaps identically. */
val SheetOverlap: Dp = 26.dp

/**
 * The structural spine of the app: a violet **canopy** with a white **sheet** overlapping upward
 * into it.
 *
 * The canopy carries who you are and where you stand today. The sheet carries the work. Having one
 * scaffold own that split is what stops screens from decaying back into a vertical stack of cards,
 * which is the failure this rebuild exists to fix.
 *
 * The canopy is fixed and the sheet scrolls beneath it, so streak and daily-goal state stay visible
 * while the learner moves through content.
 *
 * @param canopyHeight height of the canopy *below* the status bar. Sized by role: tall on Learn and
 *   Progress, short on Dictionary. The status-bar inset is added on top automatically, so this value
 *   means the same thing on every device.
 *
 * Note that text inside [canopyContent] must be pure white ([com.kasiguru.ui.theme.OnCanopy]).
 * Faded white fails AA at every alpha against the canopy gradient — see DESIGN.md.
 */
@Composable
fun CanopyScaffold(
    canopyHeight: Dp,
    modifier: Modifier = Modifier,
    canopyContent: @Composable ColumnScope.() -> Unit,
    sheetContent: @Composable BoxScope.() -> Unit
) {
    // The canopy is violet in both themes, so its status-bar icons are always light. This used to be
    // forced app-wide from Theme.kt on the assumption every screen had a canopy behind the status bar.
    com.kasiguru.ui.theme.StatusBarIcons(dark = false)

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val totalCanopyHeight = statusBarHeight + canopyHeight

    Box(modifier = modifier.fillMaxSize().background(Ground)) {
        // ── Canopy ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalCanopyHeight)
                .background(Brush.verticalGradient(listOf(CanopyTop, CanopyBottom)))
                .padding(top = statusBarHeight)
                .padding(horizontal = Space.gutter),
            content = canopyContent
        )

        // ── Sheet ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = totalCanopyHeight - SheetOverlap)
                .clip(Shapes.sheetTop)
                .background(Surface),
            content = sheetContent
        )
    }
}

/** A small circular canopy action — settings, edit, notifications — sized and tinted to match [CanopyBackButton]. */
@Composable
fun CanopyIconButton(
    @androidx.annotation.DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(Shapes.pill)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            painter = androidx.compose.ui.res.painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * The default content surface: white, one diffuse violet-tinted shadow, no lip.
 *
 * Deliberately *not* clay. Clay is reserved for things you earn or press — podium blocks, badges, the
 * FAB, the primary button. Building ordinary content out of clay is what tips a playful interface into
 * a toy, so the distinction is enforced by having two separate primitives rather than one with a flag.
 *
 * @param artwork optional illustration slot drawn behind [content]. Adrian supplies this artwork; the
 *   card must look finished with it absent.
 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = Shapes.panel,
    color: Color = Surface,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Space.md),
    artwork: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.976f else 1f,
        animationSpec = tween(if (isPressed) 120 else 180),
        label = "SoftCardPress"
    )

    Box(
        modifier = modifier
            .scale(pressScale)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = VioletShadow,
                spotColor = VioletShadow
            )
            .clip(shape)
            .background(color)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick
                ) else Modifier
            )
    ) {
        artwork?.invoke(this)
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
