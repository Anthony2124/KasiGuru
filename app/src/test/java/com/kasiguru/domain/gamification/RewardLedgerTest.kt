package com.kasiguru.domain.gamification

import org.junit.Assert.*
import org.junit.Test

class RewardLedgerTest {
    private fun r(id: String,kind: String,source: String = "game:word_match#1",day: String = "2026-10-01",
        xp: Int,value: Int = 1,imported: Boolean = false) = RewardRecord(id,kind,source,day,xp,value,imported)
    @Test fun rewardsFollowDifficultyAndCapWithoutUsingSpeed() {
        assertEquals(20,XpPolicy.game("word_match",5,5,true))
        assertEquals(25,XpPolicy.game("audio_quiz",5,5,true))
        assertEquals(40,XpPolicy.game("sentence_order",15,15,true))
        assertEquals(15,XpPolicy.game("word_search",5,5,true))
        assertEquals(20,XpPolicy.lesson(false))
        assertEquals(25,XpPolicy.lesson(true))
    }
    @Test(expected = IllegalArgumentException::class)
    fun aScoreMeasuredInXpCannotBeUsedAsCorrectness() { XpPolicy.game("sentence_order",50,5,true) }
    @Test fun replayAndImprovementAreComparedWithoutDoublePayment() {
        val rows = listOf(r("first","game",xp = 13),
            r("better","game",day = "2026-10-02",xp = 20),
            r("replay","replay",day = "2026-10-02",xp = 5))
        assertEquals(20,RewardLedger.totals(rows).activity)
        assertEquals(7,RewardLedger.totals(rows).byDay["2026-10-02"])
        assertEquals(25,RewardLedger.totals(rows + r("third","replay",day = "2026-10-03",xp = 5)).activity)
    }
    @Test fun importedCompletionDoesNotCountTowardDailyGoals() {
        val rows = listOf(r("old","game",day = "",xp = 5,imported = true),
            r("new","game",xp = 20),r("repeat","replay",xp = 5))
        val totals = RewardLedger.totals(rows)
        assertEquals(20,totals.activity)
        assertEquals(15,totals.byDay["2026-10-01"])
    }
    @Test fun reviewCapAppliesToTheCombinedEvidenceOfBothDevices() {
        val rows = (1..40).map { r("review:$it","review","word:$it",xp = 3) }
        assertEquals(60,RewardLedger.totals(rows).activity)
        assertEquals(60,RewardLedger.totals(rows).byDay["2026-10-01"])
    }
    @Test fun badgeBonusesAffectLevelButNotDailyActivity() {
        val totals = RewardLedger.totals(listOf(r("lesson","lesson",xp = 20),r("badge","badge",xp = 120)))
        assertEquals(140,totals.total)
        assertEquals(20,totals.byDay["2026-10-01"])
        assertEquals(2,XpPolicy.level(totals.total))
    }
    @Test fun receiptMergeIsIdempotentCommutativeAndKeepsBestEvidence() {
        val a = r("one","game",xp = 13)
        val b = a.copy(xp = 20)
        assertEquals(b,RewardLedger.merge(a,b))
        assertEquals(RewardLedger.merge(a,b),RewardLedger.merge(b,a))
        assertEquals(b,RewardLedger.merge(b,b))
    }
    @Test fun singleLifetimeMilestonesUseEarliestDayAndImportedHistoryStaysImported() {
        val a = r("mastery","mastery",day = "2026-09-30",xp = 5,imported = true)
        val b = a.copy(day = "2026-10-01",imported = false)
        val merged = RewardLedger.merge(a,b)
        assertEquals("2026-09-30",merged.day)
        assertTrue(merged.imported)
        assertTrue(RewardLedger.totals(listOf(merged)).byDay.isEmpty())
    }
    @Test fun allThirtyLevelBoundariesAgreeWithDisplayAndStorage() {
        assertEquals(30,XpPolicy.thresholds.size)
        assertEquals(15620,XpPolicy.thresholds.last())
        XpPolicy.thresholds.forEachIndexed { index,xp ->
            assertEquals(index+1,XpPolicy.level(xp))
            if(index > 0) assertEquals(index,XpPolicy.level(xp-1))
        }
    }
    @Test fun allElevenFamiliesHaveSixStrictlyIncreasingMilestonesAndStableIdentities() {
        assertEquals(11,BadgeCatalog.families.size)
        assertEquals(66,BadgeCatalog.rows().map { it.id }.toSet().size)
        BadgeCatalog.families.forEach { f ->
            assertEquals(6,f.thresholds.size)
            assertTrue(f.thresholds.zipWithNext().all { (a,b) -> a < b })
        }
        assertTrue(BadgeCatalog.rows().filter { it.metricType == "level" }.all { it.xpReward == 0 })
        assertNull(BadgeCatalog.familyFor("badge:word_explorer:7"))
    }
}
