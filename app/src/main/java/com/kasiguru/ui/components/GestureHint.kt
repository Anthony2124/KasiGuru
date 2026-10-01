package com.kasiguru.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.*

/** A drawn hand and arrows, paired with words so the gesture is never the only instruction. */
@Composable
fun GestureHint(swipe: Boolean, modifier: Modifier = Modifier) {
    val ink = BrandLime
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(56.dp)) {
            val s = size.width / 56f
            val hand = Path().apply {
                moveTo(21*s, 43*s); lineTo(12*s, 32*s); quadraticBezierTo(9*s, 26*s, 15*s, 28*s)
                lineTo(21*s, 34*s); lineTo(21*s, 15*s); cubicTo(21*s, 9*s, 28*s, 9*s, 28*s, 15*s)
                lineTo(28*s, 27*s); cubicTo(44*s, 24*s, 44*s, 39*s, 37*s, 45*s)
            }
            drawPath(hand, ink, style = Stroke(2*s, cap = StrokeCap.Round))
            if (swipe) {
                drawLine(ink, Offset(6*s, 7*s), Offset(49*s, 7*s), 2*s, StrokeCap.Round)
                drawLine(ink, Offset(6*s, 7*s), Offset(11*s, 3*s), 2*s)
                drawLine(ink, Offset(49*s, 7*s), Offset(44*s, 3*s), 2*s)
            } else drawCircle(ink, 8*s, Offset(25*s, 12*s), style = Stroke(1.5f*s))
        }
        Text(if (swipe) "Swipe left for Again, right for Good" else "Tap to open, then tap to flip",
            style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.weight(1f))
    }
}
