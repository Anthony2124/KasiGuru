package com.kasiguru.ui.screens.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.domain.gamification.ProfileBackgroundCatalog
import com.kasiguru.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBackgroundPicker(progress: UserProgressEntity, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface) {
        Text("Choose your background", style = MaterialTheme.typography.headlineSmall, color = Ink, modifier = Modifier.padding(horizontal = Space.gutter))
        Text("Unlock more places as you learn.", color = Muted, modifier = Modifier.padding(horizontal = Space.gutter))
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), contentPadding = PaddingValues(Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            ProfileBackgroundCatalog.backgrounds.chunked(2).forEach { row ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        row.forEach { background ->
                            val unlocked = background.isUnlocked(progress.level, progress.longestStreak)
                            val selected = progress.profileBackgroundId == background.id
                            Column(Modifier.weight(1f).clip(Shapes.tile)
                                .border(if (selected) 2.dp else 1.dp, if (selected) BrandLime else BorderHairline, Shapes.tile)
                                .clickable(enabled = unlocked) { onSelect(background.id) }) {
                                Box(Modifier.fillMaxWidth().height(104.dp)) {
                                    Image(painterResource(Scenery.forProfile(background.id).res), null,
                                        contentScale = ContentScale.Crop, alpha = if (unlocked) 1f else .35f, modifier = Modifier.fillMaxSize())
                                    if (!unlocked) Icon(painterResource(Iconsax.Lock), "Locked", tint = Cream, modifier = Modifier.align(Alignment.Center).size(28.dp))
                                }
                                Column(Modifier.padding(12.dp)) {
                                    Text(background.title, color = Ink, style = MaterialTheme.typography.titleSmall)
                                    Text(if (selected) "Selected" else if (unlocked) "Available" else background.requirement,
                                        color = Muted, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
