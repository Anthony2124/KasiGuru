package com.kasiguru.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.kasiguru.data.local.dao.AchievementDao
import com.kasiguru.data.local.dao.GameLevelDao
import com.kasiguru.data.local.dao.LessonDao
import com.kasiguru.data.local.dao.UserProgressDao
import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.LessonProgressEntity
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.data.local.KasiGuruDatabase
import com.kasiguru.data.local.entity.RewardReceiptEntity
import com.kasiguru.data.remote.RewardReceiptCodec
import com.kasiguru.domain.gamification.BadgeCatalog
import com.kasiguru.domain.gamification.RewardLedger
import com.kasiguru.domain.gamification.XpPolicy
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Source
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import com.kasiguru.data.remote.model.PublicProfileDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val lessonDao: LessonDao,
    private val vocabularyDao: VocabularyDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val db: KasiGuruDatabase,
    private val gamification: GamificationRepository,
    private val lessons: dagger.Lazy<LessonRepository>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Both caches are keyed by uid, not by document alone. Keyed by document alone, the
    // previous account's still-running sync could mark a payload as "already uploaded" and
    // make the new account's identical payload be skipped entirely - the anonymous session
    // that signOutToAnonymous() creates between a sign-out and the next sign-in did exactly
    // that, so the account signed in afterwards never had its own document written.
    @Volatile
    private var lastUploaded: Pair<String, String>? = null

    private val lastUploadedLearning = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Uploader jobs for the signed-in uid; cancelled when the account changes. */
    private var uploadJobs: List<Job> = emptyList()

    /**
     * The initial download-and-merge for the signed-in uid.
     *
     * Tracked, and cancelled on an account change, because it outlives the listener callback
     * that starts it: it is what assigns [uploadJobs], so a stale one belonging to the previous
     * account would overwrite the new account's uploader list - keeping the old observers alive
     * and leaving the new ones untracked and uncancellable.
     */
    private var sessionJob: Job? = null

    @Volatile
    private var activeUid: String? = null
    @Volatile private var readyForNormalizedUpload = false
    private var legacyCloud = false
    private val rewardUploadMutex = Mutex()
    private val uploadedRewards = java.util.concurrent.ConcurrentHashMap<String,RewardReceiptEntity>()
    private var rewardCursor: com.google.firebase.Timestamp? = null

    /** True between [beginAccountSwitch] and [finishAccountSwitch]: uid changes are not synced. */
    @Volatile private var switchingAccount = false
    /** The next main-progress pull takes the cloud row as-is instead of merging this device into it. */
    @Volatile private var adoptCloudProgress = false

    // Declared before init: addAuthStateListener can fire synchronously.
    init {
        auth.addAuthStateListener { firebaseAuth ->
            if (switchingAccount) return@addAuthStateListener
            val uid = firebaseAuth.currentUser?.uid
            if(uid == null) onUserSignedOut() else if(uid != activeUid) startSync(uid)
        }
    }

    fun restartSync() { auth.currentUser?.uid?.let(::startSync) }

    /**
     * Signing in to an *existing* account from a guest session. The account is the one that
     * survives: nothing from this device's guest may be merged into it or uploaded over it. Sync
     * stops here, before the credential is used, because the auth listener would otherwise start
     * merging the guest's rows into the account the instant the uid changes.
     */
    fun beginAccountSwitch() {
        switchingAccount = true
        onUserSignedOut()
    }

    /**
     * @param signedIn true once the sign-in succeeded and the caller has wiped the guest's local
     *   data: the account's own cloud copy is then pulled down untouched. False puts the guest
     *   session that is still signed in back to syncing as before.
     */
    fun finishAccountSwitch(signedIn: Boolean) {
        adoptCloudProgress = signedIn
        switchingAccount = false
        restartSync()
    }

    private fun startSync(uid: String) {
        // The account changed (sign-in, link, sign-out): stop uploading to the
        // previous uid before touching local data, so nothing leaks across accounts.
        sessionJob?.cancel()
        uploadJobs.forEach { it.cancel() }
        activeUid = uid
        lastUploaded = null
        lastUploadedLearning.clear()
        readyForNormalizedUpload = false
        uploadedRewards.clear()
        rewardCursor = null
        sessionJob = scope.launch {
            // Backs off rather than retrying every 5 s forever: when the project's daily read quota
            // is spent, every attempt fails until the reset, and a tight loop only burns battery.
            var wait = 5_000L
            while(activeUid == uid) {
                if(syncFromCloud(uid) && syncRewardsFromCloud(uid) && syncLearningStateFromCloud(uid)) break
                delay(wait)
                wait = (wait * 2).coerceAtMost(5 * 60_000L)
            }
            if(activeUid != uid) return@launch
            if(legacyCloud) gamification.importLegacyLearningState()
            readyForNormalizedUpload = true
            uploadJobs = listOf(
                launch { observeAndUploadRewards(uid) },
                launch { observeAndUpload(uid) },
                launch { observeAndUploadAchievements(uid) },
                launch { observeAndUploadGameLevels(uid) },
                launch { observeAndUploadLessonProgress(uid) },
                launch { observeAndUploadWordStates(uid) }
            )
        }
    }

    /**
     * Explicitly cleans up sync state when the user signs out.
     */
    fun onUserSignedOut() {
        sessionJob?.cancel()
        sessionJob = null
        uploadJobs.forEach { it.cancel() }
        uploadJobs = emptyList()
        activeUid = null
        lastUploaded = null
        lastUploadedLearning.clear()
        readyForNormalizedUpload = false
        uploadedRewards.clear()
        rewardCursor = null
    }

    /**
     * Pushes everything currently in Room to the signed-in account's documents and waits for
     * it to land.
     *
     * Signing out wipes the local tables (see [UserDataResetManager]), and the observers that
     * would otherwise have carried the last few minutes of play are debounced by [DEBOUNCE_MS].
     * Anything done inside that window - finishing the third mini-game, clearing the day's
     * review - was therefore destroyed locally without ever reaching the cloud, and signing
     * back in restored the state from before it. Uploading synchronously here, before any local
     * row is touched, closes that window.
     *
     * A no-op when nobody is signed in, which is the case on the account-deletion path: the
     * cloud documents are already gone there and must not be recreated.
     */
    suspend fun flushToCloud() {
        val uid = auth.currentUser?.uid ?: return
        check(syncFromCloud(uid) && syncRewardsFromCloud(uid) && syncLearningStateFromCloud(uid)) { "Connect to the internet before signing out so your progress can be saved." }
        if(legacyCloud) gamification.importLegacyLearningState()
        readyForNormalizedUpload = true
        check(uploadRewards(uid)) { "Your rewards could not be saved. Please try again." }
        userProgressDao.getUserProgressOnce()?.let { upload(uid, it) }

        uploadAchievements(
            uid,
            achievementDao.getAllAchievementsOnce()
                .associate { it.id to AchievementState(it.isUnlocked, it.currentValue, it.unlockedDate) }
        )
        uploadGameLevels(
            uid,
            gameLevelDao.getAllLevelsOnce()
                .associate { levelKey(it.gameType, it.levelNumber) to GameLevelState(it.starsEarned, it.isUnlocked) }
        )
        uploadLessonProgress(
            uid,
            lessonDao.getAllOnce().associate {
                lessonKey(it.unitId, it.lessonIndex) to
                    LessonState(it.isComplete, it.bestAccuracy, it.timesCompleted, it.lastCompletedAt)
            }
        )
        uploadWordStates(
            uid,
            vocabularyDao.getAllVocabularyOnce().associate { wordKey(it.kasiguranin) to it.toWordState() }
        )
    }

    private fun progressDoc(uid: String) =
        firestore.collection("users").document(uid)
            .collection("progress").document("main")

    private fun learningDoc(uid: String, name: String) =
        firestore.collection("users").document(uid)
            .collection("progress").document(name)

    /** Pulls remote progress, merges with local, persists, and reflects it back. */
    suspend fun syncFromCloud(uid: String): Boolean {
        val snapshot = runCatching { progressDoc(uid).get(Source.SERVER).await() }.getOrElse {
            Log.w(TAG,"Read main progress failed; waiting before upload",it)
            return false
        }
        if(activeUid != uid || auth.currentUser?.uid != uid) return false
        val remote = snapshot.data?.let(::toEntity)
        legacyCloud = remote != null && remote.xpPolicyVersion < XpPolicy.VERSION
        val adopt = adoptCloudProgress
        db.withTransaction {
            val local = userProgressDao.getUserProgressOnce() ?: UserProgressEntity()
            // After an account switch the local row is a blank reseed, and its defaults (the
            // placeholder name, avatar 0) must not be weighed against the account's real values.
            val merged = when {
                remote == null -> local
                adopt -> remote.copy(id = 1)
                else -> mergeProgress(local, remote)
            }
            val updated = withAccountIdentity(merged)
            if(updated != local) userProgressDao.insertOrUpdate(updated)
        }
        if (adopt) adoptCloudProgress = false
        gamification.ensureNormalized()
        if(legacyCloud && remote != null) gamification.archiveLegacy(remote)
        return true
    }

    /**
     * Called after a guest links email or Google. The uid does not change on a link, so the
     * auth listener above never re-runs [syncFromCloud], and the new email never reached the
     * profile or the leaderboard row. This does that part, then uploads straight away so
     * the account appears on the leaderboard without waiting for the next lesson.
     */
    fun onAccountLinked() {
        val uid = auth.currentUser?.uid ?: return
        scope.launch {
            val local = userProgressDao.getUserProgressOnce() ?: return@launch
            val updated = withAccountIdentity(local)
            if (updated != local) userProgressDao.insertOrUpdate(updated)
            upload(uid, updated)
        }
    }

    /** Fills a blank email or name from the signed-in account. Never overwrites what's there. */
    private fun withAccountIdentity(progress: UserProgressEntity): UserProgressEntity {
        val authEmail = auth.currentUser?.accountEmail().orEmpty()
        val authName = auth.currentUser?.displayName.orEmpty()
        return progress.copy(
            email = if (progress.email.isBlank() && authEmail.isNotBlank()) authEmail else progress.email,
            fullName = if (progress.fullName.isBlank() && authName.isNotBlank()) authName else progress.fullName
        )
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
        if(auth.currentUser?.uid != uid || activeUid != uid) return
        if(progress.xpPolicyVersion >= XpPolicy.VERSION && !readyForNormalizedUpload) return
        if(!uploadRewards(uid)) return
        val projected = userProgressDao.getUserProgressOnce() ?: return
        if(auth.currentUser?.uid != uid || activeUid != uid) return
        val key = uid to projected.toString()
        if (key == lastUploaded) return
        runCatching {
            progressDoc(uid).set(toMap(projected)).await()
        }.onSuccess {
            lastUploaded = key
            publishLeaderboardEntry(uid, projected)
        }.onFailure { e ->
            // Reaching here means the document is NOT in the cloud, and nothing above this
            // notices. firestore.rules' isValidMainProgress() rejects the whole write when
            // any field is unlisted or exceeds its sanity bound (the schema is strict), so a
            // schema mismatch that slips past the Kotlin models stops syncing permanently
            // without a single exception escaping back to the UI.
            Log.w(TAG, "upload FAILED (rule rejection?): check firestore.rules", e)
        }
    }

    // ── Leaderboard entry (Phase 5) ─────────────────────────────────────────
    //
    // Published from here on every successful upload. The Firestore rule on
    // leaderboard_public enforces the per-write +2000 XP cap so a compromised client
    // cannot mint rank #1 in one shot (same bound isValidProgressDoc enforces on the
    // progress document itself). anti-cheat still cannot prevent forging calls to the
    // Firestore API directly — that needs XP computed server-side, i.e. Blaze).

    private fun leaderboardDoc(uid: String) = firestore.collection("leaderboard_public").document(uid)

    private suspend fun publishLeaderboardEntry(uid: String, progress: UserProgressEntity) {
        // Only players with a real account are ranked. A guest's progress still syncs, but it
        // gets no leaderboard row, and any row it published before this rule is withdrawn.
        // Linking an account keeps the same uid, so the row appears on the next sync after
        // signing up. firestore.rules enforces the same thing on the server.
        if (auth.currentUser?.isAnonymous != false) {
            runCatching { firestore.collection("public_profiles").document(uid).delete().await() }
            runCatching { leaderboardDoc(uid).delete().await() }
                .onFailure { e -> Log.w(TAG, "guest leaderboard row removal FAILED", e) }
            return
        }

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
        val weekStart = java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString()
        val weeklyXp = RewardLedger.totals(db.rewardDao().all().map { it.record() })
            .byDay.filterKeys { it >= weekStart && it <= java.time.LocalDate.now().toString() }.values.sum()

        val displayName = PublicProfileDto.displayName(progress.userName, progress.fullName)
        val isAnonymous = auth.currentUser?.isAnonymous == true
        val creationTime = auth.currentUser?.metadata?.creationTimestamp ?: System.currentTimeMillis()
        val payload = mapOf(
            "displayName" to displayName.take(40),
            "isAnonymous" to isAnonymous,
            "createdAt" to creationTime,
            "registeredAt" to creationTime,
            "totalXp" to progress.totalXp,
            "level" to progress.level,
            "currentStreak" to progress.currentStreak,
            "profileIconId" to progress.profileIconId,
            "titleBadge" to progress.titleBadge,
            "weeklyXp" to weeklyXp,
            "weekStartXp" to weekStartXp,
            "weekStartDate" to weekStartDate,
            "xpPolicyVersion" to progress.xpPolicyVersion,
            "activityXp" to progress.activityXp,
            "updatedAt" to System.currentTimeMillis()
        )
        runCatching { leaderboardDoc(uid).set(payload).await() }
            .onFailure { e -> Log.w(TAG, "leaderboard publish FAILED", e) }
        // A separate allowlisted projection: never serialize the private progress entity here.
        runCatching {
            if (auth.currentUser?.uid != uid || auth.currentUser?.isAnonymous != false) return@runCatching
            val tree = lessons.get().treeSections()
            val profile = PublicProfileDto(
                displayName = displayName, profileIconId = progress.profileIconId,
                profileBackgroundId = com.kasiguru.domain.gamification.ProfileBackgroundCatalog.normalize(progress.profileBackgroundId),
                level = progress.level, totalXp = progress.totalXp, currentStreak = progress.currentStreak,
                wordsLearned = vocabularyDao.getLearnedCount().first(),
                lessonsCompleted = lessonDao.getAllOnce().count { it.isComplete && !it.unitId.startsWith("mastery:") },
                weeklyXp = weeklyXp, weekId = currentWeekId, createdAt = creationTime, updatedAt = System.currentTimeMillis(),
                badgeIds = achievementDao.getAllAchievementsOnce().filter { it.isUnlocked && BadgeCatalog.familyFor(it.id) != null }.map { it.id },
                sections = tree.associate { section -> section.id to section.nodes.count {
                    !it.isDeepDive && it.node is com.kasiguru.domain.lesson.TreeNode.Lesson && it.mastery >= com.kasiguru.domain.lesson.Mastery.FAMILIAR
                } },
                sectionTotals = tree.associate { it.id to it.coreLessonNodeCount },
                masteredSections = tree.filter { section -> section.nodes.any {
                    it.node is com.kasiguru.domain.lesson.TreeNode.MasteryTest && it.mastery >= com.kasiguru.domain.lesson.Mastery.FAMILIAR
                } }.map { it.id },
                unlockedSections = tree.filter { it.isUnlocked }.map { it.id }
            )
            firestore.collection("public_profiles").document(uid).set(profile.toMap()).await()
        }.onFailure { e -> Log.w(TAG, "public profile publish FAILED", e) }

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

    /** Pulls the learning-state documents and merges them into Room. */
    suspend fun syncLearningStateFromCloud(uid: String): Boolean = runCatching {
        mergeAchievementsFromCloud(uid)
        mergeGameLevelsFromCloud(uid)
        mergeLessonProgressFromCloud(uid)
        mergeWordStatesFromCloud(uid)
        true
    }.getOrElse { Log.w(TAG,"Learning history download failed",it); false }

    suspend fun syncNow(uid: String): Boolean {
        if(!syncFromCloud(uid) || !syncRewardsFromCloud(uid) || !syncLearningStateFromCloud(uid)) return false
        if(legacyCloud) gamification.importLegacyLearningState()
        readyForNormalizedUpload = true
        upload(uid,userProgressDao.getUserProgressOnce() ?: return false)
        return true
    }

    private suspend fun mergeAchievementsFromCloud(uid: String) {
        val remote = readMap(uid, DOC_ACHIEVEMENTS) { value ->
            AchievementState(
                isUnlocked = value["isUnlocked"] as? Boolean ?: false,
                currentValue = (value["currentValue"] as? Number)?.toInt() ?: 0,
                unlockedDate = value["unlockedDate"] as? String
            )
        } ?: return

        val localRows = achievementDao.getAllAchievementsOnce().filter { !it.id.startsWith(BadgeCatalog.PREFIX) }
        val local = localRows.associate { it.id to AchievementState(it.isUnlocked, it.currentValue, it.unlockedDate) }
        val merged = mergeAchievements(local, remote)

        val localById = localRows.associateBy { it.id }
        val updated = mergeLegacyAchievementRows(localRows, remote)
            .filter { it != localById[it.id] }
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

    private suspend fun mergeLessonProgressFromCloud(uid: String) {
        val remote = readMap(uid, DOC_LESSON_PROGRESS) { value ->
            LessonState(
                isComplete = value["isComplete"] as? Boolean ?: false,
                bestAccuracy = (value["bestAccuracy"] as? Number)?.toFloat() ?: 0f,
                timesCompleted = (value["timesCompleted"] as? Number)?.toInt() ?: 0,
                lastCompletedAt = (value["lastCompletedAt"] as? Number)?.toLong() ?: 0
            )
        } ?: return

        val localByKey = lessonDao.getAllOnce().associateBy { lessonKey(it.unitId, it.lessonIndex) }
        val local = localByKey.mapValues {
            LessonState(it.value.isComplete, it.value.bestAccuracy, it.value.timesCompleted, it.value.lastCompletedAt)
        }
        val merged = mergeLessonProgress(local, remote)

        // Unlike achievements, game levels and words, a lesson has no row until it is first played,
        // so the cloud routinely carries keys this install has no row for. Rebuild the row from its
        // key instead of only patching rows that already exist, or a reinstall silently drops them.
        val updated = merged.mapNotNull { (key, state) ->
            val row = lessonRowFrom(key, state) ?: return@mapNotNull null
            row.takeIf { it != localByKey[key] }
        }
        if (updated.isNotEmpty()) lessonDao.upsertAll(updated)
        uploadLessonProgress(uid, merged)
    }

    private suspend fun mergeWordStatesFromCloud(uid: String) {
        val remote = readMap(uid, DOC_WORD_STATES) { value ->
            WordState(
                isLearned = value["isLearned"] as? Boolean ?: false,
                timesReviewed = (value["timesReviewed"] as? Number)?.toInt() ?: 0,
                easinessFactor = (value["easinessFactor"] as? Number)?.toDouble() ?: 2.5,
                intervalDays = (value["intervalDays"] as? Number)?.toInt() ?: 0,
                nextReviewDate = value["nextReviewDate"] as? String ?: "",
                lapses = (value["lapses"] as? Number)?.toInt() ?: 0,
                relearningStep = (value["relearningStep"] as? Number)?.toInt() ?: 0
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
                nextReviewDate = state.nextReviewDate,
                lapses = state.lapses,
                relearningStep = state.relearningStep
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

    private suspend fun observeAndUploadLessonProgress(uid: String) {
        lessonDao.observeAll()
            .map { rows ->
                rows.associate {
                    lessonKey(it.unitId, it.lessonIndex) to
                        LessonState(it.isComplete, it.bestAccuracy, it.timesCompleted, it.lastCompletedAt)
                }
            }
            .distinctUntilChanged()
            .debounce(DEBOUNCE_MS)
            .collect { uploadLessonProgress(uid, it) }
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

    private suspend fun uploadLessonProgress(uid: String, states: Map<String, LessonState>) {
        val payload = states.mapValues {
            mapOf(
                "isComplete" to it.value.isComplete,
                "bestAccuracy" to it.value.bestAccuracy,
                "timesCompleted" to it.value.timesCompleted,
                "lastCompletedAt" to it.value.lastCompletedAt
            )
        }
        uploadLearningDoc(uid, DOC_LESSON_PROGRESS, payload)
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
                    "nextReviewDate" to it.value.nextReviewDate,
                    "lapses" to it.value.lapses,
                    "relearningStep" to it.value.relearningStep
                )
            }
        uploadLearningDoc(uid, DOC_WORD_STATES, payload)
    }

    private suspend fun uploadLearningDoc(uid: String, name: String, payload: Map<String, Any?>) {
        if (payload.isEmpty()) return
        val cacheKey = uid + "/" + name
        val key = payload.toString()
        if (lastUploadedLearning[cacheKey] == key) return
        runCatching {
            learningDoc(uid, name).set(mapOf("entries" to payload, "updatedAt" to System.currentTimeMillis())).await()
        }.onSuccess {
            lastUploadedLearning[cacheKey] = key
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
            throw it
        }
        @Suppress("UNCHECKED_CAST")
        val entries = snapshot.data?.get("entries") as? Map<String, Map<String, Any?>> ?: return emptyMap()
        return entries.mapValues { parse(it.value) }
    }

    private fun rewardCollection(uid: String) = firestore.collection("users").document(uid)
        .collection(RewardReceiptCodec.COLLECTION)

    private suspend fun syncRewardsFromCloud(uid: String): Boolean = runCatching {
        val cursor = rewardCursor
        var newest = cursor
        var last: com.google.firebase.firestore.DocumentSnapshot? = null
        do {
            var query = if(cursor == null) rewardCollection(uid).orderBy(FieldPath.documentId()).limit(400)
                else rewardCollection(uid).whereGreaterThanOrEqualTo("updatedAt",cursor)
                    .orderBy("updatedAt").orderBy(FieldPath.documentId()).limit(400)
            last?.let { query = query.startAfter(it) }
            val page = query.get(Source.SERVER).await()
            if(activeUid != uid || auth.currentUser?.uid != uid) return false
            val rows = page.documents.mapNotNull { doc ->
                doc.data?.let(RewardReceiptCodec::decode)?.takeIf { RewardReceiptCodec.documentId(it.id) == doc.id }
            }
            gamification.mergeRewards(rows)
            page.documents.mapNotNull { it.getTimestamp("updatedAt") }.maxOrNull()?.let {
                if(newest == null || it > newest!!) newest = it
            }
            last = page.documents.lastOrNull()
        } while(page.size() == 400)
        // A first scan is ordered by document ID; a concurrent insert may fall before its cursor.
        // Follow it with a timestamp scan from epoch so that insert cannot be skipped.
        rewardCursor = if(cursor == null) com.google.firebase.Timestamp(0,0) else newest
        true
    }.getOrElse { Log.w(TAG,"Reward download failed; keeping local rewards",it); false }

    private suspend fun observeAndUploadRewards(uid: String) {
        db.rewardDao().observe().distinctUntilChanged().debounce(DEBOUNCE_MS).collect { _ ->
            if(uploadRewards(uid)) userProgressDao.getUserProgressOnce()?.let { upload(uid,it) }
        }
    }

    /** Reads each remote receipt before writing, so concurrent devices cannot erase stronger evidence. */
    private suspend fun uploadRewards(uid: String): Boolean = rewardUploadMutex.withLock {
        if(activeUid != uid || auth.currentUser?.uid != uid || !readyForNormalizedUpload) return@withLock false
        if(!syncRewardsFromCloud(uid)) return@withLock false
        val changed = db.rewardDao().all().filter { uploadedRewards[uid + "/" + it.id] != it }
        runCatching {
            changed.chunked(200).forEach { chunk ->
                val merged = firestore.runTransaction { tx ->
                    val results = chunk.map { row ->
                        val ref = rewardCollection(uid).document(RewardReceiptCodec.documentId(row.id))
                        val old = tx.get(ref).data?.let(RewardReceiptCodec::decode)
                        val record = if(old == null) row.record() else RewardLedger.merge(row.record(),old.record())
                        ref to row.copy(day = record.day,xp = record.xp,value = record.value,imported = record.imported)
                    }
                    results.forEach { (ref,row) -> tx.set(ref,RewardReceiptCodec.encode(row) +
                        ("updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())) }
                    results.map { it.second }
                }.await()
                if(activeUid != uid || auth.currentUser?.uid != uid) return@withLock false
                gamification.mergeRewards(merged)
                merged.forEach { uploadedRewards[uid + "/" + it.id] = it }
            }
            true
        }.getOrElse { Log.w(TAG,"Reward upload failed",it); false }
    }

    private companion object {
        const val TAG = "ProgressSync"
        const val DEBOUNCE_MS = 5_000L
        const val DOC_ACHIEVEMENTS = ProgressDocuments.ACHIEVEMENTS
        const val DOC_GAME_LEVELS = ProgressDocuments.GAME_LEVELS
        const val DOC_LESSON_PROGRESS = ProgressDocuments.LESSON_PROGRESS
        const val DOC_WORD_STATES = ProgressDocuments.WORD_STATES

        fun levelKey(gameType: String, levelNumber: Int) = "${gameType}_$levelNumber"

        /**
         * A lesson is identified by its unit (a category name, which can contain spaces and
         * underscores) plus its index, so the separator has to be one a category never uses.
         */
        private const val LESSON_KEY_SEPARATOR = '#'

        fun lessonKey(unitId: String, lessonIndex: Int) = "$unitId$LESSON_KEY_SEPARATOR$lessonIndex"

        fun lessonRowFrom(key: String, state: LessonState): LessonProgressEntity? {
            val unitId = key.substringBeforeLast(LESSON_KEY_SEPARATOR, "")
            val lessonIndex = key.substringAfterLast(LESSON_KEY_SEPARATOR).toIntOrNull()
            if (unitId.isEmpty() || lessonIndex == null) return null
            return LessonProgressEntity(
                unitId = unitId,
                lessonIndex = lessonIndex,
                isComplete = state.isComplete,
                bestAccuracy = state.bestAccuracy,
                timesCompleted = state.timesCompleted,
                lastCompletedAt = state.lastCompletedAt
            )
        }

        /**
         * Words are keyed by their Kasiguranin text, not the Room row id: ids are
         * locally auto-generated and differ between installs. This matches how
         * FirestoreSyncManager maps dictionary content onto local rows.
         */
        fun wordKey(kasiguranin: String) = kasiguranin.lowercase()
    }

}

/**
 * The Firestore payload for a progress document. Top-level and `internal` (not a private
 * class member) specifically so [ProgressSyncFieldParityTest] can call it directly without
 * mocking every one of ProgressSyncManager's constructor dependencies just to reach a pure
 * mapping function that never touches any of them.
 */
internal fun toMap(p: UserProgressEntity): Map<String, Any?> = mapOf(
    "id" to p.id,
    "userName" to p.userName,
    "email" to p.email,
    "fullName" to p.fullName,
    "age" to p.age,
    "address" to p.address,
    "profileIconId" to p.profileIconId,
    "profileBackgroundId" to p.profileBackgroundId,
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
    // The daily-XP ledger must travel with the rest of progress. Any field written locally but
    // missing from this map is silently reset to its default the next time a remote document is
    // applied, because toEntity rebuilds the whole entity rather than patching it.
    "dailyXpEarned" to p.dailyXpEarned,
    "dailyXpDate" to p.dailyXpDate,
    "titleBadge" to p.titleBadge,
    // Lifetime contribution counter behind the "First Contribution" badge
    // kept for contribution statistics. It was
    // missing here, so it never reached the cloud and toEntity rebuilt it as 0 — a second
    // device, or a reinstall restoring from cloud, silently wiped the user's submission count
    // and the badge progress resting on it.
    "submissionsMade" to p.submissionsMade,
    "dailyReviewCompletedDate" to p.dailyReviewCompletedDate,
    "dailyGamesDate" to p.dailyGamesDate,
    "dailyGamesPlayedCount" to p.dailyGamesPlayedCount,
    "xpPolicyVersion" to p.xpPolicyVersion,
    "activityXp" to p.activityXp,
    "badgeBonusXp" to p.badgeBonusXp,
    "pinnedBadgeIds" to p.pinnedBadgeIds,
    "updatedAt" to System.currentTimeMillis()
    // password intentionally omitted
)

internal fun toEntity(data: Map<String, Any?>): UserProgressEntity = UserProgressEntity(
    id = (data["id"] as? Number)?.toInt() ?: 1,
    userName = data["userName"] as? String ?: "",
    email = data["email"] as? String ?: "",
    fullName = data["fullName"] as? String ?: "",
    age = (data["age"] as? Number)?.toInt(),
    address = data["address"] as? String ?: "",
    profileIconId = (data["profileIconId"] as? Number)?.toInt() ?: 1,
    profileBackgroundId = data["profileBackgroundId"] as? String ?: "",
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
    dailyXpEarned = (data["dailyXpEarned"] as? Number)?.toInt() ?: 0,
    dailyXpDate = data["dailyXpDate"] as? String ?: "",
    titleBadge = data["titleBadge"] as? String ?: "Kasiguranin Apprentice",
    submissionsMade = (data["submissionsMade"] as? Number)?.toInt() ?: 0,
    dailyReviewCompletedDate = data["dailyReviewCompletedDate"] as? String ?: "",
    dailyGamesDate = data["dailyGamesDate"] as? String ?: "",
    dailyGamesPlayedCount = (data["dailyGamesPlayedCount"] as? Number)?.toInt() ?: 0,
    xpPolicyVersion = (data["xpPolicyVersion"] as? Number)?.toInt() ?: 0,
    activityXp = (data["activityXp"] as? Number)?.toInt() ?: 0,
    badgeBonusXp = (data["badgeBonusXp"] as? Number)?.toInt() ?: 0,
    pinnedBadgeIds = data["pinnedBadgeIds"] as? String ?: "",
    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
)

/**
 * Reconciles local and remote progress.
 *
 * **This constructs a new entity field by field rather than copying one.** Any field added to
 * [UserProgressEntity] must therefore be handled here *and* in `toMap` *and* in `toEntity`, or it
 * silently reverts to its default the next time a sync runs — the write succeeds, no error is logged,
 * and the value simply disappears. That is exactly how the daily-XP ledger was being erased on every
 * app start before it was added to all three.
 *
 * **Streak merge rule**: the streak is NOT a simple lifetime max — it is a time-sensitive counter
 * that resets when the user misses a day. Taking `maxOf(local, remote)` would resurrect a remote
 * streak that the device had correctly reset to 0. Instead we adopt the streak from whichever side
 * recorded activity most recently (higher `lastActiveDate`), and then expire it here if that date
 * is already more than one day old — matching the same rule used in
 * `UserProgressRepository.validateAndResetExpiredStreak`.
 *
 * [today] is the day that expiry is measured from. Production leaves it as the real date; tests pin
 * it, because a fixed `lastActiveDate` in a test ages past the one-day window as the calendar moves
 * and the test starts failing with no code change.
 */
internal fun mergeProgress(
    local: UserProgressEntity,
    remote: UserProgressEntity,
    today: java.time.LocalDate = java.time.LocalDate.now()
): UserProgressEntity {
    val remoteNewer = remote.updatedAt >= local.updatedAt
    // Version-2 totals are projected from receipts. Legacy aggregate XP must never restore itself.
    val normalized = when {
        local.xpPolicyVersion >= XpPolicy.VERSION -> local
        remote.xpPolicyVersion >= XpPolicy.VERSION -> remote
        else -> null
    }

    // Daily-XP ledger. ISO dates compare lexicographically, so the later date wins outright and a
    // shared date takes the higher count — a second device cannot erase XP earned on this one.
    val (ledgerDate, ledgerXp) = when {
        local.dailyXpDate == remote.dailyXpDate ->
            local.dailyXpDate to maxOf(local.dailyXpEarned, remote.dailyXpEarned)
        local.dailyXpDate > remote.dailyXpDate -> local.dailyXpDate to local.dailyXpEarned
        else -> remote.dailyXpDate to remote.dailyXpEarned
    }

    val mergedReviewDate = when {
        local.dailyReviewCompletedDate == remote.dailyReviewCompletedDate -> local.dailyReviewCompletedDate
        local.dailyReviewCompletedDate > remote.dailyReviewCompletedDate -> local.dailyReviewCompletedDate
        else -> remote.dailyReviewCompletedDate
    }

    val (mergedGamesDate, mergedGamesCount) = when {
        local.dailyGamesDate == remote.dailyGamesDate ->
            local.dailyGamesDate to maxOf(local.dailyGamesPlayedCount, remote.dailyGamesPlayedCount)
        local.dailyGamesDate > remote.dailyGamesDate -> local.dailyGamesDate to local.dailyGamesPlayedCount
        else -> remote.dailyGamesDate to remote.dailyGamesPlayedCount
    }

    // ── Streak: date-aware merge, then expiry check ──────────────────────────
    //
    // Pick the (streak, lastActiveDate) pair from whichever side last recorded
    // activity. If both dates are equal, take the higher streak count (the two
    // devices may have synced at slightly different times within the same day).
    val (rawStreak, mergedLastActiveDate) = when {
        local.lastActiveDate == remote.lastActiveDate ->
            maxOf(local.currentStreak, remote.currentStreak) to local.lastActiveDate
        local.lastActiveDate > remote.lastActiveDate ->
            local.currentStreak to local.lastActiveDate
        else ->
            remote.currentStreak to remote.lastActiveDate
    }

    // Apply the same expiry rule used in validateAndResetExpiredStreak: if the
    // winning lastActiveDate is more than 1 calendar day in the past, the user
    // missed a day and the streak must be 0.
    val mergedStreak = if (rawStreak > 0 && mergedLastActiveDate.isNotEmpty()) {
        val lastDate = runCatching {
            java.time.LocalDate.parse(mergedLastActiveDate)
        }.getOrNull()
        if (lastDate != null && java.time.temporal.ChronoUnit.DAYS.between(
                lastDate, today) > 1
        ) 0 else rawStreak
    } else rawStreak
    // ────────────────────────────────────────────────────────────────────────

    return UserProgressEntity(
        id = 1,
        // A default is not a name: a real nickname on either side wins over one, whichever synced
        // last, or a fresh install would overwrite the one set on another device.
        userName = pick(
            local.userName.takeUnless(PublicProfileDto::isPlaceholderName).orEmpty(),
            remote.userName.takeUnless(PublicProfileDto::isPlaceholderName).orEmpty(),
            remoteNewer
        ).ifBlank {
            pick(local.userName, remote.userName, remoteNewer).ifBlank { PublicProfileDto.PLACEHOLDER_NAME }
        },
        password = local.password,
        email = local.email,
        fullName = pick(local.fullName, remote.fullName, remoteNewer),
        age = if (remoteNewer) remote.age ?: local.age else local.age ?: remote.age,
        address = pick(local.address, remote.address, remoteNewer),
        profileIconId = if (remoteNewer) remote.profileIconId else local.profileIconId,
        profileBackgroundId = pick(local.profileBackgroundId, remote.profileBackgroundId, remoteNewer),
        totalXp = normalized?.totalXp ?: maxOf(local.totalXp, remote.totalXp),
        level = normalized?.level ?: maxOf(local.level, remote.level),
        currentStreak = mergedStreak,
        longestStreak = maxOf(local.longestStreak, remote.longestStreak),
        lastActiveDate = mergedLastActiveDate,
        wordsLearned = maxOf(local.wordsLearned, remote.wordsLearned),
        storiesCompleted = maxOf(local.storiesCompleted, remote.storiesCompleted),
        gamesPlayed = maxOf(local.gamesPlayed, remote.gamesPlayed),
        totalCorrectAnswers = maxOf(local.totalCorrectAnswers, remote.totalCorrectAnswers),
        totalQuestionsAnswered = maxOf(local.totalQuestionsAnswered, remote.totalQuestionsAnswered),
        lessonsCompleted = maxOf(local.lessonsCompleted, remote.lessonsCompleted),
        isOnboardingCompleted = local.isOnboardingCompleted || remote.isOnboardingCompleted,
        dailyGoalXp = if (remoteNewer) remote.dailyGoalXp else local.dailyGoalXp,
        dailyXpEarned = normalized?.dailyXpEarned ?: ledgerXp,
        dailyXpDate = normalized?.dailyXpDate ?: ledgerDate,
        xpPolicyVersion = normalized?.xpPolicyVersion ?: 0,
        activityXp = normalized?.activityXp ?: 0,
        badgeBonusXp = normalized?.badgeBonusXp ?: 0,
        pinnedBadgeIds = if(remoteNewer && remote.xpPolicyVersion >= XpPolicy.VERSION) remote.pinnedBadgeIds else local.pinnedBadgeIds,
        titleBadge = pick(local.titleBadge, remote.titleBadge, remoteNewer),
        // A lifetime total, so it takes the max like the other counters rather than the
        // newer side's value — a device that synced before a submission must not undo it.
        submissionsMade = maxOf(local.submissionsMade, remote.submissionsMade),
        dailyReviewCompletedDate = mergedReviewDate,
        dailyGamesDate = mergedGamesDate,
        dailyGamesPlayedCount = mergedGamesCount,
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
    nextReviewDate = nextReviewDate,
    lapses = lapses,
    relearningStep = relearningStep
)

/**
 * Every document under `users/{uid}/progress`.
 *
 * One list, because three places have to agree on it and none of them fails loudly when they
 * drift: this manager (which writes them), [AuthRepository.deleteAccount] (which must remove
 * every one of them, or deleting an account leaves data behind), and `firestore.rules`'
 * isValidProgressDoc(). `lessonProgress` was added to the first and missed by the second, so
 * a deleted account kept its lesson history in the cloud indefinitely — the same drift that
 * silently broke main-progress sync when a field was added to toMap() but not to the rules.
 *
 * MainProgressRulesParityTest checks this list against the rules.
 */
internal object ProgressDocuments {
    const val MAIN = "main"
    const val ACHIEVEMENTS = "achievements"
    const val GAME_LEVELS = "gameLevels"
    const val LESSON_PROGRESS = "lessonProgress"
    const val WORD_STATES = "wordStates"

    /** The learning-state documents: everything except [MAIN], which has its own schema. */
    val LEARNING = listOf(ACHIEVEMENTS, GAME_LEVELS, LESSON_PROGRESS, WORD_STATES)

    /** Every progress document, for callers that must touch all of them (deletion). */
    val ALL = listOf(MAIN) + LEARNING
}
