package com.codx.recorder.data.repository

import android.content.Context
import com.codx.recorder.data.local.CodXDatabase
import com.codx.recorder.data.local.entities.DeviceEntity
import com.codx.recorder.data.local.entities.ScheduleEntity
import com.codx.recorder.data.remote.RetrofitClient
import com.codx.recorder.data.remote.models.RecorderStatusDto
import com.codx.recorder.data.remote.models.RegisterRecorderDto
import com.codx.recorder.sync.DeviceHealthMonitor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

class SyncRepository(
    private val context: Context,
    private val db: CodXDatabase,
    private val repository: RecorderRepository
) {
    private val api = RetrofitClient.getService()

    /**
     * Ensures Recorder is registered and has a pairing code.
     */
    suspend fun registerOrRefreshDevice(deviceName: String = "CodX Recorder"): DeviceEntity? {
        var localDevice = repository.getDevice()
        try {
            val deviceId = localDevice?.id ?: UUID.randomUUID().toString()
            val response = api.registerRecorder(RegisterRecorderDto(name = deviceName, deviceId = deviceId))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val entity = DeviceEntity(
                    id = body.deviceId,
                    deviceType = body.deviceType,
                    name = body.name,
                    pairingCode = body.pairingCode,
                    pairedWith = body.pairedWith,
                    apiToken = body.apiToken,
                    lastSeenTimestamp = System.currentTimeMillis()
                )
                repository.saveDevice(entity)
                repository.logSyncEvent("DEVICE_REGISTERED", "Pairing code: ${body.pairingCode}")
                return entity
            }
        } catch (e: Exception) {
            repository.logSyncEvent("DEVICE_REGISTER_FAIL", e.message, status = "FAILED")
        }
        return localDevice
    }

    /**
     * Pulls latest schedules from the server into the local offline Room database.
     */
    suspend fun syncSchedules(): Boolean {
        val device = repository.getDevice() ?: return false
        try {
            val response = api.getSchedules(device.id)
            if (response.isSuccessful && response.body() != null) {
                val dtoList = response.body()!!
                val entities = dtoList.map { dto ->
                    ScheduleEntity(
                        id = dto.id,
                        label = dto.label,
                        startTime = dto.startTimeStr,
                        stopTime = dto.stopTimeStr,
                        daysOfWeek = dto.daysOfWeek,
                        isActive = dto.isActive,
                        version = dto.version,
                        syncedAt = System.currentTimeMillis()
                    )
                }
                db.scheduleDao().insertAll(entities)
                repository.logSyncEvent("SCHEDULE_SYNC", "Downloaded ${entities.size} schedules")
                return true
            }
        } catch (e: Exception) {
            repository.logSyncEvent("SCHEDULE_SYNC_FAIL", e.message, status = "FAILED")
        }
        return false
    }

    /**
     * Sends local device health (battery, charging, storage) to cloud.
     */
    suspend fun pushDeviceHealth(): Boolean {
        val device = repository.getDevice() ?: return false
        val health = DeviceHealthMonitor.getHealth(context)

        try {
            val dto = RecorderStatusDto(
                recorderId = device.id,
                batteryPct = health.batteryPct,
                isCharging = health.isCharging,
                storageFreeBytes = health.storageFreeBytes,
                storageTotalBytes = health.storageTotalBytes,
                networkType = health.networkType,
                currentState = health.currentState
            )
            val response = api.updateStatus(dto)
            if (response.isSuccessful) {
                repository.updateDeviceStatus(
                    com.codx.recorder.data.local.entities.DeviceStatusEntity(
                        id = "LOCAL_DEVICE",
                        batteryPct = health.batteryPct,
                        isCharging = health.isCharging,
                        storageFreeBytes = health.storageFreeBytes,
                        storageTotalBytes = health.storageTotalBytes,
                        connectivity = health.networkType,
                        currentState = health.currentState,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                return true
            }
        } catch (e: Exception) {
            // Offline - no worries, recorded in local db
        }
        return false
    }

    /**
     * Uploads all pending recordings to cloud server.
     */
    suspend fun uploadPendingRecordings(): Int {
        val device = repository.getDevice() ?: return 0
        val pendingList = repository.getPendingUploads()
        var uploadedCount = 0

        for (rec in pendingList) {
            val file = File(rec.filePath)
            if (!file.exists()) {
                repository.updateRecordingUploadStatus(rec.id, "FAILED")
                continue
            }

            try {
                repository.updateRecordingUploadStatus(rec.id, "UPLOADING")

                val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val idPart = rec.id.toRequestBody(MultipartBody.FORM)
                val sessionPart = rec.sessionId.toRequestBody(MultipartBody.FORM)
                val recorderPart = device.id.toRequestBody(MultipartBody.FORM)
                val durationPart = rec.durationMs.toString().toRequestBody(MultipartBody.FORM)
                val checksumPart = rec.checksumSha256.toRequestBody(MultipartBody.FORM)

                val response = api.uploadRecording(
                    id = idPart,
                    sessionId = sessionPart,
                    recorderId = recorderPart,
                    durationMs = durationPart,
                    checksumSha256 = checksumPart,
                    file = filePart
                )

                if (response.isSuccessful) {
                    repository.updateRecordingUploadStatus(rec.id, "UPLOADED", System.currentTimeMillis())
                    repository.logSyncEvent("AUDIO_UPLOAD_SUCCESS", "Uploaded ${rec.filename}")
                    uploadedCount++
                } else {
                    repository.updateRecordingUploadStatus(rec.id, "FAILED")
                    repository.logSyncEvent("AUDIO_UPLOAD_ERROR", "Server returned ${response.code()}", status = "FAILED")
                }
            } catch (e: Exception) {
                repository.updateRecordingUploadStatus(rec.id, "FAILED")
                repository.logSyncEvent("AUDIO_UPLOAD_EXCEPTION", e.message, status = "FAILED")
            }
        }
        return uploadedCount
    }
}
