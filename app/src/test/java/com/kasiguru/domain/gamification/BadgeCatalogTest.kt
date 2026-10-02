package com.kasiguru.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeCatalogTest {

    private fun family(id: String) = BadgeCatalog.families.first { it.id == id }

    @Test
    fun `a single one reads in the singular`() {
        assertEquals("1 day", family("consistent_learner").amount(1))
        assertEquals("3 days", family("consistent_learner").amount(3))
        assertEquals("1 story", family("story_reader").amount(1))
        assertEquals("7 stories", family("story_reader").amount(7))
        assertEquals("1 category", family("category_scholar").amount(1))
    }

    @Test
    fun `progress agrees with the target`() {
        assertEquals("0 / 1 day", family("consistent_learner").progress(0, 1))
        assertEquals("2 / 3 days", family("consistent_learner").progress(2, 3))
    }

    @Test
    fun `the level family reads as a rank`() {
        assertEquals("Level 6", family("journey_rank").amount(6))
        assertEquals("Level 3 of 6", family("journey_rank").progress(3, 6))
    }

    @Test
    fun `no tier description reads as a plural of one`() {
        BadgeCatalog.rows().forEach { row ->
            assertTrue(row.description, !Regex("""\b1 \w+s\b""").containsMatchIn(row.description))
        }
    }
}
