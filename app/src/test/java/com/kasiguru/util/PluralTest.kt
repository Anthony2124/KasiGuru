package com.kasiguru.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PluralTest {

    @Test
    fun `one takes the singular`() {
        assertEquals("1 day", pluralize(1, "day"))
    }

    @Test
    fun `zero and many take the plural`() {
        assertEquals("0 days", pluralize(0, "day"))
        assertEquals("30 days", pluralize(30, "day"))
    }

    @Test
    fun `irregular plurals are passed in`() {
        assertEquals("1 story", pluralize(1, "story", "stories"))
        assertEquals("4 stories", pluralize(4, "story", "stories"))
    }
}
