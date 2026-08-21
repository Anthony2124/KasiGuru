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
 * Fetches the illustration for a single story page and keeps it on disk.
 *
 * Firebase Storage is not available on this project — it needs the Blaze plan, and KasiGuru stays on
 * Spark — so the pictures live in Firestore as raw bytes, one document per picture at
 * `story_page_images/{storyId}_{imageId}`.
 *
 * Two deliberate choices, both about the audience. PRODUCT.md describes learners as "phone-first,
 * mid-range Android, often on patchy connectivity" and "data- and storage-sensitive":
 *
 *  - **One page at a time.** Nothing here fetches a whole story. The reader asks for the page it is
 *    showing and, at most, prefetches the next one. A twelve-page story never costs twelve downloads
 *    up front for a learner who reads two pages and leaves.
 *  - **Cached to files, permanently.** The first read of a page is the only one that costs data.
 *    Bytes go to `filesDir/story_images/`, not into Room, which keeps the database small and means
 *    no schema change was needed to ship this.
 *
 * Every failure path returns null rather than throwing. A missing picture is a normal state, not an
 * error: the reader falls back to the gradient and the page's `illustrationDesc`, which is the
 * honest degradation DESIGN.md asks of every art slot.
 */
@Singleton
class StoryImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) {
    private val cacheDir: File by lazy {
        File(context.filesDir, "story_images").apply { mkdirs() }
    }

    private fun fileFor(storyId: Int, imageId: String) = File(cacheDir, "${storyId}_$imageId.webp")

    /**
     * The picture for this page, downloading it once if it is not already on disk.
     * Returns null when the page has no picture, when the document is missing, or when the device
     * is offline and nothing was cached.
     */
    suspend fun imageFor(storyId: Int, imageId: String): File? {
        if (imageId.isBlank()) return null

        val cached = fileFor(storyId, imageId)
        if (cached.exists() && cached.length() > 0) return cached

        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("story_page_images")
                    .document("${storyId}_$imageId")
                    .get()
                    .await()

                val bytes = snapshot.getBlob("data")?.toBytes()
                if (bytes == null || bytes.isEmpty()) return@withContext null

                // Write to a temporary file first: a download interrupted halfway would otherwise
                // leave a truncated file that every later read would treat as a valid cache hit.
                val temp = File(cacheDir, "${storyId}_$imageId.part")
                temp.writeBytes(bytes)
                if (!temp.renameTo(cached)) {
                    temp.delete()
                    return@withContext null
                }
                cached
            } catch (e: Exception) {
                Log.w("StoryImageRepository", "Could not load image $storyId/$imageId", e)
                null
            }
        }
    }

    /** Drops every cached picture. For a future "free up space" control; nothing calls it yet. */
    fun clearCache() {
        runCatching { cacheDir.listFiles()?.forEach { it.delete() } }
    }
}
