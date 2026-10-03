package com.kasiguru.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.navigation.Screen
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.LimeText
import com.kasiguru.ui.theme.Motion
import com.kasiguru.ui.theme.OnLime
import com.kasiguru.ui.theme.ShadowTint
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.motionTween
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor
import io.eyram.iconsax.IconSax
import com.kasiguru.util.audio.LocalSoundEffects
import com.kasiguru.util.audio.Sfx

data class BottomNavItem(
    val route: String,
    val title: String,
    @DrawableRes val iconOutline: Int,
    @DrawableRes val iconBold: Int,
    /** Lets the guided tour cut its spotlight around this exact cell. */
    val tourAnchor: TourAnchor
)

/**
 * Primary navigation: one floating pill, inset from both edges, carrying the five tabs - Home, Learn,
 * Practice, Library, Me - and nothing else.
 *
 * The selected tab grows into a filled lime pill that carries its icon and name; the other four are
 * lime icons spread evenly across the rest of the bar. The filled pill, not the colour, is what marks
 * the selection, so it still reads without colour vision. Every tab is still named to TalkBack.
 *
 * There is deliberately no docked FAB: the "continue learning" action lives on Home, where it is
 * wholly tappable and its label can say what it means.
 */
@Composable
fun KasiGuruBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Solid glyphs, not the app's two-tone Bulk set: Bulk draws half of each icon at 40%, which reads
    // as a dull smudge at this size on a dark bar.
    val items = listOf(
        BottomNavItem(Screen.Home.route, "Home", IconSax.Bold.Home, IconSax.Bold.Home, TourAnchor.NavHome),
        // The path is a course of lessons: the teacher's cap, which Help also uses for learning.
        BottomNavItem(Screen.Learn.route, "Learn", IconSax.Bold.Teacher, IconSax.Bold.Teacher, TourAnchor.NavLearn),
        BottomNavItem(Screen.GameHub.route, "Practice", IconSax.Bold.Game, IconSax.Bold.Game, TourAnchor.NavPractice),
        BottomNavItem(Screen.Library.route, "Library", IconSax.Bold.Book, IconSax.Bold.Book, TourAnchor.NavLibrary),
        BottomNavItem(Screen.Profile.route, "Me", IconSax.Bold.ProfileCircle, IconSax.Bold.ProfileCircle, TourAnchor.NavMe)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Space.md, vertical = Space.sm)
            .height(68.dp)
            .shadow(elevation = 16.dp, shape = Shapes.pill, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(Shapes.pill)
            .background(Surface)
            .border(1.dp, BorderHairline, Shapes.pill)
            // 68dp less 8dp either side leaves the pill 52dp tall, above the 48dp touch minimum.
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val sounds = LocalSoundEffects.current
        items.forEach {
            val isSelected = currentRoute == it.route
            // The open tab takes the room its name needs; the rest share what is left evenly.
            val weight by animateFloatAsState(
                targetValue = if (isSelected) 2.6f else 1f,
                animationSpec = motionTween(Motion.Standard),
                label = "NavWeight"
            )
            NavPillItem(
                item = it,
                isSelected = isSelected,
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .tourAnchor(it.tourAnchor)
            ) {
                if (!isSelected) sounds?.play(Sfx.Tap)
                onNavigateToRoute(it.route)
            }
        }
    }
}

@Composable
private fun NavPillItem(
    item: BottomNavItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) OnLime else LimeText,
        animationSpec = motionTween(Motion.Quick),
        label = "NavTint"
    )
    val pill by animateColorAsState(
        targetValue = if (isSelected) Lime else Color.Transparent,
        animationSpec = motionTween(Motion.Quick),
        label = "NavPill"
    )

    Row(
        modifier = modifier
            .clip(Shapes.pill)
            .background(pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                selected = isSelected
                contentDescription = item.title
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = if (isSelected) item.iconBold else item.iconOutline),
            contentDescription = null, // the cell itself is named
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(motionTween(Motion.Standard)) + expandHorizontally(motionTween(Motion.Standard)),
            exit = fadeOut(motionTween(Motion.Quick)) + shrinkHorizontally(motionTween(Motion.Quick))
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clearAndSetSemantics { }
            )
        }
    }
}
