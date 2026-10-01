package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.*

@Composable
fun SegmentedProgress(completed: Int, total: Int, modifier: Modifier = Modifier) {
    // Group large exercise queues so segment width stays legible on compact phones.
    val count = total.coerceIn(1, 24)
    val fraction = if (total <= 0) 0f else completed.toFloat() / total
    Row(modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f) }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(count) { i ->
            Box(Modifier.weight(1f).height(6.dp).background(
                if ((i + 1f) / count <= fraction) Lime else TrackNeutral, RoundedCornerShape(3.dp)))
        }
    }
}
