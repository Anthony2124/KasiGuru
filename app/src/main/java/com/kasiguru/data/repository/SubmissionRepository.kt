package com.kasiguru.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.kasiguru.data.remote.model.LiteratureSubmissionDto
import com.kasiguru.data.remote.model.WordSubmissionDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmissionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val submissionsCollection = firestore.collection("word_submissions")
    private val literatureSubmissionsCollection = firestore.collection("literature_submissions")

    suspend fun submitWord(submission: WordSubmissionDto): Result<Unit> {
        return try {
            val docRef = submissionsCollection.document()
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val submissionWithId = submission.copy(id = docRef.id, uid = uid)
            docRef.set(submissionWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitLiterature(submission: LiteratureSubmissionDto): Result<Unit> {
        return try {
            val docRef = literatureSubmissionsCollection.document()
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val submissionWithId = submission.copy(id = docRef.id, uid = uid)
            docRef.set(submissionWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Backs the "Trusted Voice" / "Corpus Builder" badges: how many of this account's own
     * submissions (words plus literature) an admin has approved. Only reachable for this
     * account's own uid - see firestore.rules' owner-read exception on both collections.
     */
    suspend fun getApprovedSubmissionCount(): Result<Int> = try {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Result.success(0)
        } else {
            val words = submissionsCollection.whereEqualTo("uid", uid).whereEqualTo("status", "approved").get().await()
            val literature = literatureSubmissionsCollection.whereEqualTo("uid", uid).whereEqualTo("status", "approved").get().await()
            Result.success(words.size() + literature.size())
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
