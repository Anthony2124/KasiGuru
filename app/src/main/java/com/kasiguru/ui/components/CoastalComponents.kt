package com.kasiguru.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                letterSpacing = (-0.3).sp
            )
            // Arrow circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                ArrowRightIcon(tint = Color.White, size = 16.dp)
            }
        }
    }
}

enum class PillVariant { Purple, Gold }

// ─────────────────────────────────────────────────────────────────────────────
// ProgressRingHeroCard — purple hero card with XP progress ring
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProgressRingHeroCard(
    greeting: String,
    userName: String,
    subtitle: String,
    progress: Float,           // 0f..1f
    ctaLabel: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "RingProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF6C5CE7), PlayPurpleStart, Color(0xFF9B8CFA))
                )
            )
            .padding(20.dp)
    ) {
        // Wave decoration
        Canvas(modifier = Modifier.matchParentSize()) {
            drawWaves(size)
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = greeting,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = userName,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.4).sp,
                        lineHeight = 24.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(Modifier.width(14.dp))

                // Progress ring
                Box(
                    modifier = Modifier.size(88.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val strokeWidth = 8.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        // Track
                        drawCircle(
                            color = Color.White.copy(alpha = 0.25f),
                            radius = radius,
                            center = center,
                            style = Stroke(strokeWidth)
                        )
                        // Fill
                        drawArc(
                            color = Color(0xFFFFD15C),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "TODAY",
                            color = Color.White.copy(alpha = 0.82f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            CoastPillButton(
                label = ctaLabel,
                onClick = onCtaClick,
                variant = PillVariant.Gold
            )
        }
    }
}

private fun DrawScope.drawWaves(canvasSize: Size) {
    val path1 = Path().apply {
        moveTo(0f, canvasSize.height * 0.72f)
        quadraticBezierTo(
            canvasSize.width * 0.25f, canvasSize.height * 0.60f,
            canvasSize.width * 0.5f, canvasSize.height * 0.72f
        )
        quadraticBezierTo(
            canvasSize.width * 0.75f, canvasSize.height * 0.84f,
            canvasSize.width, canvasSize.height * 0.72f
        )
        lineTo(canvasSize.width, canvasSize.height)
        lineTo(0f, canvasSize.height)
        close()
    }
    drawPath(path1, Color.White.copy(alpha = 0.10f))

    val path2 = Path().apply {
        moveTo(0f, canvasSize.height * 0.84f)
        quadraticBezierTo(
            canvasSize.width * 0.3f, canvasSize.height * 0.74f,
            canvasSize.width * 0.5f, canvasSize.height * 0.84f
        )
        quadraticBezierTo(
            canvasSize.width * 0.75f, canvasSize.height * 0.94f,
            canvasSize.width, canvasSize.height * 0.84f
        )
        lineTo(canvasSize.width, canvasSize.height)
        lineTo(0f, canvasSize.height)
        close()
    }
    drawPath(path2, Color.White.copy(alpha = 0.12f))
}

// ─────────────────────────────────────────────────────────────────────────────
// CoastStatTile — single stat tile for the 2×2 grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CoastStatTile(
    label: String,
    value: String,
    domain: StatDomain,
    modifier: Modifier = Modifier
) {
    val (iconBg, iconTint) = when (domain) {
        StatDomain.Xp      -> Color(0xFFFFB020).copy(alpha = 0.15f) to XpGold
        StatDomain.Streak  -> StreakEmber.copy(alpha = 0.14f)       to StreakEmber
        StatDomain.Words   -> VocabSea.copy(alpha = 0.14f)          to VocabSea
        StatDomain.Level   -> PlayPurpleStart.copy(alpha = 0.14f)   to PlayPurpleStart
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                StatDomainIcon(domain = domain, tint = iconTint, size = 20.dp)
            }
            Column {
                Text(
                    text = label,
                    color = CoastMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                )
                Text(
                    text = value,
                    color = CoastInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    letterSpacing = (-0.2).sp
                )
            }
        }
    }
}

enum class StatDomain { Xp, Streak, Words, Level }

// ─────────────────────────────────────────────────────────────────────────────
// CoastSectionCard — colored section card (vocab / games / stories / review)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CoastSectionCard(
    title: String,
    subtitle: String,
    domain: SectionDomain,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = when (domain) {
        SectionDomain.Vocab   -> listOf(VocabSea, VocabSeaDark)
        SectionDomain.Games   -> listOf(GamesCoralLight, GamesCoral)
        SectionDomain.Stories -> listOf(StoriesDusk, PlayPurpleStart)
        SectionDomain.Review  -> listOf(SkyReview, Color(0xFF2C7BE5))
    }

    Box(
        modifier = modifier
            .height(128.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(gradient))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                SectionDomainIcon(domain = domain, tint = Color.White, size = 20.dp)
            }
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.88f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

enum class SectionDomain { Vocab, Games, Stories, Review }

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
// WordOfTheDayStrip — reusable word-of-day card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WordOfTheDayStrip(
    kasiguranin: String,
    translation: String,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(XpGold, XpGoldDark))),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KASIGURANIN",
                    color = XpGold,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = kasiguranin,
                    color = CoastInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = translation,
                    color = CoastMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PlayPurpleStart.copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = onPlayClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                PlayIcon(tint = PlayPurpleStart, size = 18.dp)
            }
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

@Composable
private fun PlayIcon(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val path = Path().apply {
            moveTo(this@Canvas.size.width * 0.30f, this@Canvas.size.height * 0.20f)
            lineTo(this@Canvas.size.width * 0.80f, this@Canvas.size.height * 0.50f)
            lineTo(this@Canvas.size.width * 0.30f, this@Canvas.size.height * 0.80f)
            close()
        }
        drawPath(path, tint)
    }
}

@Composable
private fun StatDomainIcon(domain: StatDomain, tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size
        val sw = s.width * 0.12f
        when (domain) {
            StatDomain.Xp, StatDomain.Level -> {
                val path = Path()
                val cx = s.width / 2f; val cy = s.height / 2f
                val r = s.width * 0.42f; val ri = r * 0.41f
                for (i in 0 until 10) {
                    val angle = Math.toRadians((i * 36.0) - 90.0)
                    val radius = if (i % 2 == 0) r else ri
                    val x = cx + (radius * Math.cos(angle)).toFloat()
                    val y = cy + (radius * Math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, tint)
            }
            StatDomain.Streak -> {
                val path = Path().apply {
                    moveTo(s.width * 0.50f, s.height * 0.08f)
                    cubicTo(s.width * 0.85f, s.height * 0.40f, s.width * 0.72f, s.height * 0.68f, s.width * 0.58f, s.height * 0.62f)
                    cubicTo(s.width * 0.44f, s.height * 0.50f, s.width * 0.40f, s.height * 0.80f, s.width * 0.30f, s.height * 0.72f)
                    cubicTo(s.width * 0.12f, s.height * 0.55f, s.width * 0.20f, s.height * 0.32f, s.width * 0.50f, s.height * 0.08f)
                    close()
                }
                drawPath(path, tint)
            }
            StatDomain.Words -> {
                drawLine(tint, Offset(s.width * 0.18f, s.height * 0.30f), Offset(s.width * 0.82f, s.height * 0.30f), sw, StrokeCap.Round)
                drawLine(tint, Offset(s.width * 0.18f, s.height * 0.50f), Offset(s.width * 0.65f, s.height * 0.50f), sw, StrokeCap.Round)
                drawLine(tint, Offset(s.width * 0.18f, s.height * 0.70f), Offset(s.width * 0.50f, s.height * 0.70f), sw, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun SectionDomainIcon(domain: SectionDomain, tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size
        val sw = s.width * 0.12f
        when (domain) {
            SectionDomain.Vocab -> {
                drawLine(tint, Offset(s.width * 0.15f, s.height * 0.25f), Offset(s.width * 0.85f, s.height * 0.25f), sw, StrokeCap.Round)
                drawLine(tint, Offset(s.width * 0.15f, s.height * 0.45f), Offset(s.width * 0.85f, s.height * 0.45f), sw, StrokeCap.Round)
                drawLine(tint, Offset(s.width * 0.15f, s.height * 0.65f), Offset(s.width * 0.65f, s.height * 0.65f), sw, StrokeCap.Round)
            }
            SectionDomain.Games -> {
                drawLine(tint, Offset(s.width * 0.28f, s.height * 0.50f), Offset(s.width * 0.72f, s.height * 0.50f), sw, StrokeCap.Round)
                drawLine(tint, Offset(s.width * 0.50f, s.height * 0.28f), Offset(s.width * 0.50f, s.height * 0.72f), sw, StrokeCap.Round)
            }
            SectionDomain.Stories -> {
                drawLine(tint, Offset(s.width * 0.28f, s.height * 0.30f), Offset(s.width * 0.72f, s.height * 0.30f), sw, StrokeCap.Round)
                drawLine(tint, Offset(s.width * 0.28f, s.height * 0.50f), Offset(s.width * 0.72f, s.height * 0.50f), sw, StrokeCap.Round)
                drawLine(tint, Offset(s.width * 0.28f, s.height * 0.70f), Offset(s.width * 0.55f, s.height * 0.70f), sw, StrokeCap.Round)
            }
            SectionDomain.Review -> {
                val path = Path().apply {
                    moveTo(s.width * 0.75f, s.height * 0.30f)
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(s.width * 0.18f, s.height * 0.18f, s.width * 0.82f, s.height * 0.82f),
                        startAngleDegrees = -60f,
                        sweepAngleDegrees = 280f,
                        forceMoveTo = false
                    )
                }
                drawPath(path, tint, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
    }
}
