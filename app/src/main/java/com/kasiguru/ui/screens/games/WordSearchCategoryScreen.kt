package com.kasiguru.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.domain.wordsearch.WordSearchTier
import com.kasiguru.ui.components.KasiGuruProgressBar
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.CategoryRegistry
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.NodeLocked
import com.kasiguru.ui.theme.NodeLockedInk
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletTint

/**
 * The question Word Search asks before it starts: which category? Every category keeps its own 30
 * levels, so each row states where that track stands, and choosing one opens its level map.
 */
@Composable
fun WordSearchCategoryScreen(
    onNavigateBack: () -> Unit,
    onCategorySelected: (levelKey: String) -> Unit,
    viewModel: WordSearchCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GroundScaffold(
        title = "Word Search",
        subtitle = "Pick a category",
        onBack = onNavigateBack,
        pattern = GroundPattern.Grid,
        content = {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Violet)
                }
                return@GroundScaffold
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter, end = Space.gutter, top = Space.lg, bottom = Space.navBarClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                item {
                    GroundTitleBlock(
                        title = "Word Search",
                        subtitle = "Pick a category. Each one has its own ${WordSearchTier.MAX_LEVEL} levels, " +
                            "from a 6×6 grid up to 10×10 with words running every direction."
                    )
                    Spacer(Modifier.height(Space.xs))
                }
                items(uiState.rows, key = { it.levelKey }) { row ->
                    CategoryRow(row = row, onClick = { onCategorySelected(row.levelKey) })
                }
            }
        }
    )
}

@Composable
private fun CategoryRow(row: WordSearchCategoryRow, onClick: () -> Unit) {
    val meta = CategoryRegistry.getMeta(row.category)
    val allCleared = row.levelsCleared >= WordSearchTier.MAX_LEVEL
    val status = when {
        !row.isPlayable -> "Not enough short words in this category yet"
        allCleared -> "All ${WordSearchTier.MAX_LEVEL} levels cleared · ${row.starsEarned} stars"
        row.levelsCleared == 0 -> "Start at level 1"
        else -> "Level ${row.levelsCleared + 1} of ${WordSearchTier.MAX_LEVEL} · ${row.starsEarned} stars"
    }

    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${row.category}. $status" },
        shape = Shapes.tile,
        elevation = if (row.isPlayable) 4.dp else 0.dp,
        color = if (row.isPlayable) com.kasiguru.ui.theme.Surface else NodeLocked,
        onClick = if (row.isPlayable) onClick else null,
        contentPadding = PaddingValues(horizontal = Space.md, vertical = Space.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(Shapes.chip)
                    .background(if (row.isPlayable) meta.startColor else VioletTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = meta.iconRes),
                    contentDescription = null,
                    tint = when {
                        !row.isPlayable -> NodeLockedInk
                        meta.onGradientIsInk -> RewardInk
                        else -> Color.White
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(text = row.category, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(text = status, style = MaterialTheme.typography.bodySmall, color = Muted)
                if (row.isPlayable) {
                    Spacer(Modifier.height(Space.xs))
                    KasiGuruProgressBar(
                        progress = row.levelsCleared / WordSearchTier.MAX_LEVEL.toFloat(),
                        height = 6.dp
                    )
                }
            }
            Spacer(Modifier.width(Space.xs))
            Icon(
                painter = painterResource(id = if (row.isPlayable) Iconsax.ArrowRight else Iconsax.Lock),
                contentDescription = null,
                tint = if (row.isPlayable) Muted else NodeLockedInk,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
