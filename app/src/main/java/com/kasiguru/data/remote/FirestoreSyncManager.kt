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

        val cloudWords = snapshot.toObjects(VocabularyEntity::class.java)
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

        val cloudStories = snapshot.toObjects(StoryEntity::class.java)
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
