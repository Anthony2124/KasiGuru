package com.kasiguru.ui.screens.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.components.clay.ClaySurface
import com.kasiguru.ui.theme.CanopyBottom
import com.kasiguru.ui.theme.CanopyTop
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.KasiguraninHeadword
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space

/**
 * The single next action on Learn, led by a real Kasiguranin word.
 *
 * The screen used to open with "Continue learning" over a stack of four identical cards, and showed
 * no Kasiguranin at all — which is the wrong hero for an app whose entire argument is that this
 * language is worth preserving. DESIGN.md already says it: the headword is the loudest thing on any
 * screen that shows one. So the card leads with the first word of the lesson it is about to start,
 * and the lesson's name, section and length sit underneath as the supporting facts.
 *
 * Clay, because it is the one thing on this screen you press, and clay is reserved for exactly that.
 * White on the violet face rather than a faded tint: DESIGN.md's first colour rule is that text on
 * the canopy hues is never faded, it is differentiated by size and weight.
 */
@Composable
fun ContinueCard(card: ContinueCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ClaySurface(
        face = CanopyTop,
        lipColor = CanopyBottom,
        shape = Shapes.panel,
        onClick = onClick,
        contentPadding = PaddingValuesOf(),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription =
                    "Continue. ${card.sectionTitle}, ${card.lessonLabel}. Starts with ${card.heroWord}, ${card.heroMeaning}"
            }
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.sectionTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.W700,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = Iconsax.PlayBold),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.height(Space.md))

            // The word is the point of the card. Everything else is scale and weight beneath it.
            Text(
                text = card.heroWord,
                style = KasiguraninHeadword,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.heroMeaning,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(Space.md))

            Text(
                text = card.lessonLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

/** Slightly roomier than the clay default, because this card carries a display-size headword. */
@Composable
private fun PaddingValuesOf() = androidx.compose.foundation.layout.PaddingValues(
    horizontal = Space.lg,
    vertical = Space.lg
)
