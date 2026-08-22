package com.kasiguru.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kasiguru.data.remote.model.AnnouncementDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live, in-app-only system announcements, following the same callbackFlow shape
 * [LeaderboardRepository] already uses for `leaderboard_public`. No Room cache: an announcement
 * that hasn't loaded yet is simply absent for a moment, which is fine for a banner that isn't
 * core functionality, unlike the leaderboard rankings that repository also has to serve offline.
 */
@Singleton
class AnnouncementRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getAnnouncements(): Flow<List<AnnouncementDto>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_ACTIVE, true)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                trySend(snapshot.toObjects(AnnouncementDto::class.java))
            }
        awaitClose { registration.remove() }
    }.catch { e ->
        Log.w(TAG, "announcements unavailable", e)
        emit(emptyList())
    }

    private companion object {
        const val TAG = "Announcements"
        const val COLLECTION = "announcements"
        const val FIELD_ACTIVE = "active"
        const val FIELD_CREATED_AT = "createdAt"
        const val LIMIT = 5L
    }
}
