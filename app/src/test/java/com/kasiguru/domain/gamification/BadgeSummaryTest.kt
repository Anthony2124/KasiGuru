package com.kasiguru.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BadgeSummaryTest {

    /** The catalog's rows with the first [tiers] of [familyId] earned and [current] counted toward the next. */
    private fun rows(familyId: String, tiers: Int, current: Int = 0) = BadgeCatalog.rows().map { row ->
        if (BadgeCatalog.familyFor(row.id)?.id != familyId) return@map row
        val tier = BadgeCatalog.tierFor(row.id)!!.ordinal + 1
        row.copy(isUnlocked = tier <= tiers, currentValue = current)
    }

    @Test
    fun `every family appears once, earned or not`() {
        val summaries = BadgeSummary.families(BadgeCatalog.rows())

        assertEquals(BadgeCatalog.families.map { it.id }, summaries.map { it.family.id })
        summaries.forEach { assertNull(it.highest) }
    }

    @Test
    fun `a family shows its highest earned tier and the next one`() {
        val wordExplorer = BadgeSummary.families(rows("word_explorer", tiers = 3, current = 38))
            .first { it.family.id == "word_explorer" }

        assertEquals(BadgeTier.ACHIEVER, wordExplorer.highest)
        assertEquals(3, wordExplorer.earnedTiers)
        assertEquals(BadgeTier.EXPERT, wordExplorer.nextTier)
        assertEquals(38f / 150, wordExplorer.progress, 0.0001f)
    }

    @Test
    fun `a finished family has nothing next`() {
        val done = BadgeSummary.families(rows("story_reader", tiers = 6)).first { it.family.id == "story_reader" }

        assertEquals(BadgeTier.LEGEND, done.highest)
        assertNull(done.next)
        assertEquals(1f, done.progress)
    }

    @Test
    fun `pinned families lead, the rest keep catalog order`() {
        val ids = BadgeSummary.families(BadgeCatalog.rows(), listOf("story_reader", "", "mode_explorer")).map { it.family.id }

        assertEquals(listOf("story_reader", "mode_explorer"), ids.take(2))
        assertEquals(BadgeCatalog.families.map { it.id }.filterNot { it in setOf("story_reader", "mode_explorer") }, ids.drop(2))
    }

    @Test
    fun `next up is the unfinished tier closest to done`() {
        val achievements = BadgeCatalog.rows().map { row ->
            when (row.id) {
                "badge:review_keeper:1" -> row.copy(currentValue = 9)   // 9 of 10
                "badge:word_explorer:1" -> row.copy(currentValue = 0)
                else -> row
            }
        }

        assertEquals("review_keeper", BadgeSummary.nextUp(BadgeSummary.families(achievements))?.family?.id)
    }
}
