package com.kasiguru.domain.gamification

import com.kasiguru.data.local.entity.AchievementEntity

/**
 * One badge family as Profile shows it: the highest tier earned and the tier being worked on.
 *
 * Profile used to list the latest earned tier rows in a side-scrolling strip, so a learner saw at
 * most a handful of medals and never the eleven badges there are to earn. Summarising per family
 * lets the whole set fit on screen at once.
 */
data class BadgeFamilySummary(
    val family: BadgeFamily,
    /** Null until the first tier is earned. */
    val highest: BadgeTier?,
    val earnedTiers: Int,
    /** The first tier not yet earned, or null once all six are. */
    val next: AchievementEntity?
) {
    val nextTier: BadgeTier? get() = next?.let { BadgeCatalog.tierFor(it.id) }

    /** Progress toward [next], 0..1; 1 when every tier is earned. */
    val progress: Float
        get() = next?.let {
            if (it.requiredValue <= 0) 0f else (it.currentValue.toFloat() / it.requiredValue).coerceIn(0f, 1f)
        } ?: 1f
}

object BadgeSummary {

    /**
     * Every family in the catalog, pinned families first in pin order, the rest in catalog order -
     * a fixed order, so a badge is always in the same place on the grid.
     */
    fun families(
        achievements: List<AchievementEntity>,
        pinnedFamilyIds: List<String> = emptyList()
    ): List<BadgeFamilySummary> {
        val byFamily = achievements.groupBy { BadgeCatalog.familyFor(it.id)?.id }
        val summaries = BadgeCatalog.families.map { family ->
            val rows = byFamily[family.id].orEmpty()
                .sortedBy { BadgeCatalog.tierFor(it.id)?.ordinal ?: Int.MAX_VALUE }
            val earned = rows.filter { it.isUnlocked }
            BadgeFamilySummary(
                family = family,
                highest = earned.mapNotNull { BadgeCatalog.tierFor(it.id) }.maxByOrNull { it.ordinal },
                earnedTiers = earned.size,
                next = rows.firstOrNull { !it.isUnlocked }
            )
        }
        val pins = pinnedFamilyIds.filter { it.isNotBlank() }
        return summaries.sortedBy { pins.indexOf(it.family.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    /** The unfinished tier closest to done, across every family; null when nothing is in progress. */
    fun nextUp(summaries: List<BadgeFamilySummary>): BadgeFamilySummary? =
        summaries.filter { it.next != null && it.next.requiredValue > 0 }.maxByOrNull { it.progress }
}
