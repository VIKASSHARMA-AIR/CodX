package com.codx.recorder.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String, // Unique UUID guarantees deduplication
    val sessionId: String,
    val filename: String,
    val filePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val checksumSha256: String,
    val createdAt: Long = System.currentTimeMillis(),
    val uploadStatus: String = "PENDING", // PENDING, UPLOADING, UPLOADED, FAILED
    val uploadedAt: Long? = null
)
