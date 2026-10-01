package com.kasiguru.ui.screens.learn

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kasiguru.domain.lesson.Mastery
import com.kasiguru.domain.lesson.TreeNode
import com.kasiguru.domain.lesson.TreeSection
import com.kasiguru.ui.components.SegmentedProgress
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.glowBackground
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnLime
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space

/**
 * The single next action on Home: where the learner is on the path, and the button that takes them on.
 *
 * Section and lesson lead ("Continue · Pagbati at Sarili", "Lesson 3 of 5"), with the section's
 * progress beneath, as the UI cleanup plan draws it. The lesson's first Kasiguranin word stays on the
 * card as its supporting line, so Home still opens on the language rather than only on numbers.
 *
 * The card carries the soft night glow and Jepjep with his backpack; the lime button is the one
 * "do this" on Home.
 *
 * @param section the section the lesson belongs to, for "of N" and the progress bar. Null falls back
 *   to the lesson's own label.
 */
@Composable
fun ContinueCard(
    card: ContinueCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    section: TreeSection? = null
) {
    val position = section?.nodes
        ?.firstNotNullOfOrNull { (it.node as? TreeNode.Lesson)?.takeIf { l -> l.ref == card.lessonRef } }
        ?.positionInSection
    val total = section?.lessonNodeCount ?: 0
    val done = section?.nodes?.count { it.node is TreeNode.Lesson && it.mastery >= Mastery.FAMILIAR } ?: 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.panel)
            .glowBackground(centerX = 0.85f, centerY = 0.35f)
            .border(1.dp, BorderHairline, Shapes.panel)
            .padding(Space.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Continue · ${card.sectionTitle}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = if (position != null && total > 0) "Lesson $position of $total" else card.lessonLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(Space.xxs))
                // The first word the lesson teaches, in the headword's weight.
                Text(
                    text = buildAnnotatedString {
                        append("Starts with ")
                        withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.Bold)) { append(card.heroWord) }
                        append(" · ${card.heroMeaning}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (total > 0) {
                    Spacer(Modifier.height(Space.sm))
                    SegmentedProgress(done, total, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(Space.md))
                ClayButton(
                    label = "Continue",
                    onClick = onClick,
                    leading = {
                        Icon(
                            painter = painterResource(id = Iconsax.PlayBold),
                            contentDescription = null,
                            tint = OnLime,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            Spacer(Modifier.width(Space.sm))
            Jepjep(pose = JepjepPose.WithBackpack, height = 120.dp, breathe = true)
        }
    }
}
