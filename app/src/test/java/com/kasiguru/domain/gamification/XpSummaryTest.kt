package com.kasiguru.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class XpSummaryTest {

    private val today = LocalDate.of(2026, 10, 5)
    private fun day(back: Long) = today.minusDays(back).toString()

    private val records = listOf(
        RewardRecord("lesson:a:${day(1)}", "lesson", "lesson:a", day(1), 25),
        RewardRecord("replay:lesson:a:${day(0)}", "replay", "lesson:a", day(0), 5),
        RewardRecord("game:word_match:1:${day(0)}", "game", "game:word_match:1", day(0), 20),
        RewardRecord("replay:game:word_match:1:${day(0)}", "replay", "game:word_match:1", day(0), 5),
        // Reviews cap at 60 XP a day.
        RewardRecord("review:x:${day(0)}", "review", "x", day(0), 40),
        RewardRecord("review:y:${day(0)}", "review", "y", day(0), 40),
        RewardRecord("badge:word_explorer:1", "badge", "badge:word_explorer:1", day(0), 20),
        RewardRecord("perfect:game:word_match:1", "perfect", "game:word_match:1", "", 0)
    )

    @Test
    fun `the parts add up to the ledger total`() {
        assertEquals(RewardLedger.totals(records).total, XpSummary.bySource(records).values.sum())
    }

    @Test
    fun `each source is settled the way the ledger settles it`() {
        val parts = XpSummary.bySource(records)

        assertEquals("25 for the lesson, then a 5 XP replay the next day", 30, parts[XpSource.Lessons])
        assertEquals("a same-day replay settles to max(improvement, replay)", 20, parts[XpSource.Games])
        assertEquals("reviews cap at 60 a day", 60, parts[XpSource.Reviews])
        assertEquals(20, parts[XpSource.Badges])
    }

    @Test
    fun `sources with no XP are left out`() {
        assertEquals(
            setOf(XpSource.Lessons, XpSource.Games, XpSource.Reviews, XpSource.Badges),
            XpSummary.bySource(records).keys
        )
    }

    @Test
    fun `the week runs oldest first and ends today, without badge bonuses`() {
        val week = XpSummary.lastSevenDays(records, today)

        assertEquals(7, week.size)
        assertEquals(today, week.last().first)
        assertEquals(25, week[5].second)
        assertEquals(5 + 20 + 60, week.last().second)
    }
}
