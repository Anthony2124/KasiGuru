package com.kasiguru.domain.gamification

import kotlin.math.floor
import kotlin.math.pow

object XpPolicy {
    const val VERSION = 2
    val thresholds = (1..30).map { floor(100.0 * (it - 1).toDouble().pow(1.5) / 10 + 0.5).toInt() * 10 }
    fun level(xp: Int): Int = (thresholds.indexOfLast { xp >= it } + 1).coerceAtLeast(1)
    fun lesson(perfect: Boolean): Int = 20 + if (perfect) 5 else 0
    fun game(mode: String, correct: Int, total: Int, perfect: Boolean): Int {
        require(total > 0 && correct in 0..total) { "Game correctness must be a count, not XP." }
        val weight = when(mode) {
            "audio_quiz", "recall", "aspect_builder", "sentence_order" -> 3
            "word_search", "word_wheel" -> 1
            else -> 2
        }
        return (5 + correct * weight + if (perfect) 5 else 0).coerceAtMost(40)
    }
}

/** Pure receipt representation, also used to merge per-device evidence by stable ID. */
data class RewardRecord(val id: String, val kind: String, val source: String, val day: String,
    val xp: Int, val value: Int = 1, val imported: Boolean = false)

data class XpTotals(val activity: Int, val bonus: Int, val byDay: Map<String, Int>) {
    val total: Int get() = activity + bonus
}

object RewardLedger {
    fun merge(a: RewardRecord, b: RewardRecord): RewardRecord {
        require(a.id == b.id && a.kind == b.kind && a.source == b.source)
        require(a.day == b.day || a.kind in setOf("badge","mastery","story","approved"))
        return a.copy(day = minOf(a.day,b.day), xp = maxOf(a.xp,b.xp), value = maxOf(a.value,b.value), imported = a.imported || b.imported)
    }
    /** Cumulative lesson/game targets + daily replay offers settle to max(improvement,replay). */
    fun totals(records: List<RewardRecord>): XpTotals {
        var activity = 0
        var bonus = 0
        val days = mutableMapOf<String,Int>()
        fun addDay(day: String, xp: Int) { if(day.isNotBlank()) days[day] = (days[day] ?: 0) + xp }
        val content = records.filter { it.kind in setOf("lesson", "game", "replay") }.groupBy { it.source }
        content.values.forEach { rows ->
            var best = rows.filter { it.imported && it.kind != "replay" }.maxOfOrNull { it.xp } ?: 0
            activity += best
            rows.filter { !it.imported }.groupBy { it.day }.toSortedMap().forEach { (day, daily) ->
                val target = daily.filter { it.kind != "replay" }.maxOfOrNull { it.xp } ?: best
                val delta = (target - best).coerceAtLeast(0)
                val repeat = daily.filter { it.kind == "replay" }.maxOfOrNull { it.xp } ?: 0
                val earned = maxOf(delta, repeat)
                best = maxOf(best,target)
                activity += earned
                addDay(day,earned)
            }
        }
        // The review cap applies after a union of devices, not once on each device.
        records.filter { it.kind == "review" }.groupBy { it.day }.forEach { (day, rows) ->
            val earned = rows.sumOf { it.xp }.coerceAtMost(60)
            activity += earned
            addDay(day, rows.filter { !it.imported }.sumOf { it.xp }.coerceAtMost(60))
        }
        records.filter { it.kind !in setOf("lesson","game","replay","review") }.forEach { row ->
            if(row.kind == "badge") bonus += row.xp
            else { activity += row.xp; if(!row.imported) addDay(row.day,row.xp) }
        }
        return XpTotals(activity,bonus,days)
    }
}
