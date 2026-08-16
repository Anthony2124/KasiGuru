package com.kasiguru.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.local.dao.StoryDao
import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.StoryEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val vocabularyDao: VocabularyDao,
    private val storyDao: StoryDao
) {
    suspend fun syncWithFirestore() {
        try {
            syncVocabulary()
            syncStories()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Sync failed", e)
        }
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
