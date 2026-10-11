package com.kasiguru.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.kasiguru.data.remote.model.LiteratureSubmissionDto
import com.kasiguru.data.remote.model.SentenceSubmissionDto
import com.kasiguru.data.remote.model.WordSubmissionDto
import com.kasiguru.domain.contribute.CommunityUpdates
import com.kasiguru.domain.contribute.SubmissionState
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import java.time.ZoneId

/**
 * An approved submission and what its approval is worth: [CommunityUpdates.APPROVAL_XP] for words
 * the portal approved from 1.28 on, [CommunityUpdates.LEGACY_APPROVAL_XP] for everything else.
 */
data class ApprovedContribution(
    val id: String,
    val approvedDay: String?,
    val xp: Int = CommunityUpdates.LEGACY_APPROVAL_XP
)

/** One of this learner's word submissions, in any status, with the day it was approved if it was. */
data class OwnWordSubmission(val state: SubmissionState, val approvedDay: String?)

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

    /** Sends an example sentence for review. Written as a plain map so its keys are exactly the rule's. */
    suspend fun submitSentence(submission: SentenceSubmissionDto): Result<Unit> = runCatching {
        val docRef = firestore.collection("sentence_submissions").document()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        docRef.set(
            mapOf(
                "id" to docRef.id,
                "kasiguranin" to submission.kasiguranin.trim().take(80),
                "english" to submission.english.trim().take(300),
                "sentence" to submission.sentence.trim(),
                "translation" to submission.translation.trim(),
                "contributorName" to submission.contributorName.trim().take(60),
                "status" to "pending",
                "submittedAt" to System.currentTimeMillis(),
                "uid" to uid
            )
        ).await()
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

    suspend fun getApprovedSubmissions(): Result<List<ApprovedContribution>> = runCatching {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@runCatching emptyList()
        val words = submissionsCollection.whereEqualTo("uid",uid).whereEqualTo("status","approved").get().await()
        val literature = literatureSubmissionsCollection.whereEqualTo("uid",uid).whereEqualTo("status","approved").get().await()
        words.documents.map { ApprovedContribution("word:${it.id}", approvedDayOf(it), CommunityUpdates.approvalXp(it.get("rewardXp"))) } +
            literature.documents.map { ApprovedContribution("literature:${it.id}", approvedDayOf(it)) }
    }

    /**
     * Every word this account has sent, whatever its status, for the approved / rejected notices.
     * Owner-only reads, like the counts above: the query must name this uid for the rules to allow it.
     */
    suspend fun getOwnWordSubmissions(): Result<List<OwnWordSubmission>> = runCatching {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@runCatching emptyList()
        submissionsCollection.whereEqualTo("uid", uid).get().await().documents.map { doc ->
            OwnWordSubmission(
                state = SubmissionState(
                    id = doc.id,
                    word = doc.getString("kasiguranin").orEmpty(),
                    status = doc.getString("status") ?: "pending",
                    rewardXp = CommunityUpdates.approvalXp(doc.get("rewardXp"))
                ),
                approvedDay = approvedDayOf(doc)
            )
        }
    }

    /** The local day a submission was approved, from the portal's approvedAt or reviewedAt stamp. */
    private fun approvedDayOf(doc: DocumentSnapshot): String? {
        val millis = when (val timestamp = doc.get("approvedAt") ?: doc.get("reviewedAt")) {
            is Number -> timestamp.toLong()
            is com.google.firebase.Timestamp -> timestamp.toDate().time
            else -> null
        }
        return millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString() }
    }
}
