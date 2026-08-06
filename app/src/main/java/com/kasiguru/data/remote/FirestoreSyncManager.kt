package com.kasiguru.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.local.DatabaseSeeder
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
        if (snapshot.isEmpty) {
            // First time setup: push local seed data to Firestore
            pushVocabularySeedData()
            return
        }

        val cloudWords = snapshot.toObjects(VocabularyEntity::class.java)
        for (cloudWord in cloudWords) {
            val localWord = vocabularyDao.getVocabularyById(cloudWord.id)
            val wordToSave = if (localWord != null) {
                // Preserve local progress fields
                cloudWord.copy(
                    isLearned = localWord.isLearned,
                    timesReviewed = localWord.timesReviewed,
                    easinessFactor = localWord.easinessFactor,
                    intervalDays = localWord.intervalDays,
                    nextReviewDate = localWord.nextReviewDate
                )
            } else {
                cloudWord
            }
            vocabularyDao.insert(wordToSave)
        }
    }

    private suspend fun syncStories() {
        val snapshot = firestore.collection("stories").get().await()
        if (snapshot.isEmpty) {
            pushStoriesSeedData()
            return
        }

        val cloudStories = snapshot.toObjects(StoryEntity::class.java)
        for (cloudStory in cloudStories) {
            val localStory = storyDao.getStoryById(cloudStory.id)
            val storyToSave = if (localStory != null) {
                // Preserve local progress fields
                cloudStory.copy(
                    isUnlocked = localStory.isUnlocked,
                    isCompleted = localStory.isCompleted,
                    currentPage = localStory.currentPage
                )
            } else {
                cloudStory
            }
            storyDao.insert(storyToSave)
        }
    }

    private suspend fun pushVocabularySeedData() {
        val batch = firestore.batch()
        DatabaseSeeder.getInitialVocabulary().forEach { word ->
            val docRef = firestore.collection("vocabulary").document(word.id.toString())
            batch.set(docRef, word)
        }
        batch.commit().await()
    }

    private suspend fun pushStoriesSeedData() {
        val batch = firestore.batch()
        DatabaseSeeder.getInitialStories().forEach { story ->
            val docRef = firestore.collection("stories").document(story.id.toString())
            batch.set(docRef, story)
        }
        batch.commit().await()
    }
}
