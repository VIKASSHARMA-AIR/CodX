package com.codx.controller.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_status")
data class CachedStatusEntity(
    @PrimaryKey val recorderId: String,
    val batteryPct: Int,
    val isCharging: Boolean,
    val storageFreeBytes: Long,
    val storageTotalBytes: Long,
    val networkType: String,
    val currentState: String,
    val isOnline: Boolean,
    val lastSeenStr: String
)

@Entity(tableName = "cached_schedules")
data class CachedScheduleEntity(
    @PrimaryKey val id: String,
    val recorderId: String,
    val label: String,
    val startTime: String,
    val stopTime: String,
    val daysOfWeek: String,
    val isActive: Boolean,
    val version: Int
)

@Entity(tableName = "cached_recordings")
data class CachedRecordingEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val recorderId: String,
    val filename: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val checksumSha256: String,
    val uploadedAt: String,
    val downloadUrl: String,
    val localFilePath: String? = null
)
