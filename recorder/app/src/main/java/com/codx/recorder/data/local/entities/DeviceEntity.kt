package com.codx.recorder.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val deviceType: String, // RECORDER or CONTROLLER
    val name: String,
    val pairingCode: String?,
    val pairedWith: String?,
    val apiToken: String,
    val lastSeenTimestamp: Long
)
