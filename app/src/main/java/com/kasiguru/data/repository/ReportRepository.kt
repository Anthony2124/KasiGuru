package com.kasiguru.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kasiguru.data.remote.model.IssueReportDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val reportsCollection = firestore.collection("issue_reports")

    suspend fun submitReport(report: IssueReportDto): Result<String> {
        return try {
            val docRef = reportsCollection.document()
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val reportWithId = report.copy(
                id = docRef.id,
                uid = uid,
                submittedAt = System.currentTimeMillis(),
                status = "pending"
            )
            docRef.set(reportWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe reports submitted by the current authenticated user.
     */
    fun getMyReports(): Flow<List<IssueReportDto>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = reportsCollection
            .whereEqualTo("uid", uid)
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // Per document, inside the listener where a throw would crash the app: a report sent
                // from the web app carries an `id` field, which IssueReportDto's @DocumentId cannot map.
                val reports = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { doc.toObject(IssueReportDto::class.java) }.getOrNull()
                }
                trySend(reports)
            }

        awaitClose { listener.remove() }
    }
}
