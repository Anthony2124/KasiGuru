package com.kasiguru.ui.components.clay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.Touch
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.LimeLip
import com.kasiguru.ui.theme.LimeTint
import com.kasiguru.ui.theme.OnLime
import com.kasiguru.ui.theme.Olive
import com.kasiguru.ui.theme.BorderHairline

/**
 * Primary button. Built from clay because a button is something you press — the face compresses onto
 * its lip, which is the app's press language, and the one Adrian's onboarding designs keep.
 *
 * Every bright face carries a dark label: white on lime measures 2.3:1 and white on gold is worse, so
 * [ClayButtonTone.Primary] and [ClayButtonTone.Reward] both use [OnLime]/[RewardInk]. [ClayButtonTone.Quiet]
 * is the secondary button: a dark card face with a hairline lip and a white label. See DESIGN.md.
 */
enum class ClayButtonTone { Primary, Reward, Quiet }

@Composable
fun ClayButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ClayButtonTone = ClayButtonTone.Primary,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val face: Color
    val lip: Color
    val labelColor: Color
    when (tone) {
        ClayButtonTone.Primary -> { face = Lime;    lip = LimeLip;        labelColor = OnLime }
        ClayButtonTone.Reward  -> { face = Gold;    lip = GoldDeep;       labelColor = RewardInk }
        ClayButtonTone.Quiet   -> { face = SurfaceSunken; lip = BorderHairline; labelColor = Ink }
    }

    ClaySurface(
        face = face,
        lipColor = lip,
        modifier = modifier.defaultMinSize(minHeight = Touch.minTarget),
        shape = Shapes.pill,
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = Space.lg, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = Space.xxs))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Circular clay action, used for the docked FAB and for audio playback controls.
 * [contentDescription] is required rather than optional — this control is icon-only, so without a
 * name it is invisible to a screen reader.
 */
@Composable
fun ClayFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    face: Color = Lime,
    lipColor: Color = LimeLip,
    content: @Composable BoxScope.() -> Unit
) {
    ClayCircle(
        size = size,
        face = face,
        lipColor = lipColor,
        modifier = modifier.semanticsLabel(contentDescription),
        onClick = onClick
    ) {
        Box(Modifier.align(Alignment.Center)) { content() }
    }
}

private fun Modifier.semanticsLabel(label: String): Modifier =
    this.then(
        Modifier.semantics { contentDescription = label }
    )

/**
 * Segmented toggle — Words / Stories, All-time / This week, Inbox / Settings.
 *
 * The track is a dark card and the active segment is an olive pill with a white label (7.1:1), the
 * same "selected" fill the onboarding uses for a chosen option.
 */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = Surface
) {
    Row(
        modifier = modifier
            .clip(Shapes.pill)
            .background(trackColor)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            val bg by animateColorAsState(
                targetValue = if (isSelected) Olive else Color.Transparent,
                animationSpec = tween(200),
                label = "SegmentBg"
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) com.kasiguru.ui.theme.OnCanopy else Muted,
                animationSpec = tween(200),
                label = "SegmentFg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(Shapes.pill)
                    .background(bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs)
                )
            }
        }
    }
}

/**
 * Small metadata pill.
 *
 * Its first job in KasiGuru is carrying a word's grammar: the four Kasiguranin verb aspects, plus
 * glottal stop and vowel length. That data already exists on every vocabulary entry and is currently
 * buried inside an expandable row, where nobody reads it.
 *
 * [tint] fills the chip and the label is always [Ink] or a deep tone of the same hue — never the tint
 * colour itself on white.
 */
@Composable
fun TagChip(
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = LimeTint,
    labelColor: Color = com.kasiguru.ui.theme.LimeText
) {
    Box(
        modifier = modifier
            .clip(Shapes.chip)
            .background(tint)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor
        )
    }
}

/**
 * Row of tabs with a dot marking the active one, as in the reference's Badge / Stats / Details
 * control. Lighter than a segmented toggle: use it for switching views inside one screen, and reserve
 * [SegmentedToggle] for switching the data a view is showing.
 */
@Composable
fun DotTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val dotAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(180),
                label = "DotAlpha"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = Touch.minTarget)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Ink else Muted
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(vertical = 3.dp))
                    Box(
                        modifier = Modifier
                            .alpha(dotAlpha)
                            .padding(0.dp)
                            .clip(Shapes.pill)
                            .background(Lime)
                            .defaultMinSize(minWidth = 5.dp, minHeight = 5.dp)
                    )
                }
            }
        }
    }
}
