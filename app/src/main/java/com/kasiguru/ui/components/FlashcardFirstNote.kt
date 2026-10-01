package com.kasiguru.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.ui.theme.BrandLime

@Composable
fun FlashcardFirstNote(modifier: Modifier = Modifier) {
    val ink = BrandLime
    Row(modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Canvas(Modifier.size(56.dp)) {
            val s = size.width / 56f
            val heart = Path().apply {
                moveTo(18*s, 30*s)
                cubicTo(1*s, 20*s, 6*s, 5*s, 18*s, 14*s)
                cubicTo(31*s, 5*s, 36*s, 20*s, 18*s, 30*s)
            }
            drawPath(heart, ink, style = Stroke(1.8f*s))
            drawLine(ink, Offset(28*s, 43*s), Offset(49*s, 13*s), 1.5f*s,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4*s, 4*s)))
            drawLine(ink, Offset(49*s, 13*s), Offset(39*s, 17*s), 1.5f*s)
            drawLine(ink, Offset(49*s, 13*s), Offset(49*s, 24*s), 1.5f*s)
        }
        Text("Tap to open, then tap to flip", color = ink,
            style = TextStyle(fontFamily = FontFamily.Cursive, fontSize = 22.sp),
            modifier = Modifier.weight(1f))
    }
}
