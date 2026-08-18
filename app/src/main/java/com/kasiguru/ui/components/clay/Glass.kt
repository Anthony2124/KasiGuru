package com.kasiguru.ui.components.clay

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.VioletShadow

/**
 * KasiGuru's third surface family, after **Soft** (ordinary content) and **Clay** (things you earn or
 * press): a translucent panel for content riding on top of something vivid — the canopy, a clay
 * reward face, a dialog's own colour band. See DESIGN.md, "Glass, on vivid backdrops only".
 *
 * Glass is deliberately never a page's ordinary content surface. Over the plain Ground or a white
 * card a translucent white fill has nothing to differentiate itself from and reads as a duller
 * `SoftCard` — the craft floor's ban on "glass and blur as decoration rather than as a specific
 * effect" is exactly this failure mode.
 *
 * `Modifier.blur` only renders on API 31+; minSdk here is 26, and Compose has no portable
 * backdrop-filter that samples arbitrary content behind a composable. Glass's identity therefore
 * never depends on blur: translucency, a diagonal light-catching edge and a soft violet-tinted shadow
 * render identically on every device. [glow] is not a capture of whatever sits behind the panel — it
 * is a small decorative slot this composable blurs itself (a couple of soft colour discs, say), which
 * is the one thing `Modifier.blur` can do correctly without a backdrop-capture library. It is layered
 * in only where the OS supports it, as a bonus, never as the mechanism.
 *
 * Because a lightened backdrop reads text slightly less than the canopy alone, only ever place text
 * on this panel following the same rule as [com.kasiguru.ui.theme.ChipOnCanopy]: pure white, on the
 * canopy's deep end or a comparably deep fill — never on [com.kasiguru.ui.theme.CanopyTop] directly.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = Shapes.panel,
    tint: Color = Color.White,
    tintAlpha: Float = 0.16f,
    contentPadding: PaddingValues = PaddingValues(Space.md),
    glow: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = VioletShadow,
                spotColor = VioletShadow
            )
            .clip(shape)
    ) {
        if (glow != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(modifier = Modifier.blur(40.dp), content = glow)
        }
        Box(
            modifier = Modifier
                .background(tint.copy(alpha = tintAlpha))
                .background(GlassEdgeHighlight)
                .border(width = 1.dp, brush = GlassBorderBrush, shape = shape)
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * The pill-sized sibling of [GlassPanel]: a stat or status readout riding on the canopy, in place of a
 * flat [com.kasiguru.ui.theme.ChipOnCanopy] fill where the moment deserves the extra bit of ceremony —
 * a rank, a streak, an unlock state. Same placement rule: canopy's deep end only.
 */
@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    tintAlpha: Float = 0.20f,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(Shapes.pill)
            .background(tint.copy(alpha = tintAlpha))
            .background(GlassEdgeHighlight)
            .border(width = 1.dp, brush = GlassBorderBrush, shape = Shapes.pill)
            .padding(horizontal = Space.sm, vertical = Space.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xxs),
        content = content
    )
}

/** The diagonal light-catching edge: brightest top-left, fading toward the bottom-right. */
private val GlassEdgeHighlight = Brush.linearGradient(
    colors = listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.02f)),
    start = Offset.Zero,
    end = Offset(320f, 320f)
)

private val GlassBorderBrush = Brush.linearGradient(
    colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.10f))
)
