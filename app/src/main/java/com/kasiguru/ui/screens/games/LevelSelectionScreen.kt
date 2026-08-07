package com.kasiguru.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.GameLevelEntity
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@Composable
fun LevelSelectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (String, Int) -> Unit, // gameType, levelId
    viewModel: LevelSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Match the dark blue aesthetics from the image
    val darkBlueBgStart = Color(0xFF141A4F)
    val darkBlueBgEnd = Color(0xFF0F1230)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(darkBlueBgStart, darkBlueBgEnd)))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stars/Coins indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = Iconsax.StarBold),
                        contentDescription = "Total Stars",
                        tint = PlayGoldStart,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${uiState.totalStars}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
                
                Text(
                    text = "LEVEL SELECTION",
                    color = PlayGoldStart,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = 1.sp
                )
                
                // Invisible box for balance
                Box(modifier = Modifier.size(40.dp))
            }
            
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PlayGoldStart)
                }
            } else {
                // Level Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.levels) { level ->
                        LevelButton(
                            level = level,
                            onClick = {
                                if (level.isUnlocked) {
                                    onNavigateToGame(uiState.gameType, level.levelNumber)
                                }
                            }
                        )
                    }
                }
                
                // Bottom Bar buttons (Back)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5D93)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(160.dp)
                            .height(56.dp)
                    ) {
                        Text(
                            text = "BACK",
                            color = PlayGoldStart,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelButton(
    level: GameLevelEntity,
    onClick: () -> Unit
) {
    val buttonBgColor = Color(0xFF0F396B)
    val buttonBorderColor = Color(0xFF1E5D93)
    
    Box(
        modifier = Modifier
            .aspectRatio(1f) // Ensure it's square-ish
            .clip(RoundedCornerShape(16.dp))
            .background(if (level.isUnlocked) buttonBorderColor else buttonBorderColor.copy(alpha = 0.5f))
            .clickable(enabled = level.isUnlocked, onClick = onClick)
            .padding(4.dp) // creates the "border" effect
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(if (level.isUnlocked) buttonBgColor else buttonBgColor.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            if (level.isUnlocked) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = level.levelNumber.toString(),
                        color = PlayGoldStart,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = Iconsax.LockBold),
                    contentDescription = "Locked",
                    tint = PlayGoldStart.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Stars
        if (level.isUnlocked) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp), // Hang stars off the edge slightly
                horizontalArrangement = Arrangement.spacedBy((-6).dp)
            ) {
                // Display 3 stars, colored if earned, dark if not
                for (i in 1..3) {
                    val isEarned = i <= level.starsEarned
                    Icon(
                        painter = painterResource(id = Iconsax.StarBold),
                        contentDescription = null,
                        tint = if (isEarned) PlayGoldStart else Color(0xFF0B1229),
                        modifier = Modifier
                            .size(16.dp)
                            .offset(y = if (i == 2) (-4).dp else 0.dp) // Middle star is slightly higher
                    )
                }
            }
        }
    }
}
