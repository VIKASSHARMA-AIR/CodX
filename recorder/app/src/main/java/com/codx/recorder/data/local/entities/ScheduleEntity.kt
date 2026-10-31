package com.codx.recorder.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val label: String,
    val startTime: String, // "08:30"
    val stopTime: String,  // "09:30"
    val daysOfWeek: String, // "DAILY", "MON,WED,FRI"
    val isActive: Boolean = true,
    val version: Int = 1,
    val syncedAt: Long = System.currentTimeMillis()
)
