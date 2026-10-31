package com.codx.recorder.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_status")
data class DeviceStatusEntity(
    @PrimaryKey val id: String = "LOCAL_DEVICE",
    val batteryPct: Int,
    val isCharging: Boolean,
    val storageFreeBytes: Long,
    val storageTotalBytes: Long,
    val connectivity: String,
    val currentState: String, // IDLE, RECORDING, SYNCING, ERROR
    val updatedAt: Long = System.currentTimeMillis()
)
