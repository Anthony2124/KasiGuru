package com.kasiguru.ui.components.clay

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.CanopyBottom
import com.kasiguru.ui.theme.CanopyTop
import com.kasiguru.ui.theme.Green
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface

/**
 * One story, as a cover.
 *
 * Width comes from the caller's [modifier], so the same composable is the 160dp card on Learn's shelf
 * and the full-width card on the Stories screen. That is deliberate: the previous Stories screen
 * hand-rolled its own card with off-scale 10sp and 11sp type, and two designs for one object is how a
 * shelf and its destination stop looking like the same feature.
 *
 * Locked stories are shown rather than hidden. The lock is the motivation, and the seeded corpus is
 * built for it - two stories are free, then 50, 100 and 200 XP.
 *
 * @param cover optional artwork. Adrian authors this; the expected asset is `story_cover_<id>`, 3:2,
 *   rendered at roughly 160x107dp on the shelf and gutter-width on the Stories screen. With it absent
 *   the violet field plus the page count is a finished cover, not a placeholder - which is the state
 *   every story ships in today.
 */
@Composable
fun StoryCoverCard(
    titleKasiguranin: String,
    title: String,
    totalPages: Int,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    requiredXp: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes cover: Int? = null
) {
    val label = when {
        !isUnlocked -> "$title, locked. Earn $requiredXp XP to unlock."
        isCompleted -> "$title, $totalPages pages, completed."
        else -> "$title, $totalPages pages."
    }

    SoftCard(
        modifier = modifier.clearAndSetSemantics { contentDescription = label },
        shape = Shapes.panel,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        onClick = if (isUnlocked) onClick else null
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 2f)
                    .clip(Shapes.tile)
                    .background(
                        if (isUnlocked) {
                            Brush.linearGradient(listOf(CanopyTop, CanopyBottom))
                        } else {
                            Brush.linearGradient(listOf(Muted, Muted))
                        }
                    )
            ) {
                if (cover != null) {
                    Image(
                        painter = painterResource(id = cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!isUnlocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.Lock),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Space.xs)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.TickCircle),
                            contentDescription = null,
                            tint = Green,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(Modifier.padding(Space.sm)) {
                // The Kasiguranin title leads. On any screen that shows both, the language being
                // preserved is the loudest thing on it.
                Text(
                    text = titleKasiguranin,
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Space.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagChip(label = "$totalPages pages")
                    if (!isUnlocked) {
                        Spacer(Modifier.width(Space.xs))
                        Text(
                            text = "$requiredXp XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted
                        )
                    }
                }
            }
        }
    }
}
