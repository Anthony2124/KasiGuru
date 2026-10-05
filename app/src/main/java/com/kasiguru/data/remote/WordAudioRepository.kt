package com.kasiguru.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the pronunciation clip for a single vocabulary word and keeps it on disk.
 *
 * The same shape as [StoryImageRepository], and for the same reason: Firebase Storage needs the Blaze
 * plan and KasiGuru stays on Spark, so the clips live in Firestore as raw bytes, one document per
 * word at `word_audio/{key}` where `key` is [VocabularyEntity.audioFileName] — the slug the admin
 * portal stamps on the word when it uploads a recording.
 *
 * Two choices, both about an audience PRODUCT.md calls "phone-first, mid-range Android, often on
 * patchy connectivity" and "data- and storage-sensitive":
 *
 *  - **One word at a time.** Nothing here prefetches the dictionary. A clip is downloaded the first
 *    time a learner asks to hear that specific word, so a learner who plays ten words never pays for
 *    twelve hundred.
 *  - **Cached to files, keyed on version.** The first play is the only one that costs data; after
 *    that it is a local file. The cache filename carries [VocabularyEntity.audioUpdatedAt], so an
 *    admin re-recording the word produces a new filename and the stale file is pruned rather than
 *    served forever. Bytes go to `filesDir/word_audio/`, never into Room, so no schema change beyond
 *    the version column was needed.
 *
 * A word with no clip or a missing document returns null: a normal state, not an error. A fetch that
 * fails (offline with nothing cached, or Firestore refusing reads) throws, so
 * [com.kasiguru.util.audio.AudioPlayerManager] can tell the learner why nothing played.
 */
@Singleton
class WordAudioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) {
    private val cacheDir: File by lazy {
        File(context.filesDir, "word_audio").apply { mkdirs() }
    }

    private fun fileFor(key: String, version: Long) = File(cacheDir, "${key}_$version.dat")

    /**
     * The clip for this word, downloading it once if it is not already on disk.
     *
     * @param key [VocabularyEntity.audioFileName] — the `word_audio` document id. Blank means the
     *   word has no custom recording.
     * @param version [VocabularyEntity.audioUpdatedAt] — bumped by the admin on every re-record, so a
     *   changed clip lands under a new filename.
     * @return the cached file, or null when the word has no clip or the document is missing.
     * @throws Exception when the clip exists upstream but could not be fetched.
     */
    suspend fun audioFor(key: String, version: Long): File? {
        if (key.isBlank()) return null

        val cached = fileFor(key, version)
        if (cached.exists() && cached.length() > 0) return cached

        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("word_audio")
                    .document(key)
                    .get()
                    .await()

                val bytes = snapshot.getBlob("data")?.toBytes()
                if (bytes == null || bytes.isEmpty()) return@withContext null

                // Temp file first: a download cut off halfway would otherwise leave a truncated file
                // that every later read treats as a valid cache hit.
                val temp = File(cacheDir, "${key}_$version.part")
                // The folder is created once per process; if storage cleanup removed it since, the
                // write failed with ENOENT and every clip stayed silent until the app restarted.
                cacheDir.mkdirs()
                temp.writeBytes(bytes)
                if (!temp.renameTo(cached)) {
                    temp.delete()
                    return@withContext null
                }

                pruneOldVersions(key, keep = cached.name)
                cached
            } catch (e: Exception) {
                // Rethrown, not swallowed: "this word has no clip" and "the clip could not be
                // fetched" (offline, or the project's daily read quota spent) need different words
                // to the learner, and a silent button reads as a broken speaker either way.
                Log.w("WordAudioRepository", "Could not load audio for $key", e)
                throw e
            }
        }
    }

    /** Drops earlier-version cache files for a word after a newer clip has been written. */
    private fun pruneOldVersions(key: String, keep: String) {
        runCatching {
            cacheDir.listFiles { f ->
                f.name != keep && (f.name.startsWith("${key}_") &&
                    (f.name.endsWith(".dat") || f.name.endsWith(".part")))
            }?.forEach { it.delete() }
        }
    }

    /** Drops every cached clip. For a future "free up space" control; nothing calls it yet. */
    fun clearCache() {
        runCatching { cacheDir.listFiles()?.forEach { it.delete() } }
    }
}
