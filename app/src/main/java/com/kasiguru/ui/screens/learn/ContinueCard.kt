package com.kasiguru.ui.screens.learn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.KasiguraninHeadword
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnLime
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space

/**
 * The single next action on Home, led by a real Kasiguranin word.
 *
 * The screen used to open with "Continue learning" over a stack of four identical cards, and showed
 * no Kasiguranin at all - the wrong hero for an app whose entire argument is that this language is
 * worth preserving. The headword is the loudest thing on any screen that shows one, so the card leads
 * with the first word of the lesson it is about to start, and the lesson's name, section and length
 * sit underneath as the supporting facts.
 *
 * The card itself is a plain dark surface; the lime Continue button inside it is the one "do this" on
 * Home. Making the whole card lime would put the headword on a bright fill, where it loses to the
 * button it sits beside.
 */
@Composable
fun ContinueCard(card: ContinueCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.panel,
        border = BorderHairline,
        contentPadding = PaddingValues(Space.lg)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = card.sectionTitle,
                style = MaterialTheme.typography.labelLarge,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(Modifier.height(Space.sm))

            // The word is the point of the card. Everything else is scale and weight beneath it.
            Text(
                text = card.heroWord,
                style = KasiguraninHeadword,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.heroMeaning,
                style = MaterialTheme.typography.bodyLarge,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(Space.xs))

            Text(
                text = card.lessonLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Faint
            )

            Spacer(Modifier.height(Space.md))

            ClayButton(
                label = "Continue",
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                leading = {
                    Icon(
                        painter = painterResource(id = Iconsax.PlayBold),
                        contentDescription = null,
                        tint = OnLime,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}
