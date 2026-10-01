package com.kasiguru.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import org.json.JSONArray
import com.google.firebase.firestore.AggregateSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.kasiguru.data.local.KasiGuruDatabase
import com.kasiguru.data.local.entity.PublicProfileCacheEntity
import com.kasiguru.data.remote.model.PublicProfileDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublicProfileRepository @Inject constructor(private val firestore: FirebaseFirestore, private val db: KasiGuruDatabase) {
    private val rarityCache = mutableMapOf<String, Pair<Long, Long>>()
    private fun objectMap(json: JSONObject): Map<String, Any> = json.keys().asSequence().associateWith { key ->
        fun unwrap(value: Any): Any = when (value) {
            is JSONObject -> objectMap(value)
            is JSONArray -> (0 until value.length()).map { unwrap(value.get(it)) }
            else -> value
        }
        unwrap(json.get(key))
    }
    suspend fun badgeRarity(ids: List<String>): Map<String, Long> = coroutineScope {
        ids.distinct().map { id -> async {
            val now = System.currentTimeMillis()
            val cached = synchronized(rarityCache) { rarityCache[id] }
            if (cached != null && now - cached.first < 3_600_000) id to cached.second
            else try {
                val count = firestore.collection("public_profiles").whereArrayContains("badgeIds", id)
                    .count().get(AggregateSource.SERVER).await().count
                synchronized(rarityCache) { rarityCache[id] = now to count }
                id to count
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { id to Long.MAX_VALUE }
        } }.awaitAll().toMap()
    }
    fun currentWeekId(): String {
        val date = java.time.LocalDate.now(); val fields = java.time.temporal.WeekFields.ISO
        return "${date.get(fields.weekBasedYear())}-W${date.get(fields.weekOfWeekBasedYear()).toString().padStart(2, '0')}"
    }
    suspend fun localWeeklyXp(): Int {
        val today = java.time.LocalDate.now()
        val start = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString()
        return com.kasiguru.domain.gamification.RewardLedger.totals(db.rewardDao().all().map { it.record() })
            .byDay.filterKeys { it >= start && it <= today.toString() }.values.sum()
    }
    suspend fun weeklyRank(uid: String, profile: PublicProfileDto): Int? {
        if (profile.weekId != currentWeekId()) return null
        val row = firestore.collection("leaderboard_public").document(uid).get().await()
        if (!row.exists() || row.getString("weekStartDate") != currentWeekId()) return null
        val score = row.getLong("weeklyXp") ?: return null
        return (firestore.collection("leaderboard_public").whereEqualTo("weekStartDate", currentWeekId())
            .whereGreaterThan("weeklyXp", score).count().get(AggregateSource.SERVER).await().count + 1).toInt()
    }
    suspend fun cached(uid: String): PublicProfileDto? = db.publicProfileCacheDao().get(uid)?.let {
        runCatching { PublicProfileDto.fromMap(objectMap(JSONObject(it.payload))) }.getOrNull()
    }
    suspend fun refresh(uid: String): PublicProfileDto? {
        val doc = firestore.collection("public_profiles").document(uid).get().await()
        if (!doc.exists()) { db.publicProfileCacheDao().delete(uid); return null }
        val profile = PublicProfileDto.fromMap(doc.data.orEmpty())
        db.publicProfileCacheDao().put(PublicProfileCacheEntity(uid, JSONObject(profile.toMap()).toString()))
        return profile
    }
}
