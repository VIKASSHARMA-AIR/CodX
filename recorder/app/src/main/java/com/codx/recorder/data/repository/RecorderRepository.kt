package com.codx.recorder.data.repository

import com.codx.recorder.data.local.CodXDatabase
import com.codx.recorder.data.local.entities.*
import kotlinx.coroutines.flow.Flow

class RecorderRepository(private val db: CodXDatabase) {

    // Schedules
    val schedulesFlow: Flow<List<ScheduleEntity>> = db.scheduleDao().getAllSchedulesFlow()

    suspend fun getActiveSchedules(): List<ScheduleEntity> = db.scheduleDao().getActiveSchedules()

    suspend fun insertOrUpdateSchedule(schedule: ScheduleEntity) = db.scheduleDao().insertOrUpdate(schedule)

    suspend fun setScheduleActive(id: String, isActive: Boolean) = db.scheduleDao().setScheduleActive(id, isActive)

    suspend fun deleteSchedule(id: String) = db.scheduleDao().deleteById(id)

    // Recordings & Sessions
    val recordingsFlow: Flow<List<RecordingEntity>> = db.recordingDao().getAllRecordingsFlow()

    suspend fun insertRecording(recording: RecordingEntity) = db.recordingDao().insert(recording)

    suspend fun getPendingUploads(): List<RecordingEntity> = db.recordingDao().getPendingUploads()

    suspend fun updateRecordingUploadStatus(id: String, status: String, uploadedAt: Long? = null) {
        db.recordingDao().updateUploadStatus(id, status, uploadedAt)
    }

    suspend fun createSession(session: RecordingSessionEntity) = db.sessionDao().insertOrUpdate(session)

    suspend fun completeSession(id: String, endedAt: Long, status: String, errorMessage: String? = null) {
        db.sessionDao().completeSession(id, endedAt, status, errorMessage)
    }

    suspend fun getActiveSession(): RecordingSessionEntity? = db.sessionDao().getActiveSession()

    // Device & Status
    val deviceFlow: Flow<DeviceEntity?> = db.deviceDao().getLocalDeviceFlow()

    suspend fun getDevice(): DeviceEntity? = db.deviceDao().getLocalDevice()

    suspend fun saveDevice(device: DeviceEntity) = db.deviceDao().insertOrUpdate(device)

    val deviceStatusFlow: Flow<DeviceStatusEntity?> = db.deviceStatusDao().getStatusFlow()

    suspend fun updateDeviceStatus(status: DeviceStatusEntity) = db.deviceStatusDao().updateStatus(status)

    // Sync Events & Queue
    val syncEventsFlow: Flow<List<SyncEventEntity>> = db.syncEventDao().getAllEventsFlow()

    suspend fun logSyncEvent(eventType: String, details: String?, status: String = "SUCCESS") {
        val event = SyncEventEntity(
            id = java.util.UUID.randomUUID().toString(),
            eventType = eventType,
            details = details,
            timestamp = System.currentTimeMillis(),
            status = status
        )
        db.syncEventDao().insertEvent(event)
    }

    val syncQueueFlow: Flow<List<SyncQueueEntity>> = db.syncQueueDao().getAllQueueFlow()

    suspend fun enqueueSyncTask(task: SyncQueueEntity) = db.syncQueueDao().enqueue(task)

    suspend fun getPendingSyncTasks(): List<SyncQueueEntity> = db.syncQueueDao().getPendingTasks()

    suspend fun removeSyncTask(id: String) = db.syncQueueDao().deleteTask(id)
}
