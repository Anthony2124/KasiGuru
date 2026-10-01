package com.kasiguru.domain.gamification

import com.kasiguru.data.local.entity.AchievementEntity

object BadgeShowcase {
    fun candidates(ids: List<String>): List<AchievementEntity> = BadgeCatalog.rows()
        .filter { it.id in ids }.groupBy { BadgeCatalog.familyFor(it.id)?.id }
        .values.map { family -> family.maxBy { BadgeCatalog.tierFor(it.id)?.ordinal ?: -1 } }
    fun choose(ids: List<String>, earnedByPlayers: Map<String, Long>): List<AchievementEntity> = candidates(ids)
        .sortedWith(compareByDescending<AchievementEntity> { BadgeCatalog.tierFor(it.id)?.ordinal ?: -1 }
            .thenBy { earnedByPlayers[it.id] ?: Long.MAX_VALUE }.thenBy { it.id }).take(3)
}
