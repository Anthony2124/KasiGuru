package com.kasiguru.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// CoastPillButton — primary CTA pill with trailing arrow circle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CoastPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PillVariant = PillVariant.Purple,
    enabled: Boolean = true
) {
    val gradientColors = when (variant) {
        PillVariant.Purple -> listOf(PlayPurpleStart, PlayPurpleEnd)
        PillVariant.Gold   -> listOf(XpGold, XpGoldDark)
    }
    // Gold is a fill that carries dark ink, never white — see DESIGN.md. Purple's own gradient is
    // deep enough to keep carrying white.
    val labelColor = if (variant == PillVariant.Gold) RewardInk else Color.White

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(start = 22.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = label,
                color = labelColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                letterSpacing = (-0.3).sp
            )
            // Arrow circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(labelColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                ArrowRightIcon(tint = labelColor, size = 16.dp)
            }
        }
    }
}

enum class PillVariant { Purple, Gold }

// ─────────────────────────────────────────────────────────────────────────────
// MascotOwlSlot — Canvas-drawn Kasiguranin owl mascot
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MascotOwlSlot(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h * 0.56f

        // Body
        drawOval(
            color = Color(0xFFFFF3D6),
            topLeft = Offset(cx - w * 0.32f, cy - h * 0.34f),
            size = Size(w * 0.64f, h * 0.70f)
        )
        // Left wing
        val lwing = Path().apply {
            moveTo(cx - w * 0.32f, cy - h * 0.1f)
            quadraticBezierTo(cx - w * 0.50f, cy + h * 0.08f, cx - w * 0.30f, cy + h * 0.24f)
            quadraticBezierTo(cx - w * 0.22f, cy + h * 0.10f, cx - w * 0.32f, cy - h * 0.1f)
            close()
        }
        drawPath(lwing, Color(0xFFE9C97A))
        // Right wing
        val rwing = Path().apply {
            moveTo(cx + w * 0.32f, cy - h * 0.1f)
            quadraticBezierTo(cx + w * 0.50f, cy + h * 0.08f, cx + w * 0.30f, cy + h * 0.24f)
            quadraticBezierTo(cx + w * 0.22f, cy + h * 0.10f, cx + w * 0.32f, cy - h * 0.1f)
            close()
        }
        drawPath(rwing, Color(0xFFE9C97A))
        // Left ear tuft
        val lear = Path().apply {
            moveTo(cx - w * 0.18f, cy - h * 0.33f)
            lineTo(cx - w * 0.25f, cy - h * 0.50f)
            lineTo(cx - w * 0.10f, cy - h * 0.35f)
            close()
        }
        drawPath(lear, Color(0xFFD4A84B))
        // Right ear tuft
        val rear = Path().apply {
            moveTo(cx + w * 0.18f, cy - h * 0.33f)
            lineTo(cx + w * 0.25f, cy - h * 0.50f)
            lineTo(cx + w * 0.10f, cy - h * 0.35f)
            close()
        }
        drawPath(rear, Color(0xFFD4A84B))
        // Eye whites
        val eyeR = w * 0.135f
        drawCircle(Color.White, eyeR, Offset(cx - w * 0.16f, cy - h * 0.05f))
        drawCircle(Color.White, eyeR, Offset(cx + w * 0.16f, cy - h * 0.05f))
        // Pupils
        val pupilR = w * 0.068f
        drawCircle(Color(0xFF1C2233), pupilR, Offset(cx - w * 0.16f, cy - h * 0.04f))
        drawCircle(Color(0xFF1C2233), pupilR, Offset(cx + w * 0.16f, cy - h * 0.04f))
        // Eye shine
        val shineR = w * 0.026f
        drawCircle(Color.White, shineR, Offset(cx - w * 0.13f, cy - h * 0.07f))
        drawCircle(Color.White, shineR, Offset(cx + w * 0.19f, cy - h * 0.07f))
        // Beak
        val beak = Path().apply {
            moveTo(cx, cy + h * 0.04f)
            lineTo(cx - w * 0.07f, cy + h * 0.13f)
            lineTo(cx + w * 0.07f, cy + h * 0.13f)
            close()
        }
        drawPath(beak, Color(0xFFFF9E1B))
        // Belly patch
        drawOval(
            color = Color(0xFFFFE4A0).copy(alpha = 0.60f),
            topLeft = Offset(cx - w * 0.18f, cy + h * 0.10f),
            size = Size(w * 0.36f, h * 0.22f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingDotStepper — animated dot row with active pill
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingDotStepper(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = PlayPurpleStart,
    inactiveColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isActive = index == current
            val width by animateDpAsState(
                targetValue = if (isActive) 28.dp else 7.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "DotWidth"
            )
            val color by animateColorAsState(
                targetValue = if (isActive) activeColor else inactiveColor,
                animationSpec = tween(200),
                label = "DotColor"
            )
            Box(
                modifier = Modifier
                    .height(7.dp)
                    .width(width)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Inline icon primitives (vector-path Canvas draws — no drawable dep)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArrowRightIcon(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size
        val strokeW = s.width * 0.12f
        drawLine(tint, Offset(s.width * 0.18f, s.height * 0.5f), Offset(s.width * 0.82f, s.height * 0.5f), strokeW, StrokeCap.Round)
        drawLine(tint, Offset(s.width * 0.55f, s.height * 0.24f), Offset(s.width * 0.82f, s.height * 0.5f), strokeW, StrokeCap.Round)
        drawLine(tint, Offset(s.width * 0.55f, s.height * 0.76f), Offset(s.width * 0.82f, s.height * 0.5f), strokeW, StrokeCap.Round)
    }
}
