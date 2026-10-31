package com.codx.recorder.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_events")
data class SyncEventEntity(
    @PrimaryKey val id: String,
    val eventType: String, // e.g. ALARM_FIRED, RECORDING_SAVED, UPLOAD_SUCCESS, REBOOT_RECOVERED
    val details: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS"
)
