package com.kasiguru.data.repository

import androidx.room.withTransaction
import com.kasiguru.data.local.KasiGuruDatabase
import com.kasiguru.data.local.entity.*
import com.kasiguru.domain.gamification.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class RewardOutcome(val activityXp: Int, val bonusXp: Int, val level: Int,
    val promotions: List<String> = emptyList())

/** All grants, projections and normalization commit in the same Room transaction. */
@Singleton
class GamificationRepository @Inject constructor(private val db: KasiGuruDatabase) {
    private val dao get() = db.rewardDao()
    private val events = MutableSharedFlow<RewardOutcome>(extraBufferCapacity = 16)
    val rewardEvents = events.asSharedFlow()
    private fun today() = LocalDate.now().toString()

    private suspend fun put(row: RewardReceiptEntity) {
        val old = dao.get(row.id)
        val merged = if(old == null) row else RewardLedger.merge(old.record(),row.record()).let {
            row.copy(day = it.day, xp = it.xp, value = it.value, imported = it.imported)
        }
        if(merged != old) dao.put(merged)
    }

    suspend fun ensureNormalized() = db.withTransaction { normalizeLocked() }

    private suspend fun normalizeLocked() {
        val existing = db.userProgressDao().getUserProgressOnce()
        val progress = existing ?: UserProgressEntity().also { db.userProgressDao().insertOrUpdate(it) }
        if(dao.normalization() != null) return
        val currentIds = db.achievementDao().getAllIds().toSet()
        db.achievementDao().insertAll(BadgeCatalog.rows().filter { it.id !in currentIds })
        if(db.storyDao().getStoryCount() == 0) db.storyDao().insertMissing(com.kasiguru.data.local.DatabaseSeeder.getInitialStories())
        db.storyDao().getAllStoriesOnce().filter { it.isUnlocked || it.requiredXp <= progress.totalXp }.forEach {
            put(RewardReceiptEntity("access:story:${it.id}","access","story:${it.id}","",0,1,true))
        }
        importSupportedHistoryLocked()
        settleLocked(imported = true)
        val normalized = db.userProgressDao().getUserProgressOnce()!!
        dao.putNormalization(ProgressNormalizationEntity(originalXp = progress.totalXp,
            originalLevel = progress.level, normalizedXp = normalized.totalXp,
            unsupportedHistory = "Older review ratings, manual word mastery, game accuracy/hints and replay dates could not be reconstructed. Original XP and badges are archived.",
            acknowledged = progress.totalXp == 0 || progress.xpPolicyVersion >= XpPolicy.VERSION))
        val goal = if(progress.xpPolicyVersion < XpPolicy.VERSION && progress.isOnboardingCompleted) when(progress.dailyGoalXp) {
            in 0..50 -> 30
            in 51..100 -> 50
            in 101..150 -> 80
            else -> 100
        } else progress.dailyGoalXp
        db.userProgressDao().insertOrUpdate(normalized.copy(dailyGoalXp = goal))
    }

    private suspend fun importSupportedHistoryLocked() {
        val receipts = dao.all()
        db.lessonDao().getAllOnce().filter { it.isComplete }.forEach { row ->
            val source = "lesson:${row.unitId}#${row.lessonIndex}"
            if(receipts.none { it.source == source && it.kind == "lesson" }) put(RewardReceiptEntity("$source:import", "lesson", source,"",
                XpPolicy.lesson(row.bestAccuracy >= 1f), if(row.unitId.startsWith("mastery:")) 0 else 1,true))
        }
        db.gameLevelDao().getAllLevelsOnce().filter { it.starsEarned >= 1 }.forEach { row ->
            val source = "game:${row.gameType}#${row.levelNumber}"
            if(receipts.none { it.source == source && it.kind == "game" }) put(RewardReceiptEntity("$source:import","game",source,"",5,1,true))
        }
        db.storyDao().getAllStoriesOnce().filter { it.isCompleted }.forEach { row ->
            val source = "story:${row.id}"
            if(receipts.none { it.source == source && it.kind == "story" }) put(RewardReceiptEntity(source,"story",source,"",20,1,true))
        }
    }

    /** Used only when restoring version-1 learning records; never guesses absent activity. */
    suspend fun importLegacyLearningState() = db.withTransaction {
        normalizeLocked()
        importSupportedHistoryLocked()
        settleLocked(imported = true)
        val report = dao.normalization()!!
        dao.putNormalization(report.copy(normalizedXp = db.userProgressDao().getUserProgressOnce()!!.totalXp))
    }

    private suspend fun transact(action: suspend () -> Unit): RewardOutcome {
        val result = db.withTransaction {
            normalizeLocked()
            val before = RewardLedger.totals(dao.all().map { it.record() })
            action()
            val promotions = settleLocked()
            val after = RewardLedger.totals(dao.all().map { it.record() })
            val outcome = RewardOutcome((after.activity-before.activity).coerceAtLeast(0),
                (after.bonus-before.bonus).coerceAtLeast(0),XpPolicy.level(after.total),promotions)
            if(promotions.isNotEmpty() || outcome.level > XpPolicy.level(before.total)) {
                dao.celebrate(RewardCelebrationEntity(java.util.UUID.randomUUID().toString(),
                    outcome.activityXp,outcome.bonusXp,outcome.level,outcome.level > XpPolicy.level(before.total),
                    promotions.joinToString(","),System.currentTimeMillis()))
            }
            outcome
        }
        if(result.activityXp > 0 || result.bonusXp > 0 || result.promotions.isNotEmpty()) events.tryEmit(result)
        return result
    }

    suspend fun lesson(unit: String, index: Int, accuracy: Float): RewardOutcome = transact {
        require(accuracy.isFinite() && accuracy in 0f..1f)
        val source = "lesson:$unit#$index"
        val previous = dao.all().filter { it.source == source && it.kind == "lesson" }
        val day = today()
        put(RewardReceiptEntity("$source:$day","lesson",source,day,XpPolicy.lesson(accuracy >= 1f),
            if(unit.startsWith("mastery:")) 0 else 1))
        if(previous.isNotEmpty() && previous.none { it.day == day && !it.imported })
            put(RewardReceiptEntity("replay:$source:$day","replay",source,day,5))
        val existing = db.lessonDao().get(unit,index)
        db.lessonDao().upsert(LessonProgressEntity(unit,index,true,
            maxOf(existing?.bestAccuracy ?: 0f,accuracy),(existing?.timesCompleted ?: 0)+1,System.currentTimeMillis()))
    }

    suspend fun game(mode: String, levelKey: String, number: Int, correct: Int, total: Int,
        stars: Int, perfect: Boolean): RewardOutcome = transact {
        if(stars < 1) return@transact
        require(!perfect || correct == total)
        val source = "game:$levelKey#$number"
        val day = today()
        val reward = XpPolicy.game(mode,correct,total,perfect)
        val previous = dao.all().filter { it.source == source && it.kind == "game" }
        put(RewardReceiptEntity("$source:$day","game",source,day,reward))
        if(previous.isNotEmpty() && previous.none { it.day == day && !it.imported })
            put(RewardReceiptEntity("replay:$source:$day","replay",source,day,reward/4))
        if(perfect) put(RewardReceiptEntity("perfect:$source","perfect",source,"",0))
        db.gameLevelDao().getLevel(levelKey,number)?.let { row ->
            db.gameLevelDao().update(row.copy(starsEarned = maxOf(row.starsEarned,stars.coerceIn(1,3)),isUnlocked = true))
            db.gameLevelDao().unlockLevel(levelKey,number+1)
        }
    }

    suspend fun story(id: Int): RewardOutcome = transact {
        val source = "story:$id"
        if(dao.get(source) == null) put(RewardReceiptEntity(source,"story",source,today(),20))
        db.storyDao().markAsCompleted(id)
    }

    suspend fun review(wordId: Int, rating: String, due: Boolean, mastered: Boolean,
        updatedWord: VocabularyEntity? = null): RewardOutcome = transact {
        val day = today()
        require(rating in setOf("AGAIN","HARD","GOOD","EASY"))
        val existingWord = db.vocabularyDao().getVocabularyById(wordId) ?: return@transact
        val word = updatedWord ?: existingWord
        if(updatedWord != null) {
            require(updatedWord.id == wordId)
            db.vocabularyDao().updateVocabulary(updatedWord)
        }
        val source = wordSource(word.kasiguranin)
        val positive = rating != "AGAIN"
        if(positive) put(RewardReceiptEntity("retrieval:$source:$day","retrieval",source,day,0))
        if(due) {
            val id = "review:$source:$day"
            val old = dao.get(id)
            val xp = old?.xp ?: when(rating) { "AGAIN" -> 1; "HARD" -> 2; else -> 3 }
            put(RewardReceiptEntity(id,"review",source,day,xp,if(positive) 1 else 0))
        }
        val records = dao.all()
        if(mastered && positive && records.count { it.kind == "retrieval" && it.source == source } >= 3)
            if(dao.get("mastery:$source") == null) put(RewardReceiptEntity("mastery:$source","mastery",source,day,5))
    }

    suspend fun approved(ids: List<String>,days: Map<String,String?> = emptyMap()): RewardOutcome = transact {
        ids.distinct().forEach { id ->
            val key = "approved:$id"
            val day = if(days.isEmpty()) today() else days[id].orEmpty()
            if(dao.get(key) == null) put(RewardReceiptEntity(key,"approved",id,day,10,1,day != today()))
        }
    }

    suspend fun updateMetrics(): RewardOutcome = transact { }

    private suspend fun settleLocked(imported: Boolean = false): List<String> {
        val masteredIds = dao.all().filter { it.kind == "mastery" }.map { it.source }.toSet()
        if(masteredIds.isNotEmpty()) db.vocabularyDao().getAllVocabularyOnce().groupBy { it.category }.forEach { (category, words) ->
            if(words.isNotEmpty() && words.all { wordSource(it.kasiguranin) in masteredIds })
                put(RewardReceiptEntity("category:$category","category",category,"",0))
        }
        val records = dao.all()
        val totals = RewardLedger.totals(records.map { it.record() })
        val progress = db.userProgressDao().getUserProgressOnce() ?: return emptyList()
        val games = records.filter { it.kind == "game" }.distinctBy { it.source }
        val metrics = mapOf(
            "verifiedWords" to records.count { it.kind == "mastery" },
            "distinctLessons" to records.filter { it.kind == "lesson" && it.value > 0 }.distinctBy { it.source }.size,
            "scheduledReviews" to records.count { it.kind == "review" && it.value > 0 },
            "streak" to progress.longestStreak,
            "distinctGameLevels" to games.size,
            "perfectLevels" to records.count { it.kind == "perfect" },
            "gameModesPlayed" to games.map { if(it.source.startsWith("game:word_search_")) "word_search" else it.source.substringAfter("game:").substringBefore('#') }.distinct().size,
            "storiesCompleted" to records.count { it.kind == "story" },
            "verifiedCategories" to records.count { it.kind == "category" },
            "submissionsApproved" to records.count { it.kind == "approved" },
            "level" to XpPolicy.level(totals.total))
        val promotions = mutableListOf<String>()
        val rows = db.achievementDao().getAllAchievementsOnce()
        rows.filter { BadgeCatalog.familyFor(it.id) != null && it.metricType != "level" }.forEach { row ->
            val value = metrics[row.metricType] ?: 0
            val achieved = row.isUnlocked || value >= row.requiredValue
            if(achieved && dao.get(row.id) == null) {
                put(RewardReceiptEntity(row.id,"badge",row.id,today(),row.xpReward,1,imported))
                if(!imported && !row.isUnlocked) promotions += row.id
            }
            val updated = row.copy(currentValue = maxOf(row.currentValue,value),isUnlocked = achieved,
                unlockedDate = row.unlockedDate ?: if(achieved) today() else null)
            if(updated != row) db.achievementDao().insert(updated)
        }
        val final = RewardLedger.totals(dao.all().map { it.record() })
        val level = XpPolicy.level(final.total)
        rows.filter { BadgeCatalog.familyFor(it.id) != null && it.metricType == "level" }.forEach { row ->
            val achieved = row.isUnlocked || level >= row.requiredValue
            val updated = row.copy(currentValue = level,isUnlocked = achieved,
                unlockedDate = row.unlockedDate ?: if(achieved) today() else null)
            if(updated != row) db.achievementDao().insert(updated)
            if(!imported && achieved && !row.isUnlocked) promotions += row.id
        }
        val projected = progress.copy(totalXp = final.total,level = level,
            activityXp = final.activity,badgeBonusXp = final.bonus,xpPolicyVersion = XpPolicy.VERSION,
            dailyXpDate = today(),dailyXpEarned = final.byDay[today()] ?: 0,
            wordsLearned = maxOf(progress.wordsLearned,metrics["verifiedWords"] ?: 0),
            storiesCompleted = metrics["storiesCompleted"] ?: 0,
            lessonsCompleted = metrics["distinctLessons"] ?: 0)
        if(projected != progress) db.userProgressDao().insertOrUpdate(projected.copy(updatedAt = System.currentTimeMillis()))
        val access = dao.all().filter { it.kind == "access" }.map { it.source }.toSet()
        val finishedStories = records.filter { it.kind == "story" }.map { it.source }.toSet()
        db.storyDao().getAllStoriesOnce().forEach {
            val updated = it.copy(isUnlocked = it.isUnlocked || "story:${it.id}" in access || it.requiredXp <= final.total,
                isCompleted = it.isCompleted || "story:${it.id}" in finishedStories)
            if(updated != it) db.storyDao().insert(updated)
        }
        return promotions
    }

    suspend fun mergeRewards(remote: List<RewardReceiptEntity>) = db.withTransaction {
        normalizeLocked()
        remote.forEach { row ->
            if(com.kasiguru.data.remote.RewardReceiptCodec.decode(com.kasiguru.data.remote.RewardReceiptCodec.encode(row)) != null) put(row)
        }
        settleLocked(imported = true)
    }

    suspend fun pin(familyId: String) = db.withTransaction {
        val family = BadgeCatalog.families.firstOrNull { it.id == familyId } ?: return@withTransaction
        if(db.achievementDao().getAllAchievementsOnce().none { BadgeCatalog.familyFor(it.id) == family && it.isUnlocked }) return@withTransaction
        val progress = db.userProgressDao().getUserProgressOnce() ?: return@withTransaction
        val pins = progress.pinnedBadgeIds.split(',').filter { it.isNotBlank() }.toMutableList()
        if(familyId in pins) pins.remove(familyId) else if(pins.size < 3) pins.add(familyId)
        db.userProgressDao().insertOrUpdate(progress.copy(pinnedBadgeIds = pins.joinToString(","),updatedAt = System.currentTimeMillis()))
    }

    suspend fun accessKeys(): Set<String> {
        ensureNormalized()
        return dao.all().filter { it.kind == "access" }.map { it.source }.toSet()
    }
    suspend fun importedLessons(): Map<String,Int> = dao.all().filter { it.kind == "lesson" && it.imported }.associate { it.source to it.xp }
    suspend fun preserveAccess(keys: Collection<String>) = db.withTransaction {
        keys.forEach { put(RewardReceiptEntity("access:$it","access",it,"",0,1,true)) }
    }
    suspend fun archiveLegacy(remote: UserProgressEntity) = db.withTransaction {
        normalizeLocked()
        val report = dao.normalization()!!
        if(remote.totalXp > report.originalXp) dao.putNormalization(report.copy(
            originalXp = remote.totalXp,originalLevel = remote.level,acknowledged = false))
        db.storyDao().getAllStoriesOnce().filter { it.requiredXp <= remote.totalXp }.forEach {
            put(RewardReceiptEntity("access:story:${it.id}","access","story:${it.id}","",0,1,true))
        }
    }
    private fun wordSource(text: String) = "word:" + text.trim().lowercase(java.util.Locale.ROOT)
}
