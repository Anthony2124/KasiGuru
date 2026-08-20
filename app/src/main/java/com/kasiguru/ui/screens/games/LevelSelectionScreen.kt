package com.kasiguru.ui.screens.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.GameLevelEntity
import com.kasiguru.ui.components.clay.TagChip
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.components.clay.GroundPattern
import com.kasiguru.ui.components.clay.GroundScaffold
import com.kasiguru.ui.components.clay.GroundTitleBlock
import com.kasiguru.ui.components.clay.GlassChip
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.NodeLocked
import com.kasiguru.ui.theme.NodeLockedInk
import com.kasiguru.ui.theme.OnCanopy
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletTint

/**
 * Level picker for one mini-game: 30 levels across three difficulty bands. A pushed subscreen (no
 * bottom bar), so the canopy carries its own back affordance rather than relying on one.
 */
@Composable
fun LevelSelectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (String, Int) -> Unit,
    viewModel: LevelSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val ruleInfo = GameRulesRegistry.games[uiState.gameType]

    GroundScaffold(
        title = ruleInfo?.title ?: "Levels",
        subtitle = "Clear a level to unlock the next one",
        onBack = onNavigateBack,
        pattern = GroundPattern.Arcs,
        content = {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Violet)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Space.gutter, end = Space.gutter, top = Space.lg, bottom = Space.navBarClearance
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    verticalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        GroundTitleBlock(
                            title = ruleInfo?.title ?: "Levels",
                            subtitle = "Clear a level to unlock the next one",
                            // The star count was a canopy GlassChip. Gold carries RewardInk at 9.00
                            // measured, so the tag states it without needing a translucent fill.
                            lead = {
                                TagChip(
                                    label = "${uiState.totalStars} stars earned",
                                    tint = Gold,
                                    labelColor = RewardInk
                                )
                            }
                        )
                    }
                    items(uiState.levels) { level ->
                        LevelCell(
                            level = level,
                            onClick = {
                                if (level.isUnlocked) onNavigateToGame(uiState.gameType, level.levelNumber)
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun LevelCell(level: GameLevelEntity, onClick: () -> Unit) {
    // The cell's own text ("3") plus three star icons with individually-null descriptions read as
    // just a bare number to a screen reader — nothing ever announced how many stars were earned.
    val a11yLabel = if (level.isUnlocked) {
        "Level ${level.levelNumber}, ${level.starsEarned} of 3 stars"
    } else {
        "Level ${level.levelNumber}, locked"
    }
    Box(modifier = Modifier.clearAndSetSemantics { contentDescription = a11yLabel }) {
        SoftCard(
            modifier = Modifier.aspectRatio(1f).fillMaxWidth(),
            shape = Shapes.chip,
            color = if (level.isUnlocked) VioletTint else NodeLocked,
            elevation = if (level.isUnlocked) 2.dp else 0.dp,
            onClick = if (level.isUnlocked) onClick else null,
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (level.isUnlocked) {
                    Text(
                        text = "${level.levelNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Violet
                    )
                } else {
                    Icon(
                        painter = painterResource(id = Iconsax.Lock),
                        contentDescription = "Locked",
                        tint = NodeLockedInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (level.isUnlocked) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 8.dp),
                horizontalArrangement = Arrangement.spacedBy((-4).dp)
            ) {
                for (i in 1..3) {
                    val earned = i <= level.starsEarned
                    Icon(
                        painter = painterResource(id = Iconsax.StarBold),
                        contentDescription = null,
                        tint = if (earned) Gold else NodeLocked,
                        modifier = Modifier.size(12.dp).alpha(if (earned) 1f else 0.7f)
                    )
                }
            }
        }
    }
}
