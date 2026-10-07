package com.kasiguru.domain.gamification

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LeaderboardRefreshGateTest {
    @Test fun repeatedVisitsShareOneRefreshUntilExpiry() = runBlocking {
        var now = 0L
        val gate = LeaderboardRefreshGate(nowMillis = { now })
        var reads = 0
        gate.refresh("weekly", "account:week1") { reads++ }
        now = 179_999
        gate.refresh("weekly", "account:week1") { reads++ }
        assertEquals(1, reads)
        now = 180_000
        gate.refresh("weekly", "account:week1") { reads++ }
        assertEquals(2, reads)
    }

    @Test fun boardsAccountsAndWeeksDoNotReuseEachOthersFreshness() = runBlocking {
        val gate = LeaderboardRefreshGate(nowMillis = { 100 })
        var reads = 0
        gate.refresh("weekly", "a:week1") { reads++ }
        gate.refresh("alltime", "a:week1") { reads++ }
        gate.refresh("weekly", "b:week1") { reads++ }
        gate.refresh("weekly", "b:week2") { reads++ }
        assertEquals(4, reads)
    }

    @Test fun concurrentVisitsDoNotDuplicateServerQueries() = runBlocking {
        val gate = LeaderboardRefreshGate(nowMillis = { 100 })
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var reads = 0
        val first = async {
            gate.refresh("weekly", "a:week1") { reads++; started.complete(Unit); finish.await() }
        }
        started.await()
        val second = async { gate.refresh("weekly", "a:week1") { reads++ } }
        finish.complete(Unit)
        awaitAll(first, second)
        assertEquals(1, reads)
    }

    @Test fun failedRefreshIsRetried() = runBlocking {
        val gate = LeaderboardRefreshGate(nowMillis = { 100 })
        var attempts = 0
        try { gate.refresh("weekly", "a:week1") { attempts++; error("offline") } }
        catch (_: IllegalStateException) { }
        gate.refresh("weekly", "a:week1") { attempts++ }
        assertEquals(2, attempts)
    }
}
