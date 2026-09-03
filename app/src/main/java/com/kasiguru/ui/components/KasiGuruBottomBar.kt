package com.kasiguru.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.navigation.Screen
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.Touch
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletShadow
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
 * Primary navigation: one floating pill, inset from both edges, carrying five evenly spaced
 * destinations and nothing else.
 *
 * There is deliberately no docked FAB. The "continue learning" action used to overlap this pill's top
 * edge, which cost it roughly its top 12dp of touch area — a child that overflows its parent stops
 * receiving touches in Compose, and the offset here was larger than the padding it had to escape. That
 * action now lives as a full-width primary button at the top of Learn, where it is both legible and
 * wholly tappable, and where its label can say which of "continue" or "review" it currently means.
 *
 * The pill is a single [Row] rather than a [androidx.compose.foundation.layout.Box] wrapping one: with
 * nothing left to overlay, the wrapper only reintroduced the overflow that caused the bug.
 */
@Composable
fun KasiGuruBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(Screen.Learn.route, "Learn", Iconsax.HomeOutline, Iconsax.HomeBold, TourAnchor.NavLearn),
        BottomNavItem(Screen.GameHub.route, "Practice", Iconsax.Element4Outline, Iconsax.Element4Bold, TourAnchor.NavPractice),
        BottomNavItem(Screen.VocabularyList.route, "Words", Iconsax.BookOutline, Iconsax.BookBold, TourAnchor.NavWords),
        BottomNavItem(Screen.Achievements.route, "Progress", Iconsax.MedalStar, Iconsax.MedalStarBold, TourAnchor.NavProgress),
        BottomNavItem(Screen.Profile.route, "Profile", Iconsax.ProfileOutline, Iconsax.ProfileBold, TourAnchor.NavProfile)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Space.lg, vertical = Space.sm)
            .height(64.dp)
            .shadow(elevation = 16.dp, shape = Shapes.pill, ambientColor = VioletShadow, spotColor = VioletShadow)
            .clip(Shapes.pill)
            .background(Surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach {
            NavIconItem(
                item = it,
                isSelected = currentRoute == it.route,
                modifier = Modifier.tourAnchor(it.tourAnchor)
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
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = tween(200),
        label = "NavIconScale"
    )
    val tint by animateColorAsState(
        targetValue = if (isSelected) Violet else Muted,
        animationSpec = tween(200),
        label = "NavIconTint"
    )

    Column(
        modifier = modifier
            .defaultMinSize(minWidth = Touch.minTarget, minHeight = Touch.minTarget)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = if (isSelected) item.iconBold else item.iconOutline),
            contentDescription = null, // the visible label already names the destination
            tint = tint,
            modifier = Modifier.size(22.dp).scale(iconScale)
        )
        Spacer(Modifier.height(2.dp))
        // Five cells across a 360dp screen leave roughly 66dp each; "Practice" and "Progress" at a
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
