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
            val snapshot = releasesCollection
                .orderBy("versionCode", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val release = snapshot.documents.first().toObject(AppReleaseDto::class.java)
                Result.success(release)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
