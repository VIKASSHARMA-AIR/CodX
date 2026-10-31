package com.codx.controller.data.repository

import android.content.Context
import com.codx.controller.data.local.ControllerDatabase
import com.codx.controller.data.local.entities.CachedRecordingEntity
import com.codx.controller.data.local.entities.CachedScheduleEntity
import com.codx.controller.data.local.entities.CachedStatusEntity
import com.codx.controller.data.remote.RetrofitClient
import com.codx.controller.data.remote.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ControllerRepository(
    private val context: Context,
    private val db: ControllerDatabase
) {
    private val api = RetrofitClient.getService()
    private val prefs = context.getSharedPreferences("codx_controller_prefs", Context.MODE_PRIVATE)

    var controllerId: String
        get() = prefs.getString("controller_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("controller_id", it).apply()
        }
        set(value) = prefs.edit().putString("controller_id", value).apply()

    var pairedRecorderId: String?
        get() = prefs.getString("paired_recorder_id", null)
        set(value) = prefs.edit().putString("paired_recorder_id", value).apply()

    var pairedRecorderName: String?
        get() = prefs.getString("paired_recorder_name", null)
        set(value) = prefs.edit().putString("paired_recorder_name", value).apply()

    suspend fun pairWithRecorder(code: String): Result<PairResponseDto> {
        return try {
            // First register controller if not registered
            api.registerController(RegisterControllerDto(name = "CodX Controller Phone", deviceId = controllerId))

            val pairReq = PairDeviceDto(controllerId = controllerId, pairingCode = code.trim().uppercase())
            val response = api.pairWithRecorder(pairReq)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                pairedRecorderId = body.recorderId
                pairedRecorderName = body.recorderName
                Result.success(body)
            } else {
                Result.failure(Exception("Pairing failed: ${response.code()} ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchStatus(): Result<RecorderStatusDto> {
        val recorderId = pairedRecorderId ?: return Result.failure(IllegalStateException("No recorder paired"))
        return try {
            val response = api.getRecorderStatus(recorderId)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                db.controllerDao().saveStatus(
                    CachedStatusEntity(
                        recorderId = dto.recorderId,
                        batteryPct = dto.batteryPct,
                        isCharging = dto.isCharging,
                        storageFreeBytes = dto.storageFreeBytes,
                        storageTotalBytes = dto.storageTotalBytes,
                        networkType = dto.networkType,
                        currentState = dto.currentState,
                        isOnline = dto.isOnline,
                        lastSeenStr = dto.lastSeen
                    )
                )
                Result.success(dto)
            } else {
                Result.failure(Exception("Failed to fetch status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedStatus(recorderId: String): Flow<CachedStatusEntity?> =
        db.controllerDao().getStatusFlow(recorderId)

    suspend fun fetchSchedules(): Result<List<ScheduleDto>> {
        val recorderId = pairedRecorderId ?: return Result.failure(IllegalStateException("No recorder paired"))
        return try {
            val response = api.getSchedules(recorderId)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                val cached = list.map {
                    CachedScheduleEntity(
                        id = it.id,
                        recorderId = it.recorderId,
                        label = it.label,
                        startTime = it.startTimeStr,
                        stopTime = it.stopTimeStr,
                        daysOfWeek = it.daysOfWeek,
                        isActive = it.isActive,
                        version = it.version
                    )
                }
                db.controllerDao().saveSchedules(cached)
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to fetch schedules"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedSchedules(recorderId: String): Flow<List<CachedScheduleEntity>> =
        db.controllerDao().getSchedulesFlow(recorderId)

    suspend fun createSchedule(createDto: CreateScheduleDto): Result<ScheduleDto> {
        val recorderId = pairedRecorderId ?: return Result.failure(IllegalStateException("No recorder paired"))
        return try {
            val response = api.createSchedule(recorderId, createDto)
            if (response.isSuccessful && response.body() != null) {
                fetchSchedules() // refresh
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create schedule"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSchedule(scheduleId: String, updateDto: UpdateScheduleDto): Result<ScheduleDto> {
        val recorderId = pairedRecorderId ?: return Result.failure(IllegalStateException("No recorder paired"))
        return try {
            val response = api.updateSchedule(recorderId, scheduleId, updateDto)
            if (response.isSuccessful && response.body() != null) {
                fetchSchedules()
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update schedule"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        val recorderId = pairedRecorderId ?: return Result.failure(IllegalStateException("No recorder paired"))
        return try {
            val response = api.deleteSchedule(recorderId, scheduleId)
            if (response.isSuccessful) {
                db.controllerDao().deleteSchedule(scheduleId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete schedule"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRecordings(): Result<List<RecordingItemDto>> {
        val recorderId = pairedRecorderId ?: return Result.failure(IllegalStateException("No recorder paired"))
        return try {
            val response = api.listRecordings(recorderId)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                val cached = list.map {
                    CachedRecordingEntity(
                        id = it.id,
                        sessionId = it.sessionId,
                        recorderId = it.recorderId,
                        filename = it.filename,
                        durationMs = it.durationMs,
                        fileSizeBytes = it.fileSizeBytes,
                        checksumSha256 = it.checksumSha256,
                        uploadedAt = it.uploadedAt,
                        downloadUrl = it.downloadUrl
                    )
                }
                db.controllerDao().saveRecordings(cached)
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to fetch recordings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedRecordings(recorderId: String): Flow<List<CachedRecordingEntity>> =
        db.controllerDao().getRecordingsFlow(recorderId)

    suspend fun downloadAudioFile(recordingId: String, filename: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val response = api.downloadRecording(recordingId)
            if (response.isSuccessful && response.body() != null) {
                val downloadsDir = File(context.filesDir, "downloaded_recordings")
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                val localFile = File(downloadsDir, filename)
                response.body()!!.byteStream().use { input ->
                    FileOutputStream(localFile).use { output ->
                        input.copyTo(output)
                    }
                }
                db.controllerDao().updateLocalPath(recordingId, localFile.absolutePath)
                Result.success(localFile)
            } else {
                Result.failure(Exception("Download failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
