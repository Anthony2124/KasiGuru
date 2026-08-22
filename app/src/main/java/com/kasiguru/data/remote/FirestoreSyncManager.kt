package com.kasiguru.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.local.dao.StoryDao
import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.StoryEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.repository.UserPreferencesRepository
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val vocabularyDao: VocabularyDao,
    private val storyDao: StoryDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        private const val TAG = "FirestoreSyncManager"

        /** Field the admin site stamps on every vocabulary/story write, in epoch millis. */
        const val UPDATED_AT = "updatedAt"

        /**
         * Minimum gap between content pulls.
         *
         * Running the pull unconditionally in `MainActivity.onCreate` meant roughly 125
         * app opens in total, across the whole userbase, could exhaust the Spark plan's
         * 50,000 reads/day — a project-wide ceiling shared by every user — and take the
         * leaderboard, announcements, update check and progress sync down with it.
         *
         * Six hours keeps a same-day dictionary edit reaching users quickly while cutting
         * a heavy user's launches from dozens of pulls a day to at most four.
         */
        private val MIN_SYNC_INTERVAL_MS = TimeUnit.HOURS.toMillis(6)

        /**
         * How often to fall back to reading the whole collection.
         *
         * The incremental path queries `updatedAt > lastSync`, so it cannot see a document
         * written by a path that forgot to stamp the field, or one written before the
         * backfill. A periodic full pull picks those up and self-heals that drift. Weekly
         * against the six-hour incremental cadence means the expensive read happens on
         * roughly one sync in twenty-eight.
         *
         * It does **not** propagate deletions. A document removed upstream simply stops
         * being returned, which is indistinguishable from "unchanged", and this class has
         * never deleted local rows — it only inserts and updates. Making the full pull
         * authoritative enough to delete would mean trusting that the cloud collection is
         * the complete source of truth, but every install also ships DatabaseSeeder's
         * bundled copy of the dictionary. If the two ever diverge, a reconcile that
         * deleted would destroy seeded words and the SRS progress attached to them. That
         * needs verifying against production before it can be safe, so deletion
         * propagation stays unsolved here rather than guessed at.
         */
        private val FULL_RECONCILE_INTERVAL_MS = TimeUnit.DAYS.toMillis(7)
    }

    /**
     * Pulls dictionary and story content, unless it was pulled recently.
     *
     * Pass [force] to bypass the interval check for a user-initiated refresh — the point
     * of the throttle is to stop *automatic* launches from spending the quota, not to
     * refuse someone who explicitly asked.
     */
    suspend fun syncWithFirestore(force: Boolean = false) {
        try {
            if (!force && !shouldSync()) {
                Log.d(TAG, "Content sync skipped: last pull was under 6h ago")
                return
            }

            val now = System.currentTimeMillis()
            val fullReconcile = force || shouldFullReconcile()
            // 0 makes the incremental query match every document that carries the field,
            // which is what a first sync on a device wants anyway.
            val since = if (fullReconcile) 0L else userPreferencesRepository.lastContentSyncAtOnce()

            syncVocabulary(since, fullReconcile)
            syncStories(since, fullReconcile)

            userPreferencesRepository.setLastContentSyncAt(now)
            if (fullReconcile) {
                userPreferencesRepository.setLastFullReconcileAt(now)
            }
        } catch (e: Exception) {
            // Deliberately not stamping the timestamp on failure, so a failed pull retries
            // on the next launch instead of being throttled out for six hours.
            Log.e(TAG, "Sync failed", e)
        }
    }

    private suspend fun shouldSync(): Boolean = isDueForSync(
        // Counted rather than fetched — a full pull reads every row anyway, and there is
        // no reason to pay for that when it won't run.
        hasLocalContent = vocabularyDao.getTotalCountDirect() > 0,
        lastRunAt = userPreferencesRepository.lastContentSyncAtOnce(),
        now = System.currentTimeMillis(),
        intervalMs = MIN_SYNC_INTERVAL_MS
    )

    private suspend fun shouldFullReconcile(): Boolean = isDueForSync(
        hasLocalContent = vocabularyDao.getTotalCountDirect() > 0,
        lastRunAt = userPreferencesRepository.lastFullReconcileAtOnce(),
        now = System.currentTimeMillis(),
        intervalMs = FULL_RECONCILE_INTERVAL_MS
    )

    /**
     * Reads the collection, incrementally when possible.
     *
     * The incremental query needs `updatedAt` present on the document. Anything written
     * before the backfill, or by a path that forgot to stamp it, is invisible here and is
     * picked up by the next full reconcile instead — which is why [FULL_RECONCILE_INTERVAL_MS]
     * exists rather than this being a pure incremental sync.
     */
    private suspend fun fetchContent(collection: String, since: Long, fullReconcile: Boolean) =
        if (fullReconcile) {
            firestore.collection(collection).get().await()
        } else {
            firestore.collection(collection)
                .whereGreaterThan(UPDATED_AT, since)
                .get()
                .await()
        }

    private suspend fun syncVocabulary(since: Long, fullReconcile: Boolean) {
        val snapshot = fetchContent("vocabulary", since, fullReconcile)
        // Nothing changed since the last pull — the common case, and the whole point.
        if (snapshot.isEmpty) {
            if (fullReconcile) Log.w(TAG, "vocabulary is empty upstream; leaving local rows alone")
            return
        }

        // Mapped field by field rather than via toObjects(): documents written by
        // the admin site often omit optional fields, and reflection would leave
        // those Kotlin non-null strings null, blowing up on the first copy().
        val cloudWords = snapshot.documents.mapNotNull { doc ->
            val word = doc.getString("kasiguranin")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            VocabularyEntity(
                kasiguranin = word,
                tagalog = doc.getString("tagalog").orEmpty(),
                english = doc.getString("english").orEmpty(),
                rootForm = doc.getString("rootForm").orEmpty(),
                neutralForm = doc.getString("neutralForm").orEmpty(),
                imperfectiveForm = doc.getString("imperfectiveForm").orEmpty(),
                perfectiveForm = doc.getString("perfectiveForm").orEmpty(),
                contemplativeForm = doc.getString("contemplativeForm").orEmpty(),
                category = doc.getString("category").orEmpty().ifBlank { "Uncategorized" },
                audioFileName = doc.getString("audioResName").orEmpty(),
                exampleSentence = doc.getString("exampleSentence").orEmpty(),
                exampleTranslation = doc.getString("exampleTranslation").orEmpty(),
                phoneticGlottal = doc.getBoolean("phoneticGlottal") ?: false,
                phoneticVowelLength = doc.getBoolean("phoneticVowelLength") ?: false,
                ipaNotation = doc.getString("ipaNotation").orEmpty()
            )
        }
        if (cloudWords.isEmpty()) return

        val localByWord = vocabularyDao.getAllVocabularyOnce()
            .associateBy { it.kasiguranin.lowercase() }

        val wordsToSave = cloudWords.map { cloudWord ->
            val localWord = localByWord[cloudWord.kasiguranin.lowercase()]
            if (localWord != null) {
                // Preserve local ID & learning progress fields
                cloudWord.copy(
                    id = localWord.id,
                    isLearned = localWord.isLearned,
                    timesReviewed = localWord.timesReviewed,
                    easinessFactor = localWord.easinessFactor,
                    intervalDays = localWord.intervalDays,
                    nextReviewDate = localWord.nextReviewDate
                )
            } else {
                cloudWord.copy(id = 0)
            }
        }

        if (wordsToSave.isNotEmpty()) {
            vocabularyDao.insertAll(wordsToSave)
        }
        // Deduplicate any legacy duplicates (kept after sync, not on every app start).
        vocabularyDao.deleteDuplicateWords()
    }

    private suspend fun syncStories(since: Long, fullReconcile: Boolean) {
        val snapshot = fetchContent("stories", since, fullReconcile)
        if (snapshot.isEmpty) return

        val cloudStories = snapshot.documents.mapNotNull { doc ->
            val title = doc.getString("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            StoryEntity(
                id = (doc.getLong("id") ?: 0L).toInt(),
                title = title,
                titleKasiguranin = doc.getString("titleKasiguranin").orEmpty(),
                description = doc.getString("description").orEmpty(),
                category = doc.getString("category").orEmpty(),
                iconEmoji = doc.getString("iconEmoji").orEmpty().ifBlank { "📖" },
                pagesJson = doc.getString("pagesJson").orEmpty().ifBlank { "[]" },
                totalPages = (doc.getLong("totalPages") ?: 0L).toInt(),
                requiredXp = (doc.getLong("requiredXp") ?: 0L).toInt()
            )
        }
        if (cloudStories.isEmpty()) return

        val localById = storyDao.getAllStoriesOnce().associateBy { it.id }

        val storiesToSave = cloudStories.map { cloudStory ->
            val localStory = localById[cloudStory.id]
            if (localStory != null) {
                // Preserve local progress fields
                cloudStory.copy(
                    isUnlocked = localStory.isUnlocked,
                    isCompleted = localStory.isCompleted,
                    currentPage = localStory.currentPage
                )
            } else {
                cloudStory
            }
        }

        if (storiesToSave.isNotEmpty()) {
            storyDao.insertAll(storiesToSave)
        }
    }
}

/**
 * Whether an interval-gated sync pass is due. Pure, so it can be unit-tested without a
 * Firestore instance or a DAO — the same reason [com.kasiguru.data.repository.mergeProgress]
 * lives outside its manager.
 *
 * Both the six-hour content pull and the weekly full reconcile run this identical decision
 * with different intervals. Sharing it means the awkward cases below are reasoned about once
 * rather than duplicated and drifting apart.
 *
 * @param hasLocalContent false on a fresh install or after the user clears app data, where
 *   there is nothing to read offline and the interval must not apply.
 * @param lastRunAt epoch millis of the last successful pass; 0 means never.
 * @param now current epoch millis.
 * @param intervalMs minimum gap between passes.
 */
internal fun isDueForSync(
    hasLocalContent: Boolean,
    lastRunAt: Long,
    now: Long,
    intervalMs: Long
): Boolean {
    // Nothing usable offline — sync regardless of how recently one ran.
    if (!hasLocalContent) return true

    // Never run before.
    if (lastRunAt <= 0L) return true

    val elapsed = now - lastRunAt
    // A clock moved backwards (timezone change, manual set, or a device whose clock was
    // wrong when the timestamp was written) would otherwise wedge the throttle shut until
    // real time caught up to the stored future timestamp — potentially years.
    if (elapsed < 0) return true

    return elapsed >= intervalMs
}
