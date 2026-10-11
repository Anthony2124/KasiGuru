package com.kasiguru.util.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kasiguru.data.repository.CommunityUpdatesRepository
import com.kasiguru.util.AppVisibility
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Every few hours, with a connection: the learner's words approved or rejected, and words added,
 * edited or removed, as phone notifications that pop up even when the app is closed. While the app
 * is open the same news becomes the in-app pop-up instead. See [CommunityUpdatesRepository].
 */
class CommunityUpdatesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CommunityUpdatesEntryPoint {
        fun communityUpdatesRepository(): CommunityUpdatesRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(applicationContext, CommunityUpdatesEntryPoint::class.java)
            entryPoint.communityUpdatesRepository().checkAndDeliver(inForeground = AppVisibility.isForeground)
            Result.success()
        } catch (e: Exception) {
            Log.w("CommunityUpdatesWorker", "Community update check failed", e)
            Result.success()
        }
    }
}
