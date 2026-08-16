package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.R
import com.kasiguru.ui.theme.*

enum class NodeState {
    COMPLETED, ACTIVE, LOCKED
}

@Composable
fun LearningPathNode(
    title: String,
    categoryName: String,
    nodeState: NodeState = NodeState.ACTIVE,
    horizontalOffset: Int = 0, // -40dp to +40dp for curved map layout
    xpReward: Int = 50,
    onClick: () -> Unit = {}
) {
    val nodeBg = when (nodeState) {
        NodeState.COMPLETED -> Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A)))
        NodeState.ACTIVE -> Brush.linearGradient(listOf(NudgeBurple, NudgeBink))
        NodeState.LOCKED -> Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFF64748B)))
    }

    val iconRes = when (nodeState) {
        NodeState.COMPLETED -> R.drawable.ic_tick_circle
        NodeState.ACTIVE -> R.drawable.ic_play_outline
        NodeState.LOCKED -> R.drawable.ic_lock_outline
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = horizontalOffset.dp)
            .padding(vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .shadow(elevation = if (nodeState == NodeState.ACTIVE) 8.dp else 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(nodeBg)
                .border(
                    width = 3.dp,
                    color = if (nodeState == NodeState.ACTIVE) NudgeGold else Color.White,
                    shape = CircleShape
                )
                .clickable(enabled = nodeState != NodeState.LOCKED) { onClick() }
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Node Title Tag
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (nodeState == NodeState.ACTIVE) NudgeBurple.copy(alpha = 0.15f) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (nodeState == NodeState.ACTIVE) FontWeight.Bold else FontWeight.Medium,
                color = if (nodeState == NodeState.ACTIVE) NudgeBurple else TextSubtleGray
            )
        }
    }
}
