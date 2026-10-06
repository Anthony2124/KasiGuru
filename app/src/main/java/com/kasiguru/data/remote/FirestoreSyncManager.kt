package com.kasiguru.data.remote

import android.util.Log
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.local.dao.StoryDao
import com.kasiguru.data.local.DatabaseSeeder
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
         * How often to reconcile: check for what the incremental pull cannot see.
         *
         * The incremental path queries `updatedAt > lastSync`, so it cannot see a document
         * written by a path that forgot to stamp the field, or one written before the
         * backfill. A full pull picks those up and self-heals that drift.
         *
         * It is also the only pull that may *remove* a word — see [pruneWithdrawnWords]. The
         * realtime listener handles deletions instantly when the app is open, but if the app was
         * offline when a word was removed, this is the safety net that catches it.
         *
         * The reconcile used to re-read every word, ~1,150 reads per phone per day, which on its
         * own came close to the Spark plan's 50,000. It now reads the whole collection only when
         * [vocabularyFingerprint] changed since the last full read, or [FULL_READ_SAFETY_INTERVAL_MS]
         * is up, and otherwise costs a couple of reads.
         */
        private val FULL_RECONCILE_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)

        /**
         * The longest a phone goes without reading every word, fingerprint or not. The fingerprint
         * moves with every add, delete and stamped edit; this catches the one change it cannot see,
         * an edit to a field the app reads by a writer that did not stamp `updatedAt`. Every writer
         * in this repository stamps it (functions/tag_themes.js writes only proposal fields the app
         * never reads), so this is a backstop, not a schedule.
         */
        private val FULL_READ_SAFETY_INTERVAL_MS = TimeUnit.DAYS.toMillis(30)
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
            val reconcile = force || shouldFullReconcile()
            val lastSync = userPreferencesRepository.lastContentSyncAtOnce()

            // Taken before the read, so a write landing during it leaves the stored fingerprint
            // behind and the next reconcile reads everything again rather than missing that write.
            val fingerprint = if (reconcile) vocabularyFingerprint() else null
            val readAllWords = reconcile && needsFullVocabularyRead(
                force = force,
                hasLocalContent = vocabularyDao.getTotalCountDirect() > 0,
                storedFingerprint = userPreferencesRepository.vocabularyFingerprintOnce(),
                currentFingerprint = fingerprint,
                lastFullReadAt = userPreferencesRepository.lastFullVocabularyReadAtOnce(),
                now = now,
                safetyIntervalMs = FULL_READ_SAFETY_INTERVAL_MS
            )

            // 0 makes the incremental query match every document that carries the field,
            // which is what a first sync on a device wants anyway.
            syncVocabulary(if (readAllWords) 0L else lastSync, readAllWords)
            // Stories stay on the daily full read: there are a handful, so it costs a handful.
            syncStories(if (reconcile) 0L else lastSync, reconcile)

            userPreferencesRepository.setLastContentSyncAt(now)
            if (reconcile) {
                userPreferencesRepository.setLastFullReconcileAt(now)
            }
            if (readAllWords) {
                userPreferencesRepository.setFullVocabularyRead(now, fingerprint)
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
     * The number of vocabulary documents and the sum of their `updatedAt`, as "count:sum".
     *
     * One aggregation query, billed one read per 1,000 index entries: about two reads where reading
     * the collection costs one per word. Adding, deleting or re-stamping any word moves it. Null
     * when the query fails, which falls back to reading everything as before.
     */
    private suspend fun vocabularyFingerprint(): String? = runCatching {
        val sum = AggregateField.sum(UPDATED_AT)
        val snapshot = firestore.collection("vocabulary")
            .aggregate(AggregateField.count(), sum)
            .get(AggregateSource.SERVER)
            .await()
        versionedFingerprint("${snapshot.count}:${snapshot.get(sum)}")
    }.onFailure {
        Log.w(TAG, "vocabulary fingerprint failed; reading every word instead", it)
    }.getOrNull()

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

    /**
     * Identifies one *sense* of a word, which is what a vocabulary row actually is.
     *
     * Must stay in step with the grouping in [VocabularyDao.deleteDuplicateWords]: if the two
     * disagree, one of them silently merges or deletes a sense the other meant to keep.
     */
    private fun senseKey(kasiguranin: String, english: String): String =
        kasiguranin.trim().lowercase() + " " + english.trim().lowercase()

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
                category = doc.getString("category").orEmpty().ifBlank { "General" },
                // The learning tree's section tag. Only the whole-collection realtime listener read it,
                // so once 1.24 scoped that listener to recent edits a new install never got a theme and
                // its course collapsed into one "Pang-araw-araw" section. "nan" is a pandas export
                // artefact meaning untagged.
                theme = doc.getString("theme").orEmpty().trim().let { if (it == "nan") "" else it },
                partOfSpeech = doc.getString("partOfSpeech").orEmpty(),
                // Editorial definitions. Blank until backfill_meanings.js has run against this
                // project; every consumer treats blank as "no definition yet" and hides itself.
                meaningEnglish = doc.getString("meaningEnglish").orEmpty(),
                meaningTagalog = doc.getString("meaningTagalog").orEmpty(),
                audioFileName = doc.getString("audioResName").orEmpty(),
                // Bumped by the admin word form on every pronunciation-clip change. > 0 is how the
                // merge tells an admin-authored audio field from a legacy doc that omits it.
                audioUpdatedAt = doc.getLong("audioUpdatedAt") ?: 0L,
                exampleSentence = doc.getString("exampleSentence").orEmpty(),
                exampleTranslation = doc.getString("exampleTranslation").orEmpty(),
                // The admin word form has written these since v25 and nothing ever read them, so a
                // second example could be recorded in the portal and never reach a learner.
                exampleSentence2 = doc.getString("exampleSentence2").orEmpty(),
                exampleTranslation2 = doc.getString("exampleTranslation2").orEmpty(),
                phoneticGlottal = doc.getBoolean("phoneticGlottal") ?: false,
                phoneticVowelLength = doc.getBoolean("phoneticVowelLength") ?: false,
                ipaNotation = doc.getString("ipaNotation").orEmpty()
            )
        }
        if (cloudWords.isEmpty()) return

        // Keyed on headword *and* gloss. On the headword alone every homonym collapsed into one
        // entry here before deleteDuplicateWords() ever ran, so the cloud's `lima` (five) was merged
        // onto the device's `lima` (hand) and one of the two senses was lost on every sync.
        val localByWord = vocabularyDao.getAllVocabularyOnce()
            .associateBy { senseKey(it.kasiguranin, it.english) }

        val wordsToSave = cloudWords.map { cloudWord ->
            val local = localByWord[senseKey(cloudWord.kasiguranin, cloudWord.english)]
            VocabularyContentMerge.merge(local, cloudWord)
        }

        if (wordsToSave.isNotEmpty()) {
            vocabularyDao.insertAll(wordsToSave)
        }
        // Deduplicate any legacy duplicates (kept after sync, not on every app start).
        vocabularyDao.deleteDuplicateWords()

        if (fullReconcile) pruneWithdrawnWords(cloudWords)
    }

    /**
     * Drops rows that no longer exist anywhere upstream.
     *
     * The content sync only ever inserted and updated, so a document *renamed* in the portal left
     * the row it used to be behind on every device that had already pulled it. That went unnoticed
     * until a corpus repair corrected fifty-two documents at once and every device that had synced
     * beforehand kept fifty-two stale rows -- `kulot kulot` sitting in the dictionary next to the
     * `kulot` that replaced it, with no definition and no way to remove it.
     *
     * Only on a full reconcile, because only then is [cloud] the complete upstream collection; on
     * an incremental pull it is just the recent changes, and treating that as the whole world would
     * delete the entire dictionary.
     *
     * A word is removed only when it is missing from the cloud *and* from the corpus this build
     * ships, so anything seeded locally survives a Firestore that has never heard of it. The
     * learner's history for a withdrawn word goes with it, which is the point: the word is gone.
     */
    private suspend fun pruneWithdrawnWords(cloud: List<VocabularyEntity>) {
        val keep = HashSet<String>()
        cloud.forEach { keep.add(senseKey(it.kasiguranin, it.english)) }
        DatabaseSeeder.getInitialVocabulary().forEach { keep.add(senseKey(it.kasiguranin, it.english)) }

        val withdrawn = vocabularyDao.getAllVocabularyOnce()
            .filter { senseKey(it.kasiguranin, it.english) !in keep }
        if (withdrawn.isEmpty()) return

        Log.i(TAG, "pruning " + withdrawn.size + " words withdrawn upstream")
        vocabularyDao.deleteWords(withdrawn.map { it.id })
    }

    private suspend fun syncStories(since: Long, fullReconcile: Boolean) {
        val snapshot = fetchContent("stories", since, fullReconcile)
        if (snapshot.isEmpty) return

        val cloudStories = snapshot.documents.mapNotNull { doc ->
            val title = doc.getString("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            // A document with no usable id would land on story 0, and with REPLACE every such
            // document would overwrite that one row in turn. Skip it the way syncVocabulary skips
            // a blank headword.
            val id = (doc.getLong("id") ?: 0L).toInt().takeIf { it > 0 } ?: return@mapNotNull null
            StoryEntity(
                id = id,
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

        if (fullReconcile) pruneWithdrawnStories(cloudStories)
    }

    /**
     * Drops story rows that no longer exist anywhere upstream.
     *
     * The story sync only ever inserted and updated, so a story deleted in the admin portal, or
     * re-created there under a new id, left its old row behind on every device that had already
     * pulled it -- the same orphaning [pruneWithdrawnWords] fixes on the vocabulary side, never
     * ported here until now.
     *
     * Only on a full reconcile, because only then is [cloud] the complete upstream collection; on an
     * incremental pull it is just the recent changes, and treating that as the whole world would
     * delete every story.
     *
     * A story is removed only when its id is missing from the cloud *and* from the set this build
     * ships, so the ten seeded folk tales survive a Firestore that has never heard of them.
     */
    private suspend fun pruneWithdrawnStories(cloud: List<StoryEntity>) {
        val withdrawn = storiesToPrune(
            localIds = storyDao.getAllStoriesOnce().map { it.id },
            cloudIds = cloud.map { it.id },
            seededIds = DatabaseSeeder.getInitialStories().map { it.id }
        )
        if (withdrawn.isEmpty()) return

        Log.i(TAG, "pruning " + withdrawn.size + " stories withdrawn upstream")
        storyDao.deleteStories(withdrawn)
    }
}

/**
 * Local story ids to remove on a full reconcile: those on the device but in neither the upstream
 * collection nor the corpus this build ships.
 *
 * Pure, so the reconcile rule can be unit-tested without a Firestore instance or a DAO -- the same
 * reason [isDueForSync] lives out here.
 */
internal fun storiesToPrune(
    localIds: List<Int>,
    cloudIds: List<Int>,
    seededIds: List<Int>
): List<Int> {
    val keep = HashSet<Int>(cloudIds)
    keep.addAll(seededIds)
    return localIds.filter { it !in keep }
}

/**
 * What the word parser reads. Bump it whenever the parser starts reading a field it used to skip:
 * every phone's stored fingerprint then stops matching and it reads the whole collection once, so
 * the new field reaches words that will never be edited again. 2 added `theme` (1.25.1).
 */
internal const val VOCABULARY_PARSER_VERSION = 2

/** The collection's "count:sum" fingerprint, tagged with [VOCABULARY_PARSER_VERSION]. */
internal fun versionedFingerprint(raw: String): String = "v$VOCABULARY_PARSER_VERSION/$raw"

/**
 * Whether the daily reconcile must read every vocabulary document, or can trust the incremental
 * pull. Pure, for the same reason as [isDueForSync].
 *
 * Reads everything when asked to, when there is nothing local, when either fingerprint is missing
 * (a first run, a phone updating from a version that never took one, or a failed aggregation),
 * when the fingerprint moved since the last full read, or when that read is older than
 * [safetyIntervalMs].
 */
internal fun needsFullVocabularyRead(
    force: Boolean,
    hasLocalContent: Boolean,
    storedFingerprint: String?,
    currentFingerprint: String?,
    lastFullReadAt: Long,
    now: Long,
    safetyIntervalMs: Long
): Boolean {
    if (force || !hasLocalContent) return true
    if (storedFingerprint.isNullOrEmpty() || currentFingerprint.isNullOrEmpty()) return true
    if (storedFingerprint != currentFingerprint) return true
    return isDueForSync(hasLocalContent = true, lastRunAt = lastFullReadAt, now = now, intervalMs = safetyIntervalMs)
}

/**
 * Whether an interval-gated sync pass is due. Pure, so it can be unit-tested without a
 * Firestore instance or a DAO — the same reason [com.kasiguru.data.repository.mergeProgress]
 * lives outside its manager.
 *
 * The six-hour content pull, the daily reconcile and the 30-day full read run this identical
 * decision with different intervals. Sharing it means the awkward cases below are reasoned about once
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
