package com.kasiguru.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.kasiguru.data.local.dao.AchievementDao
import com.kasiguru.data.local.dao.GameLevelDao
import com.kasiguru.data.local.dao.UserProgressDao
import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.UserProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-device progress sync.
 *
 * - On sign-in: downloads the users/{uid}/progress documents, merges each with the
 *   local tables (always additively — see LearningStateMerge), and persists.
 * - Afterwards: observes the local tables (debounced) and uploads changes, so a
 *   second device or a reinstall can continue from the same place.
 *
 * Identity is the Firebase Auth uid. Linking an anonymous account to
 * email/Google keeps that uid, so progress survives the upgrade untouched; a
 * later sign-in on a new install re-attaches to the same documents.
 *
 * Passwords are never part of the cloud payload.
 */
@Singleton
class ProgressSyncManager @Inject constructor(
    private val userProgressDao: UserProgressDao,
    private val achievementDao: AchievementDao,
    private val gameLevelDao: GameLevelDao,
    private val vocabularyDao: VocabularyDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var lastUploaded: String? = null

    private val lastUploadedLearning = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Uploader jobs for the signed-in uid; cancelled when the account changes. */
    private var uploadJobs: List<Job> = emptyList()

    @Volatile
    private var activeUid: String? = null

    // Declared before init: addAuthStateListener can fire synchronously.
    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid ?: return@addAuthStateListener
            if (uid == activeUid) return@addAuthStateListener
            // The account changed (sign-in, link, sign-out): stop uploading to the
            // previous uid before touching local data, so nothing leaks across accounts.
            uploadJobs.forEach { it.cancel() }
            activeUid = uid
            lastUploaded = null
            lastUploadedLearning.clear()
            scope.launch {
                syncFromCloud(uid)
                syncLearningStateFromCloud(uid)
                uploadJobs = listOf(
                    launch { observeAndUpload(uid) },
                    launch { observeAndUploadAchievements(uid) },
                    launch { observeAndUploadGameLevels(uid) },
                    launch { observeAndUploadWordStates(uid) }
                )
            }
        }
    }

    private fun progressDoc(uid: String) =
        firestore.collection("users").document(uid)
            .collection("progress").document("main")

    private fun learningDoc(uid: String, name: String) =
        firestore.collection("users").document(uid)
            .collection("progress").document(name)

    /** Pulls remote progress, merges with local, persists, and reflects it back. */
    suspend fun syncFromCloud(uid: String) {
        val remote = runCatching {
            progressDoc(uid).get().await()
        }.getOrNull()?.data?.let { toEntity(it) }

        val local = userProgressDao.getUserProgressOnce()
        if (local == null) return
        val merged = if (remote != null) mergeProgress(local, remote) else local
        if (merged != local) {
            userProgressDao.insertOrUpdate(merged)
        }
        upload(uid, merged)
    }

    /** Watches local changes and pushes them (debounced, idempotent). */
    private suspend fun observeAndUpload(uid: String) {
        userProgressDao.getUserProgress()
            .debounce(5_000)
            .distinctUntilChanged()
            .collect { progress ->
                if (progress != null) upload(uid, progress)
            }
    }

    suspend fun upload(uid: String, progress: UserProgressEntity) {
        val key = progress.toString()
        if (key == lastUploaded) return
        runCatching {
            progressDoc(uid).set(toMap(progress)).await()
        }.onSuccess {
            lastUploaded = key
            publishLeaderboardEntry(uid, progress)
        }.onFailure { e ->
            Log.w(TAG, "upload FAILED", e)
        }
    }

    // ── Leaderboard (free-plan: client-published, rules-bounded) ────────────
    //
    // No Cloud Function is available on the Spark plan to aggregate rankings
    // server-side, so the client publishes its own row directly. firestore.rules'
    // isValidLeaderboardEntry() caps how much a single write can move totalXp,
    // which blocks casual tampering (not a determined attacker calling the
    // Firestore API directly — that needs XP computed server-side, i.e. Blaze).

    private fun leaderboardDoc(uid: String) = firestore.collection("leaderboard_public").document(uid)

    private suspend fun publishLeaderboardEntry(uid: String, progress: UserProgressEntity) {
        val currentWeekId = currentIsoWeekId()
        val existing = runCatching { leaderboardDoc(uid).get().await() }.getOrNull()
        val storedWeekId = existing?.getString("weekStartDate")
        val storedWeekStartXp = existing?.getLong("weekStartXp")?.toInt()

        // A new calendar week (or no prior entry): this write becomes the new
        // baseline, so weeklyXp starts back at 0 rather than carrying over.
        val (weekStartXp, weekStartDate) =
            if (storedWeekId == currentWeekId && storedWeekStartXp != null) {
                storedWeekStartXp to storedWeekId
            } else {
                progress.totalXp to currentWeekId
            }
        val weeklyXp = (progress.totalXp - weekStartXp).coerceAtLeast(0)

        val displayName = progress.fullName.ifBlank { progress.userName }.ifBlank { "Learner" }
        val payload = mapOf(
            "displayName" to displayName.take(40),
            "totalXp" to progress.totalXp,
            "level" to progress.level,
            "currentStreak" to progress.currentStreak,
            "profileIconId" to progress.profileIconId,
            "titleBadge" to progress.titleBadge,
            "weeklyXp" to weeklyXp,
            "weekStartXp" to weekStartXp,
            "weekStartDate" to weekStartDate,
            "updatedAt" to System.currentTimeMillis()
        )
        runCatching { leaderboardDoc(uid).set(payload).await() }
            .onFailure { e -> Log.w(TAG, "leaderboard publish FAILED", e) }
    }

    /** ISO calendar week id (e.g. "2026-W33"), used to roll the weekly board over. */
    private fun currentIsoWeekId(): String {
        val today = java.time.LocalDate.now()
        val fields = java.time.temporal.WeekFields.ISO
        val week = today.get(fields.weekOfWeekBasedYear())
        val year = today.get(fields.weekBasedYear())
        return "%d-W%02d".format(year, week)
    }

    // ── Achievements / game levels / word review state ──────────────────────
    //
    // Same shape as the main progress doc: pull, merge additively, persist the
    // result locally, then push the merged view back so the other side catches up.

    /** Pulls the three learning-state documents and merges them into Room. */
    suspend fun syncLearningStateFromCloud(uid: String) {
        mergeAchievementsFromCloud(uid)
        mergeGameLevelsFromCloud(uid)
        mergeWordStatesFromCloud(uid)
    }

    private suspend fun mergeAchievementsFromCloud(uid: String) {
        val remote = readMap(uid, DOC_ACHIEVEMENTS) { value ->
            AchievementState(
                isUnlocked = value["isUnlocked"] as? Boolean ?: false,
                currentValue = (value["currentValue"] as? Number)?.toInt() ?: 0,
                unlockedDate = value["unlockedDate"] as? String
            )
        } ?: return

        val localRows = achievementDao.getAllAchievementsOnce()
        val local = localRows.associate { it.id to AchievementState(it.isUnlocked, it.currentValue, it.unlockedDate) }
        val merged = mergeAchievements(local, remote)

        val updated = localRows.mapNotNull { row ->
            val state = merged[row.id] ?: return@mapNotNull null
            val next = row.copy(
                isUnlocked = state.isUnlocked,
                currentValue = state.currentValue,
                unlockedDate = state.unlockedDate
            )
            next.takeIf { it != row }
        }
        if (updated.isNotEmpty()) achievementDao.insertAll(updated)
        uploadAchievements(uid, merged)
    }

    private suspend fun mergeGameLevelsFromCloud(uid: String) {
        val remote = readMap(uid, DOC_GAME_LEVELS) { value ->
            GameLevelState(
                starsEarned = (value["starsEarned"] as? Number)?.toInt() ?: 0,
                isUnlocked = value["isUnlocked"] as? Boolean ?: false
            )
        } ?: return

        val localRows = gameLevelDao.getAllLevelsOnce()
        val local = localRows.associate { levelKey(it.gameType, it.levelNumber) to GameLevelState(it.starsEarned, it.isUnlocked) }
        val merged = mergeGameLevels(local, remote)

        val updated = localRows.mapNotNull { row ->
            val state = merged[levelKey(row.gameType, row.levelNumber)] ?: return@mapNotNull null
            row.copy(starsEarned = state.starsEarned, isUnlocked = state.isUnlocked).takeIf { it != row }
        }
        if (updated.isNotEmpty()) gameLevelDao.insertAll(updated)
        uploadGameLevels(uid, merged)
    }

    private suspend fun mergeWordStatesFromCloud(uid: String) {
        val remote = readMap(uid, DOC_WORD_STATES) { value ->
            WordState(
                isLearned = value["isLearned"] as? Boolean ?: false,
                timesReviewed = (value["timesReviewed"] as? Number)?.toInt() ?: 0,
                easinessFactor = (value["easinessFactor"] as? Number)?.toDouble() ?: 2.5,
                intervalDays = (value["intervalDays"] as? Number)?.toInt() ?: 0,
                nextReviewDate = value["nextReviewDate"] as? String ?: ""
            )
        } ?: return

        val localRows = vocabularyDao.getAllVocabularyOnce()
        val local = localRows.associate { wordKey(it.kasiguranin) to it.toWordState() }
        val merged = mergeWordStates(local, remote)

        val updated = localRows.mapNotNull { row ->
            val state = merged[wordKey(row.kasiguranin)] ?: return@mapNotNull null
            row.copy(
                isLearned = state.isLearned,
                timesReviewed = state.timesReviewed,
                easinessFactor = state.easinessFactor,
                intervalDays = state.intervalDays,
                nextReviewDate = state.nextReviewDate
            ).takeIf { it != row }
        }
        if (updated.isNotEmpty()) vocabularyDao.insertAll(updated)
        uploadWordStates(uid, merged)
    }

    private suspend fun observeAndUploadAchievements(uid: String) {
        achievementDao.getAllAchievementsForSync()
            .map { rows -> rows.associate { it.id to AchievementState(it.isUnlocked, it.currentValue, it.unlockedDate) } }
            .distinctUntilChanged()
            .debounce(DEBOUNCE_MS)
            .collect { uploadAchievements(uid, it) }
    }

    private suspend fun observeAndUploadGameLevels(uid: String) {
        gameLevelDao.getAllLevelsForSync()
            .map { rows -> rows.associate { levelKey(it.gameType, it.levelNumber) to GameLevelState(it.starsEarned, it.isUnlocked) } }
            .distinctUntilChanged()
            .debounce(DEBOUNCE_MS)
            .collect { uploadGameLevels(uid, it) }
    }

    private suspend fun observeAndUploadWordStates(uid: String) {
        vocabularyDao.getAllVocabulary()
            // Project to review state first: dictionary content updates must not
            // trigger an upload, only actual learning progress.
            .map { rows -> rows.associate { wordKey(it.kasiguranin) to it.toWordState() } }
            .distinctUntilChanged()
            .debounce(DEBOUNCE_MS)
            .collect { uploadWordStates(uid, it) }
    }

    private suspend fun uploadAchievements(uid: String, states: Map<String, AchievementState>) {
        val payload = states.mapValues {
            mapOf(
                "isUnlocked" to it.value.isUnlocked,
                "currentValue" to it.value.currentValue,
                "unlockedDate" to it.value.unlockedDate
            )
        }
        uploadLearningDoc(uid, DOC_ACHIEVEMENTS, payload)
    }

    private suspend fun uploadGameLevels(uid: String, states: Map<String, GameLevelState>) {
        val payload = states.mapValues {
            mapOf("starsEarned" to it.value.starsEarned, "isUnlocked" to it.value.isUnlocked)
        }
        uploadLearningDoc(uid, DOC_GAME_LEVELS, payload)
    }

    private suspend fun uploadWordStates(uid: String, states: Map<String, WordState>) {
        // Only words the user has actually touched: keeps the document small
        // instead of mirroring the whole 400+ word dictionary per user.
        val payload = states
            .filterValues { it.isLearned || it.timesReviewed > 0 }
            .mapValues {
                mapOf(
                    "isLearned" to it.value.isLearned,
                    "timesReviewed" to it.value.timesReviewed,
                    "easinessFactor" to it.value.easinessFactor,
                    "intervalDays" to it.value.intervalDays,
                    "nextReviewDate" to it.value.nextReviewDate
                )
            }
        uploadLearningDoc(uid, DOC_WORD_STATES, payload)
    }

    private suspend fun uploadLearningDoc(uid: String, name: String, payload: Map<String, Any?>) {
        if (payload.isEmpty()) return
        val key = payload.toString()
        if (lastUploadedLearning[name] == key) return
        runCatching {
            learningDoc(uid, name).set(mapOf("entries" to payload, "updatedAt" to System.currentTimeMillis())).await()
        }.onSuccess {
            lastUploadedLearning[name] = key
        }.onFailure { e ->
            Log.w(TAG, "upload $name FAILED", e)
        }
    }

    /**
     * Reads one learning-state document. Returns an empty map when the document
     * does not exist yet, and null only when the read itself failed — the caller
     * must not treat a failed read as "the cloud has nothing".
     */
    private suspend fun <T> readMap(
        uid: String,
        name: String,
        parse: (Map<String, Any?>) -> T
    ): Map<String, T>? {
        val snapshot = runCatching { learningDoc(uid, name).get().await() }.getOrElse {
            Log.w(TAG, "read $name FAILED", it)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        val entries = snapshot.data?.get("entries") as? Map<String, Map<String, Any?>> ?: return emptyMap()
        return entries.mapValues { parse(it.value) }
    }

    private companion object {
        const val TAG = "ProgressSync"
        const val DEBOUNCE_MS = 5_000L
        const val DOC_ACHIEVEMENTS = "achievements"
        const val DOC_GAME_LEVELS = "gameLevels"
        const val DOC_WORD_STATES = "wordStates"

        fun levelKey(gameType: String, levelNumber: Int) = "${gameType}_$levelNumber"

        /**
         * Words are keyed by their Kasiguranin text, not the Room row id: ids are
         * locally auto-generated and differ between installs. This matches how
         * FirestoreSyncManager maps dictionary content onto local rows.
         */
        fun wordKey(kasiguranin: String) = kasiguranin.lowercase()
    }

    private fun toMap(p: UserProgressEntity): Map<String, Any?> = mapOf(
        "id" to p.id,
        "userName" to p.userName,
        "email" to p.email,
        "fullName" to p.fullName,
        "age" to p.age,
        "address" to p.address,
        "profileIconId" to p.profileIconId,
        "totalXp" to p.totalXp,
        "level" to p.level,
        "currentStreak" to p.currentStreak,
        "longestStreak" to p.longestStreak,
        "lastActiveDate" to p.lastActiveDate,
        "wordsLearned" to p.wordsLearned,
        "storiesCompleted" to p.storiesCompleted,
        "gamesPlayed" to p.gamesPlayed,
        "totalCorrectAnswers" to p.totalCorrectAnswers,
        "totalQuestionsAnswered" to p.totalQuestionsAnswered,
        "lessonsCompleted" to p.lessonsCompleted,
        "isOnboardingCompleted" to p.isOnboardingCompleted,
        "dailyGoalXp" to p.dailyGoalXp,
        "titleBadge" to p.titleBadge,
        "updatedAt" to System.currentTimeMillis()
        // password intentionally omitted
    )

    private fun toEntity(data: Map<String, Any?>): UserProgressEntity = UserProgressEntity(
        id = (data["id"] as? Number)?.toInt() ?: 1,
        userName = data["userName"] as? String ?: "",
        email = data["email"] as? String ?: "",
        fullName = data["fullName"] as? String ?: "",
        age = (data["age"] as? Number)?.toInt(),
        address = data["address"] as? String ?: "",
        profileIconId = (data["profileIconId"] as? Number)?.toInt() ?: 1,
        totalXp = (data["totalXp"] as? Number)?.toInt() ?: 0,
        level = (data["level"] as? Number)?.toInt() ?: 1,
        currentStreak = (data["currentStreak"] as? Number)?.toInt() ?: 0,
        longestStreak = (data["longestStreak"] as? Number)?.toInt() ?: 0,
        lastActiveDate = data["lastActiveDate"] as? String ?: "",
        wordsLearned = (data["wordsLearned"] as? Number)?.toInt() ?: 0,
        storiesCompleted = (data["storiesCompleted"] as? Number)?.toInt() ?: 0,
        gamesPlayed = (data["gamesPlayed"] as? Number)?.toInt() ?: 0,
        totalCorrectAnswers = (data["totalCorrectAnswers"] as? Number)?.toInt() ?: 0,
        totalQuestionsAnswered = (data["totalQuestionsAnswered"] as? Number)?.toInt() ?: 0,
        lessonsCompleted = (data["lessonsCompleted"] as? Number)?.toInt() ?: 0,
        isOnboardingCompleted = data["isOnboardingCompleted"] as? Boolean ?: false,
        dailyGoalXp = (data["dailyGoalXp"] as? Number)?.toInt() ?: 100,
        titleBadge = data["titleBadge"] as? String ?: "Kasiguranin Apprentice",
        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
    )
}

/**
 * Merges local and remote progress: counters take the max, profile fields come
 * from the side with the newer [UserProgressEntity.updatedAt]. Pure function
 * (unit-tested). Passwords/emails are never part of the cloud payload.
 */
internal fun mergeProgress(
    local: UserProgressEntity,
    remote: UserProgressEntity
): UserProgressEntity {
    val remoteNewer = remote.updatedAt >= local.updatedAt
    return UserProgressEntity(
        id = 1,
        userName = pick(local.userName, remote.userName, remoteNewer),
        password = local.password,
        email = local.email,
        fullName = pick(local.fullName, remote.fullName, remoteNewer),
        age = if (remoteNewer) remote.age ?: local.age else local.age ?: remote.age,
        address = pick(local.address, remote.address, remoteNewer),
        profileIconId = if (remoteNewer) remote.profileIconId else local.profileIconId,
        totalXp = maxOf(local.totalXp, remote.totalXp),
        level = maxOf(local.level, remote.level),
        currentStreak = maxOf(local.currentStreak, remote.currentStreak),
        longestStreak = maxOf(local.longestStreak, remote.longestStreak),
        lastActiveDate = maxOf(local.lastActiveDate, remote.lastActiveDate),
        wordsLearned = maxOf(local.wordsLearned, remote.wordsLearned),
        storiesCompleted = maxOf(local.storiesCompleted, remote.storiesCompleted),
        gamesPlayed = maxOf(local.gamesPlayed, remote.gamesPlayed),
        totalCorrectAnswers = maxOf(local.totalCorrectAnswers, remote.totalCorrectAnswers),
        totalQuestionsAnswered = maxOf(local.totalQuestionsAnswered, remote.totalQuestionsAnswered),
        lessonsCompleted = maxOf(local.lessonsCompleted, remote.lessonsCompleted),
        isOnboardingCompleted = local.isOnboardingCompleted || remote.isOnboardingCompleted,
        dailyGoalXp = if (remoteNewer) remote.dailyGoalXp else local.dailyGoalXp,
        titleBadge = pick(local.titleBadge, remote.titleBadge, remoteNewer),
        updatedAt = maxOf(local.updatedAt, remote.updatedAt)
    )
}

private fun pick(local: String, remote: String, remoteNewer: Boolean): String =
    if (remoteNewer) remote.ifBlank { local } else local.ifBlank { remote }

private fun com.kasiguru.data.local.entity.VocabularyEntity.toWordState() = WordState(
    isLearned = isLearned,
    timesReviewed = timesReviewed,
    easinessFactor = easinessFactor,
    intervalDays = intervalDays,
    nextReviewDate = nextReviewDate
)
