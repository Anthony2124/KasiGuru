package com.kasiguru.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.*

/**
 * Premium glass-morphism styled card for KasiGuru UI.
 */
@Composable
fun KasiGuruCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(DarkCard, DarkSurfaceVariant),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .shadow(8.dp, shape, ambientColor = Primary.copy(alpha = 0.2f)),
            shape = shape,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(gradientColors),
                        shape = shape
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    content = content
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .shadow(8.dp, shape, ambientColor = Primary.copy(alpha = 0.2f))
                .background(
                    brush = Brush.linearGradient(gradientColors),
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}
