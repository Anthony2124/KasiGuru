package com.kasiguru.domain.gamification

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Coalesces refreshes and keeps each board fresh for three minutes within one account/week. */
class LeaderboardRefreshGate(
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
    private val freshForMillis: Long = 3 * 60 * 1000
) {
    private data class Stamp(val context: String, val at: Long)
    private val stamps = mutableMapOf<String, Stamp>()
    private val mutex = Mutex()

    suspend fun refresh(board: String, context: String, fetch: suspend () -> Unit) = mutex.withLock {
        val stamp = stamps[board]
        val age = stamp?.let { nowMillis() - it.at }
        if (stamp?.context == context && age != null && age >= 0 && age < freshForMillis) return@withLock
        fetch()
        // A failed or cancelled fetch never makes an old Room board fresh.
        stamps[board] = Stamp(context, nowMillis())
    }
}
