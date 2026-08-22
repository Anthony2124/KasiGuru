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

        /**
         * Minimum gap between content pulls.
         *
         * [syncVocabulary] and [syncStories] each read their entire collection — there is
         * no incremental query, because the documents carry no `updatedAt` to filter on.
         * At ~400 vocabulary documents that is ~400 reads per pull, against the Spark
         * plan's 50,000 reads/day *project-wide* ceiling shared by every user. Running
         * that unconditionally in `MainActivity.onCreate` meant roughly 125 app opens in
         * total, across the whole userbase, could exhaust the day's quota and take the
         * leaderboard, announcements, update check and progress sync down with it.
         *
         * Six hours keeps a same-day dictionary edit reaching users quickly while cutting
         * a heavy user's launches from dozens of pulls a day to at most four.
         */
        private val MIN_SYNC_INTERVAL_MS = TimeUnit.HOURS.toMillis(6)
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
            syncVocabulary()
            syncStories()
            userPreferencesRepository.setLastContentSyncAt(System.currentTimeMillis())
        } catch (e: Exception) {
            // Deliberately not stamping the timestamp on failure, so a failed pull retries
            // on the next launch instead of being throttled out for six hours.
            Log.e(TAG, "Sync failed", e)
        }
    }

    private suspend fun shouldSync(): Boolean {
        // An empty local dictionary means a fresh install (or cleared data) that has
        // nothing to read offline, so it always syncs regardless of the interval.
        // Counted rather than fetched — syncVocabulary() already pulls the full row set
        // when it runs, and there is no reason to pay for that when it won't.
        if (vocabularyDao.getTotalCountDirect() == 0) return true

        val lastSync = userPreferencesRepository.lastContentSyncAtOnce()
        if (lastSync == 0L) return true

        val elapsed = System.currentTimeMillis() - lastSync
        // A clock moved backwards (timezone change, manual set) would otherwise wedge
        // the throttle shut until real time caught up.
        if (elapsed < 0) return true

        return elapsed >= MIN_SYNC_INTERVAL_MS
    }

    private suspend fun syncVocabulary() {
        val snapshot = firestore.collection("vocabulary").get().await()
        // Seeding is admin-only (Firestore rules deny client writes).
        if (snapshot.isEmpty) return

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

    private suspend fun syncStories() {
        val snapshot = firestore.collection("stories").get().await()
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
