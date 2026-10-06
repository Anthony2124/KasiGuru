package com.kasiguru.ui.screens.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** Inbox times read relative to now; older messages keep the text they were stored with. */
class InboxTimestampTest {

    private fun at(time: LocalDateTime): Long = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val now = LocalDateTime.of(2026, 10, 6, 18, 0)

    @Test
    fun recentMessagesCountMinutes() {
        assertEquals("Just now", inboxTimestamp(at(now.minusSeconds(20)).toString(), at(now)))
        assertEquals("5 min ago", inboxTimestamp(at(now.minusMinutes(5)).toString(), at(now)))
    }

    @Test
    fun earlierTodayCountsHours() {
        assertEquals("3 h ago", inboxTimestamp(at(now.minusHours(3)).toString(), at(now)))
    }

    @Test
    fun olderMessagesNameTheDay() {
        assertEquals("Yesterday", inboxTimestamp(at(now.minusDays(1)).toString(), at(now)))
        assertEquals("Friday", inboxTimestamp(at(LocalDateTime.of(2026, 10, 2, 9, 0)).toString(), at(now)))
        assertEquals("1 Sep 2026", inboxTimestamp(at(LocalDateTime.of(2026, 9, 1, 9, 0)).toString(), at(now)))
    }

    @Test
    fun legacyTextIsShownAsStored() {
        assertEquals("2 hours ago", inboxTimestamp("2 hours ago", at(now)))
    }
}
