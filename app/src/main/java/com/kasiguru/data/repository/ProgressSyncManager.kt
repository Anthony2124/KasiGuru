package com.kasiguru.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.local.dao.UserProgressDao
import com.kasiguru.data.local.entity.UserProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-device progress sync (Phase 6).
 *
 * - On sign-in: downloads users/{uid}/progress/main, merges it with the local
 *   row (counters take the max, profile fields from the newer side), and
 *   persists the merged state.
 * - Afterwards: observes local user_progress changes (debounced) and uploads
 *   the merged state back to the cloud, so a second device can continue.
 *
 * Notes:
 * - Passwords/emails are NEVER synced to the cloud.
 * - Identity is the anonymous Firebase Auth uid; real multi-device identity
 *   still requires email/password accounts (future work).
 */
@Singleton
class ProgressSyncManager @Inject constructor(
    private val userProgressDao: UserProgressDao,
    private val firestore: FirebaseFirestore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var lastUploaded: String? = null

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid ?: return@addAuthStateListener
            scope.launch {
                syncFromCloud(uid)
                observeAndUpload(uid)
            }
        }
    }

    private fun progressDoc(uid: String) =
        firestore.collection("users").document(uid)
            .collection("progress").document("main")

    /** Pulls remote progress, merges with local, persists, and reflects it back. */
    suspend fun syncFromCloud(uid: String) {
        val remote = runCatching {
            progressDoc(uid).get().await()
        }.getOrNull()?.data?.let { toEntity(it) }

        val local = userProgressDao.getUserProgressOnce() ?: return
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
        }
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
