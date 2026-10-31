package com.codx.recorder.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String, // AUDIO_FILE, STATUS_HEARTBEAT, SESSION_LOG
    val entityId: String,
    val action: String, // UPLOAD, SYNC
    val payload: String?, // Additional parameters or serialized json
    val retryCount: Int = 0,
    val nextRetryTimestamp: Long = System.currentTimeMillis(),
    val status: String = "QUEUED", // QUEUED, PROCESSING, FAILED, COMPLETED
    val createdAt: Long = System.currentTimeMillis()
)
