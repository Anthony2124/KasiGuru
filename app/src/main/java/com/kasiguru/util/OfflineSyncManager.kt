package com.kasiguru.util

import com.kasiguru.data.local.dao.SyncQueueDao
import com.kasiguru.data.local.entity.SyncQueueEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline Sync Manager (Module 3.6 in System Architecture).
 * Manages the SyncQueue for offline-first data backup strategy.
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao
) {
    suspend fun enqueueAction(type: String, ref: String, data: String = "") {
        syncQueueDao.enqueue(
            SyncQueueEntity(
                payloadType = type,
                payloadRef = ref,
                payloadData = data
            )
        )
    }

    suspend fun processPendingSync(): Int {
        val pending = syncQueueDao.getPendingItems()
        pending.forEach { item ->
            // Perform sync item status update
            syncQueueDao.update(item.copy(status = "SYNCED"))
        }
        syncQueueDao.purgeSynced()
        return pending.size
    }
}
