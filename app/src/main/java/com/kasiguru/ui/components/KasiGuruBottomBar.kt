package com.kasiguru.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.navigation.Screen
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.Motion
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.ShadowTint
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.motionTween
import com.kasiguru.ui.tour.TourAnchor
import com.kasiguru.ui.tour.tourAnchor

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
 * There is deliberately no docked FAB. The "continue learning" action used to overlap this pill's top
 * edge, which cost it roughly its top 12dp of touch area - a child that overflows its parent stops
 * receiving touches in Compose, and the offset here was larger than the padding it had to escape. That
 * action now lives as the one lime button on Home, where it is both legible and wholly tappable, and
 * where its label can say which of "continue" or "review" it currently means.
 *
 * The selected tab sits on a sunken pill with a lime icon and label. The pill is what carries the
 * selection, not the lime alone, so the active tab still reads without colour vision.
 */
@Composable
fun KasiGuruBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(Screen.Home.route, "Home", Iconsax.HomeOutline, Iconsax.HomeBold, TourAnchor.NavHome),
        // The path is a course of lessons: the teacher's cap, which Help also uses for learning.
        BottomNavItem(Screen.Learn.route, "Learn", Iconsax.Teacher, Iconsax.Teacher, TourAnchor.NavLearn),
        BottomNavItem(Screen.GameHub.route, "Practice", Iconsax.Game, Iconsax.GameBold, TourAnchor.NavPractice),
        BottomNavItem(Screen.Library.route, "Library", Iconsax.BookOutline, Iconsax.BookBold, TourAnchor.NavLibrary),
        BottomNavItem(Screen.Profile.route, "Me", Iconsax.ProfileOutline, Iconsax.ProfileBold, TourAnchor.NavMe)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Space.md, vertical = Space.sm)
            .height(64.dp)
            .shadow(elevation = 16.dp, shape = Shapes.pill, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(Shapes.pill)
            .background(Surface)
            .border(1.dp, BorderHairline, Shapes.pill)
            // 64dp less 6dp either side leaves each cell 52dp tall, above the 48dp touch minimum.
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach {
            NavIconItem(
                item = it,
                isSelected = currentRoute == it.route,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .tourAnchor(it.tourAnchor)
            ) { onNavigateToRoute(it.route) }
        }
    }
}

@Composable
private fun NavIconItem(
    item: BottomNavItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) Lime else Muted,
        animationSpec = motionTween(Motion.Quick),
        label = "NavTint"
    )
    val cell by animateColorAsState(
        targetValue = if (isSelected) SurfaceSunken else Color.Transparent,
        animationSpec = motionTween(Motion.Quick),
        label = "NavCell"
    )

    Column(
        modifier = modifier
            .clip(Shapes.pill)
            .background(cell)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { selected = isSelected }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = if (isSelected) item.iconBold else item.iconOutline),
            contentDescription = null, // the visible label already names the destination
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        // Five cells across a 360dp screen leave roughly 62dp each; "Practice" and "Library" at a
        // 1.3 font scale will otherwise wrap and push the pill's contents off centre.
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            softWrap = false
        )
    }
}
