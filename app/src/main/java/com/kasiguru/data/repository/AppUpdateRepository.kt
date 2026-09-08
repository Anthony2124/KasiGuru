package com.kasiguru.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.remote.model.AppReleaseDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val releasesCollection = firestore.collection("app_releases")

    suspend fun getLatestRelease(): Result<AppReleaseDto?> {
        return try {
            // Fetch a small window of recent releases rather than just the top one, and return the
            // newest that has not been yanked in the admin release manager. A composite index would
            // be needed to filter `yanked` server-side; there is never a long run of yanked builds,
            // so filtering client-side over a bounded window is simpler and costs a handful of reads.
            val snapshot = releasesCollection
                .orderBy("versionCode", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(15)
                .get()
                .await()

            val release = snapshot.documents
                .asSequence()
                .mapNotNull { it.toObject(AppReleaseDto::class.java) }
                .firstOrNull { !it.yanked }
            Result.success(release)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
