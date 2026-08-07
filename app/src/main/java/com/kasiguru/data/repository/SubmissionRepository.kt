package com.kasiguru.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.remote.model.WordSubmissionDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmissionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val submissionsCollection = firestore.collection("word_submissions")

    suspend fun submitWord(submission: WordSubmissionDto): Result<Unit> {
        return try {
            val docRef = submissionsCollection.document()
            val submissionWithId = submission.copy(id = docRef.id)
            docRef.set(submissionWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
