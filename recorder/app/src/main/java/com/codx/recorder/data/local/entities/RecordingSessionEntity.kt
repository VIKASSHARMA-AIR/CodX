package com.codx.recorder.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_sessions")
data class RecordingSessionEntity(
    @PrimaryKey val id: String,
    val scheduleId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String, // RECORDING, COMPLETED, INTERRUPTED, FAILED
    val errorMessage: String? = null
)
