package com.kasiguru.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kasiguru.data.local.entity.RewardCelebrationEntity
import com.kasiguru.domain.gamification.*
import androidx.compose.runtime.LaunchedEffect
import com.kasiguru.util.audio.LocalSoundEffects
import com.kasiguru.util.audio.Sfx

@Composable
fun RewardCelebrationDialog(reward: RewardCelebrationEntity,onDismiss: () -> Unit) {
    val badges = reward.badgeIds.split(',').filter { BadgeCatalog.familyFor(it) != null }
        .groupBy { BadgeCatalog.familyFor(it)!! }.map { (family, ids) ->
            family to ids.mapNotNull(BadgeCatalog::tierFor).maxBy { it.ordinal }
        }
    val sounds = LocalSoundEffects.current
    LaunchedEffect(reward) { sounds?.play(if (reward.levelChanged) Sfx.LevelUp else Sfx.Badge) }
    AlertDialog(modifier = Modifier.tapSounds(), onDismissRequest = onDismiss,
        title = { Text(if(reward.levelChanged) "Level ${reward.level} reached!" else "Badge upgraded!") },
        text = {
            Column {
                if(reward.activityXp > 0) Text("+${reward.activityXp} activity XP")
                if(reward.bonusXp > 0) Text("+${reward.bonusXp} badge XP")
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(badges,key = { it.first.id }) { (family,tier) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StandardBadgeMedal(tier = tier, earned = true, size = 64.dp, familyId = family.id)
                            Column {
                                Text(family.name,style = MaterialTheme.typography.titleMedium)
                                Text(tier.label)
                            }
                        }
                    }
                }
                if(badges.size > 1) Text("All earned milestones are saved in your collection.")
            }
        },confirmButton = { TextButton(onClick = onDismiss) { Text("Continue") } })
}
