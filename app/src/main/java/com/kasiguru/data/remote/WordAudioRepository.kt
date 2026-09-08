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
 * Every failure path returns null. A word with no clip, a missing document, or an offline device with
 * nothing cached is a normal state, not an error: [com.kasiguru.util.audio.AudioPlayerManager] falls
 * back to text-to-speech, exactly as it does today.
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
     * @return the cached file, or null when the word has no clip, the document is missing, or the
     *   device is offline with nothing cached.
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
                temp.writeBytes(bytes)
                if (!temp.renameTo(cached)) {
                    temp.delete()
                    return@withContext null
                }

                pruneOldVersions(key, keep = cached.name)
                cached
            } catch (e: Exception) {
                Log.w("WordAudioRepository", "Could not load audio for $key", e)
                null
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
