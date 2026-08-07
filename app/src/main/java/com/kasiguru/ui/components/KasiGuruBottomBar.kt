package com.kasiguru.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.navigation.Screen
import com.kasiguru.ui.theme.*

data class BottomNavItem(
    val route: String,
    val title: String,
    @DrawableRes val iconOutline: Int,
    @DrawableRes val iconBold: Int
)

@Composable
fun KasiGuruBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        BottomNavItem(Screen.Home.route, "Home", Iconsax.HomeOutline, Iconsax.HomeBold),
        BottomNavItem(Screen.VocabularyList.route, "Learn", Iconsax.BookOutline, Iconsax.BookBold),
        BottomNavItem(Screen.FlashcardDeck.route, "Review", Iconsax.RepeatOutline, Iconsax.RepeatBold),
        BottomNavItem(Screen.GameHub.route, "Practice", Iconsax.Element4Outline, Iconsax.Element4Bold),
        BottomNavItem(Screen.Profile.route, "Profile", Iconsax.ProfileOutline, Iconsax.ProfileBold)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp, top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(292.dp)
                .height(56.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.45f),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                ),
            shape = CircleShape,
            color = PlayNavDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isSelected = currentRoute == item.route

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) TextDark else Color.White.copy(alpha = 0.85f),
                        animationSpec = tween(durationMillis = 220),
                        label = "IconColor"
                    )

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) PlayGoldStart else Color.Transparent,
                        animationSpec = tween(durationMillis = 220),
                        label = "BgColor"
                    )

                    val itemScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 0.88f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "ItemScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(itemScale)
                            .clip(CircleShape)
                            .background(backgroundColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigateToRoute(item.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isSelected) item.iconBold else item.iconOutline
                            ),
                            contentDescription = item.title,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
