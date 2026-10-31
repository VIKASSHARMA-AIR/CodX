package com.codx.recorder.data.remote.models

import com.google.gson.annotations.SerializedName

data class RegisterRecorderDto(
    @SerializedName("name") val name: String,
    @SerializedName("device_id") val deviceId: String? = null
)

data class DeviceAuthDto(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("name") val name: String,
    @SerializedName("pairing_code") val pairingCode: String?,
    @SerializedName("paired_with") val pairedWith: String?,
    @SerializedName("api_token") val apiToken: String
)

data class RecorderStatusDto(
    @SerializedName("recorder_id") val recorderId: String,
    @SerializedName("battery_pct") val batteryPct: Int,
    @SerializedName("is_charging") val isCharging: Boolean,
    @SerializedName("storage_free_bytes") val storageFreeBytes: Long,
    @SerializedName("storage_total_bytes") val storageTotalBytes: Long,
    @SerializedName("network_type") val networkType: String,
    @SerializedName("current_state") val currentState: String
)

data class ScheduleDto(
    @SerializedName("id") val id: String,
    @SerializedName("recorder_id") val recorderId: String,
    @SerializedName("label") val label: String,
    @SerializedName("start_time_str") val startTimeStr: String,
    @SerializedName("stop_time_str") val stopTimeStr: String,
    @SerializedName("days_of_week") val daysOfWeek: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("version") val version: Int
)

data class SessionCreateDto(
    @SerializedName("id") val id: String,
    @SerializedName("recorder_id") val recorderId: String,
    @SerializedName("schedule_id") val scheduleId: String?,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("status") val status: String
)

data class SessionEndDto(
    @SerializedName("ended_at") val endedAt: String,
    @SerializedName("status") val status: String,
    @SerializedName("error_message") val errorMessage: String? = null
)

data class RecordingResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("recorder_id") val recorderId: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("duration_ms") val durationMs: Long,
    @SerializedName("file_size_bytes") val fileSizeBytes: Long,
    @SerializedName("checksum_sha256") val checksumSha256: String,
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("stream_url") val streamUrl: String
)
