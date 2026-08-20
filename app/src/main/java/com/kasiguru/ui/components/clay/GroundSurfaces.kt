package com.kasiguru.ui.components.clay

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Coral
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.LocalDarkMode
import com.kasiguru.ui.theme.Motion
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.StatusBarIcons
import com.kasiguru.ui.theme.Touch
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.motionTween

/** The bar's height below the status bar. One value everywhere — the canopy's per-screen sizing is what this replaces. */
val GroundBarHeight: Dp = 56.dp

/** How far the content scrolls before the large title hands off to the bar. */
private val TitleHandoff: Dp = 40.dp

/**
 * The texture drawn behind a Ground screen, so a page of white cards on lavender does not read as flat.
 *
 * Drawn rather than shipped. Raster tiles would cost APK bytes on a build that has to stay small for a
 * data-sensitive audience, and a drawn pattern re-derives itself from the theme tokens, so it follows
 * dark mode for free.
 */
enum class GroundPattern {
    /** Soft colour fields. The default, and the only one that gives glass something to be glass over. */
    Orbs,

    /** A dot lattice. For dense, list-heavy screens where colour fields would fight the rows. */
    Grid,

    /** Concentric rings echoing the sheet radius. For screens about something earned. */
    Arcs,

    /** Nothing. For screens where any texture competes with the task, such as a game in play. */
    None
}

/**
 * The app's second shell, and the counterpart to [CanopyScaffold].
 *
 * The canopy's identity is a violet block plus a white sheet riding up into it. This is the deliberate
 * removal of both: [Ground] runs unbroken from behind the status bar to the bottom edge, and white
 * appears only as [SoftCard]s floating on it. That is why the two read as different systems rather than
 * as one being a shrunken version of the other — this does not shorten a canopy, it deletes what makes
 * one.
 *
 * The canopy earns its space on Learn, which has to say who you are and how today is going. On a
 * settings list or a word detail there is no such thing to say, and a hero block there is decoration.
 *
 * @param onBack null on the five tab roots. They are reached by switching tabs rather than by
 *   navigating forward, so a back affordance would answer a question nobody asked.
 * @param compactTitle true for screens that render no [GroundTitleBlock] — a word detail, a game in
 *   play, anything whose name needs saying once and no more. The title then sits in the bar from the
 *   start instead of waiting for a scroll that may never come, which is what an ordinary Android
 *   detail screen looks like. Screens that do render the block leave this false, so their bar stays
 *   empty until the large title has actually scrolled out of view.
 * @param patternOverlay optional tileable motif drawn over [pattern]. Adrian authors this artwork; the
 *   screen must look finished with it absent, which is the state every screen ships in today.
 */
@Composable
fun GroundScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    @DrawableRes navIcon: Int = Iconsax.ArrowLeft,
    pattern: GroundPattern = GroundPattern.Orbs,
    @DrawableRes patternOverlay: Int? = null,
    compactTitle: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    // Ground follows the theme, so the status icons must too: dark glyphs on the light lavender,
    // light glyphs on the dark ground. The canopy can hardcode light because it is violet in both.
    StatusBarIcons(dark = !LocalDarkMode.current)

    val handoffPx = with(LocalDensity.current) { TitleHandoff.toPx() }
    var scrolled by remember { mutableStateOf(false) }

    // Accumulates in a plain field and only writes state when the threshold is actually crossed.
    // Hoisting the offset into a State instead would recompose the whole screen on every scroll frame.
    val scrollWatcher = remember(handoffPx) {
        object : NestedScrollConnection {
            private var travelled = 0f
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                travelled = (travelled - consumed.y).coerceAtLeast(0f)
                val next = travelled > handoffPx
                if (next != scrolled) scrolled = next
                return Offset.Zero
            }
        }
    }

    val orbA = Violet
    val orbB = Coral
    val orbC = Gold
    val dot = Ink
    val overlay: ImageBitmap? = patternOverlay?.let { ImageBitmap.imageResource(id = it) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .drawBehind {
                drawGroundPattern(pattern, title, orbA, orbB, orbC, dot)
                overlay?.let {
                    drawRect(
                        brush = ShaderBrush(ImageShader(it, TileMode.Repeated, TileMode.Repeated)),
                        alpha = 0.06f
                    )
                }
            }
            .nestedScroll(scrollWatcher)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(GroundBarHeight)
                    // A 48dp target around a 24dp icon carries 12dp of its own padding. Starting the
                    // row at Space.xs puts the icon itself on the 20dp gutter (8 + 12), so the chevron
                    // lines up with the title below it instead of sitting a half-target inside.
                    .padding(start = if (onBack != null) Space.xs else Space.gutter, end = Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    GroundBackButton(
                        onClick = onBack,
                        iconRes = navIcon,
                        modifier = Modifier.padding(end = Space.xxs)
                    )
                }

                Crossfade(
                    targetState = scrolled || compactTitle,
                    animationSpec = motionTween(Motion.Quick),
                    label = "GroundBarTitle",
                    modifier = Modifier.weight(1f)
                ) { isScrolled ->
                    if (isScrolled) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(Modifier.fillMaxWidth())
                    }
                }

                actions?.invoke(this)
            }

            AnimatedVisibility(
                visible = scrolled,
                enter = fadeIn(motionTween(Motion.Quick)),
                exit = fadeOut(motionTween(Motion.exit(Motion.Quick)))
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(BorderHairline))
            }

            Box(Modifier.fillMaxSize(), content = content)
        }
    }
}

/**
 * The large title, placed by the caller as the first item of its own scroll container so it scrolls
 * away and hands off to the bar.
 *
 * [GroundScaffold] deliberately does not render this itself. Owning the scroll container would mean one
 * scaffold for lists and another for grids, and would break the form screens that scroll a plain
 * Column — the three containers this shell has to serve.
 *
 * Screens with nothing to add beyond a name simply omit it; their title then lives permanently in the
 * bar, which is what an ordinary Android detail screen looks like.
 */
@Composable
fun GroundTitleBlock(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    lead: (@Composable ColumnScope.() -> Unit)? = null
) {
    // Only a small bottom gap: callers place this inside a list that already applies its own
    // vertical spacing, and Space.lg on top of that reads as a hole rather than separation.
    Column(modifier = modifier.fillMaxWidth().padding(top = Space.xs, bottom = Space.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Space.xxs))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        if (lead != null) {
            Spacer(Modifier.height(Space.md))
            lead()
        }
    }
}

/**
 * Back affordance for a Ground screen: a bare chevron in a 48dp target, with a standard ripple.
 *
 * No pill behind it. [CanopyBackButton]'s white-at-18% pill exists solely so a white glyph survives the
 * violet gradient; on lavender an [Ink] chevron is legible on its own, and keeping the pill would be
 * carrying a solution to a problem this shell does not have.
 */
@Composable
fun GroundBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int = Iconsax.ArrowLeft,
    contentDescription: String = "Back"
) {
    Box(
        modifier = modifier
            .size(Touch.minTarget)
            .clip(Shapes.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = Ink,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** A bar action — settings, edit, overflow. Sized to match [GroundBackButton] so the bar reads level. */
@Composable
fun GroundIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(Touch.minTarget)
            .clip(Shapes.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = Muted,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Draws the texture behind a Ground screen.
 *
 * Deliberately no `Modifier.blur`. Blur is API 31+ where minSdk here is 26, and a radial gradient is
 * already a soft-edged disc — blurring one would buy no visible softness while putting a full-screen
 * render pass on the mid-range phones this app targets.
 *
 * Placement varies by [seed] (the screen's title) so twenty-two screens do not all carry the same
 * arrangement, which would be its own kind of flatness. Same screen, same layout, every time.
 */
private fun DrawScope.drawGroundPattern(
    pattern: GroundPattern,
    seed: String,
    orbA: Color,
    orbB: Color,
    orbC: Color,
    dot: Color
) {
    when (pattern) {
        GroundPattern.None -> Unit

        GroundPattern.Orbs -> {
            val variant = (seed.hashCode().mod(3))
            val w = size.width
            val h = size.height
            // Three arrangements, each keeping the strongest field high where the title sits and the
            // quieter ones low, so the texture never competes with the content in the middle.
            val orbs = when (variant) {
                0 -> listOf(
                    Triple(Offset(w * 0.85f, h * 0.06f), w * 0.55f, orbA),
                    Triple(Offset(w * 0.05f, h * 0.38f), w * 0.45f, orbB),
                    Triple(Offset(w * 0.75f, h * 0.86f), w * 0.50f, orbC)
                )
                1 -> listOf(
                    Triple(Offset(w * 0.10f, h * 0.04f), w * 0.60f, orbA),
                    Triple(Offset(w * 0.95f, h * 0.30f), w * 0.42f, orbC),
                    Triple(Offset(w * 0.20f, h * 0.90f), w * 0.48f, orbB)
                )
                else -> listOf(
                    Triple(Offset(w * 0.50f, h * -0.02f), w * 0.62f, orbA),
                    Triple(Offset(w * 0.02f, h * 0.60f), w * 0.44f, orbC),
                    Triple(Offset(w * 0.98f, h * 0.78f), w * 0.46f, orbB)
                )
            }
            orbs.forEachIndexed { index, (centre, radius, colour) ->
                // The violet field leads; the warm ones sit back so they read as light, not as blobs.
                val alpha = if (index == 0) 0.10f else 0.07f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colour.copy(alpha = alpha), Color.Transparent),
                        center = centre,
                        radius = radius
                    ),
                    radius = radius,
                    center = centre
                )
            }
        }

        GroundPattern.Grid -> {
            val step = 24.dp.toPx()
            val r = 1.5.dp.toPx()
            val colour = dot.copy(alpha = 0.04f)
            var y = step
            while (y < size.height) {
                var x = step
                while (x < size.width) {
                    drawCircle(color = colour, radius = r, center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }

        GroundPattern.Arcs -> {
            val centre = Offset(size.width * 0.5f, size.height * 0.12f)
            val stroke = Stroke(width = 1.dp.toPx())
            val colour = orbA.copy(alpha = 0.08f)
            // Radii step by the sheet corner radius, so the rings echo a shape the app already uses
            // rather than introducing an unrelated rhythm.
            val step = 32.dp.toPx()
            var radius = step * 2
            while (radius < size.width * 1.1f) {
                drawCircle(color = colour, radius = radius, center = centre, style = stroke)
                radius += step
            }
        }
    }
}
