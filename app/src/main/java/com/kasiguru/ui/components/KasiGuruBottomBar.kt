package com.kasiguru.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kasiguru.R
import com.kasiguru.ui.navigation.Screen
import com.kasiguru.ui.theme.*

data class BottomNavItem(
    val route: String,
    val title: String,
    val iconRes: Int
)

@Composable
fun KasiGuruBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        BottomNavItem(Screen.Home.route, "Home", R.drawable.ic_home_outline),
        BottomNavItem(Screen.VocabularyList.route, "Learn", R.drawable.ic_book_outline),
        BottomNavItem(Screen.FlashcardDeck.route, "Review", R.drawable.ic_repeat_outline),
        BottomNavItem(Screen.GameHub.route, "Practice", R.drawable.ic_grid_outline),
        BottomNavItem(Screen.Profile.route, "Profile", R.drawable.ic_profile_outline)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(20.dp, shape = RoundedCornerShape(34.dp)),
            shape = RoundedCornerShape(34.dp),
            color = FloatingNavBackground
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isSelected = currentRoute == item.route

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) FloatingNavBackground else TextSubtleGray,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "IconColor"
                    )

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) FloatingNavActive else androidx.compose.ui.graphics.Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "BgColor"
                    )

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(backgroundColor)
                            .clickable { onNavigateToRoute(item.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = item.iconRes),
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
