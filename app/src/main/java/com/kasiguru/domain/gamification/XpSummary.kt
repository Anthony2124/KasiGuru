package com.kasiguru.domain.gamification

import java.time.LocalDate

/** Where XP comes from, as the XP page groups it. */
enum class XpSource(val label: String) {
    Lessons("Lessons"),
    Games("Games"),
    Reviews("Word reviews"),
    Stories("Stories"),
    Community("Approved contributions"),
    Badges("Badge bonuses")
}

/**
 * The numbers behind the XP page, worked out from the reward receipts with the same settlement
 * [RewardLedger.totals] uses for the learner's total - so the parts always add up to the whole,
 * rather than re-counting a replayed level or a capped review day.
 */
object XpSummary {

    /**
     * XP per [XpSource]. The receipts are split into groups the ledger already settles
     * independently - content by source, reviews by day, everything else row by row - and each
     * group is settled on its own, so no group's cap or improvement rule reaches into another.
     */
    fun bySource(records: List<RewardRecord>): Map<XpSource, Int> {
        val content = setOf("lesson", "game", "replay")
        val groups = mutableMapOf<XpSource, MutableList<RewardRecord>>()
        fun add(source: XpSource, rows: List<RewardRecord>) { groups.getOrPut(source) { mutableListOf() } += rows }

        // A replay receipt carries no hint of what it replayed; the other receipts for the same
        // source do, so a source with a lesson receipt is a lesson and the rest are games.
        records.filter { it.kind in content }.groupBy { it.source }.values.forEach { rows ->
            add(if (rows.any { it.kind == "lesson" }) XpSource.Lessons else XpSource.Games, rows)
        }
        records.filter { it.kind !in content }.forEach { row ->
            when (row.kind) {
                "review", "mastery" -> add(XpSource.Reviews, listOf(row))
                "story" -> add(XpSource.Stories, listOf(row))
                "approved" -> add(XpSource.Community, listOf(row))
                "badge" -> add(XpSource.Badges, listOf(row))
            }
        }
        return groups.mapValues { (_, rows) -> RewardLedger.totals(rows).total }.filterValues { it > 0 }
    }

    /** XP earned on each of the seven days ending [today], oldest first. Badge bonuses are not dated. */
    fun lastSevenDays(records: List<RewardRecord>, today: LocalDate): List<Pair<LocalDate, Int>> {
        val byDay = RewardLedger.totals(records).byDay
        return (6 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            day to (byDay[day.toString()] ?: 0)
        }
    }
}
