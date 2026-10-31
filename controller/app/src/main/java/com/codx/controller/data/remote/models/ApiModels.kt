package com.codx.controller.data.remote.models

import com.google.gson.annotations.SerializedName

data class GoogleLoginRequestDto(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("photo_url") val photoUrl: String? = null
)

data class UserAuthResponseDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("firebase_uid") val firebaseUid: String,
    @SerializedName("email") val email: String,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("api_token") val apiToken: String
)

data class DeleteAccountResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

data class RegisterControllerDto(
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

data class PairDeviceDto(
    @SerializedName("controller_id") val controllerId: String,
    @SerializedName("pairing_code") val pairingCode: String
)

data class PairResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("recorder_id") val recorderId: String,
    @SerializedName("recorder_name") val recorderName: String
)

data class RecorderStatusDto(
    @SerializedName("recorder_id") val recorderId: String,
    @SerializedName("battery_pct") val batteryPct: Int,
    @SerializedName("is_charging") val isCharging: Boolean,
    @SerializedName("storage_free_bytes") val storageFreeBytes: Long,
    @SerializedName("storage_total_bytes") val storageTotalBytes: Long,
    @SerializedName("network_type") val networkType: String,
    @SerializedName("current_state") val currentState: String,
    @SerializedName("is_online") val isOnline: Boolean,
    @SerializedName("last_seen") val lastSeen: String
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

data class CreateScheduleDto(
    @SerializedName("label") val label: String,
    @SerializedName("start_time_str") val startTimeStr: String,
    @SerializedName("stop_time_str") val stopTimeStr: String,
    @SerializedName("days_of_week") val daysOfWeek: String = "DAILY",
    @SerializedName("is_active") val isActive: Boolean = true
)

data class UpdateScheduleDto(
    @SerializedName("label") val label: String? = null,
    @SerializedName("start_time_str") val startTimeStr: String? = null,
    @SerializedName("stop_time_str") val stopTimeStr: String? = null,
    @SerializedName("days_of_week") val daysOfWeek: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)

data class RecordingItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("recorder_id") val recorderId: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("duration_ms") val durationMs: Long,
    @SerializedName("file_size_bytes") val fileSizeBytes: Long,
    @SerializedName("checksum_sha256") val checksumSha256: String,
    @SerializedName("uploaded_at") val uploadedAt: String,
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("stream_url") val streamUrl: String
)
