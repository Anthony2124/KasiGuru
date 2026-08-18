package com.kasiguru.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.components.clay.ClayFab
import com.kasiguru.ui.navigation.Screen
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.Touch
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletShadow

data class BottomNavItem(
    val route: String,
    val title: String,
    @DrawableRes val iconOutline: Int,
    @DrawableRes val iconBold: Int
)

/**
 * Primary navigation: a floating pill, inset from both edges, with the "continue learning" action
 * docked into its top edge — the shape DESIGN.md actually calls for ("a floating bottom bar" plus
 * "a docked FAB"). The previous version was a Material [androidx.compose.material3.NavigationBar]:
 * edge-to-edge, square-cornered, with the FAB stacked in its own strip above it rather than docked in,
 * which read as two separate flat bars rather than one navigation object.
 *
 * The five destinations split around the FAB's gap — [Learn, Practice] to its left, [Words, Progress,
 * Profile] to its right — rather than the FAB overlapping a sixth icon, so nothing is ever hidden
 * under it and every tap target stays clear.
 */
@Composable
fun KasiGuruBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    onContinueClick: () -> Unit,
    @DrawableRes continueIconRes: Int,
    continueDescription: String,
    modifier: Modifier = Modifier
) {
    val leftItems = listOf(
        BottomNavItem(Screen.Learn.route, "Learn", Iconsax.HomeOutline, Iconsax.HomeBold),
        BottomNavItem(Screen.GameHub.route, "Practice", Iconsax.Element4Outline, Iconsax.Element4Bold)
    )
    val rightItems = listOf(
        BottomNavItem(Screen.VocabularyList.route, "Words", Iconsax.BookOutline, Iconsax.BookBold),
        BottomNavItem(Screen.Achievements.route, "Progress", Iconsax.MedalStar, Iconsax.MedalStarBold),
        BottomNavItem(Screen.Profile.route, "Profile", Iconsax.ProfileOutline, Iconsax.ProfileBold)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Space.lg, vertical = Space.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(elevation = 16.dp, shape = Shapes.pill, ambientColor = VioletShadow, spotColor = VioletShadow)
                .clip(Shapes.pill)
                .background(Surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                leftItems.forEach { NavIconItem(it, currentRoute == it.route) { onNavigateToRoute(it.route) } }
            }
            Spacer(Modifier.width(58.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                rightItems.forEach { NavIconItem(it, currentRoute == it.route) { onNavigateToRoute(it.route) } }
            }
        }

        // Docked above the pill's centre gap, overlapping its top edge rather than sitting in its own
        // strip. A control that overflows its parent stops receiving touches in Compose, so this reads
        // as one navigation object rather than a button floating disconnected above a separate bar.
        ClayFab(
            onClick = onContinueClick,
            contentDescription = continueDescription,
            size = 56.dp,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-24).dp)
        ) {
            Icon(
                painter = painterResource(id = continueIconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp).align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun NavIconItem(item: BottomNavItem, isSelected: Boolean, onClick: () -> Unit) {
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
        modifier = Modifier
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
        Text(text = item.title, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
