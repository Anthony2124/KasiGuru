package com.kasiguru.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an offline action queued for cloud synchronization.
 * Follows the offline-first SyncQueue strategy in the system architecture.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val payloadType: String,
    val payloadRef: String,
    val payloadData: String = "",
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val queuedAt: Long = System.currentTimeMillis()
)
