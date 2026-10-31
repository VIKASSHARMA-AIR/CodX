package com.codx.recorder.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codx.recorder.data.local.CodXDatabase
import com.codx.recorder.data.local.entities.*
import com.codx.recorder.data.remote.RetrofitClient
import com.codx.recorder.data.repository.RecorderRepository
import com.codx.recorder.data.repository.SyncRepository
import com.codx.recorder.recording.RecordingService
import com.codx.recorder.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class RecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CodXDatabase.getDatabase(application)
    private val repository = RecorderRepository(db)
    private val syncRepository = SyncRepository(application, db, repository)

    val deviceFlow: StateFlow<DeviceEntity?> = repository.deviceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val deviceStatusFlow: StateFlow<DeviceStatusEntity?> = repository.deviceStatusFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val schedulesFlow: StateFlow<List<ScheduleEntity>> = repository.schedulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordingsFlow: StateFlow<List<RecordingEntity>> = repository.recordingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncEventsFlow: StateFlow<List<SyncEventEntity>> = repository.syncEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncQueueFlow: StateFlow<List<SyncQueueEntity>> = repository.syncQueueFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Register or refresh device on startup
        viewModelScope.launch {
            syncRepository.registerOrRefreshDevice()
            syncRepository.pushDeviceHealth()
        }
    }

    fun startManualRecording() {
        val sessionId = UUID.randomUUID().toString()
        RecordingService.startService(getApplication(), sessionId = sessionId)
    }

    fun stopManualRecording() {
        RecordingService.stopService(getApplication())
    }

    fun toggleSchedule(id: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.setScheduleActive(id, isActive)
            val schedule = db.scheduleDao().getScheduleById(id)
            if (schedule != null) {
                if (isActive) {
                    AlarmScheduler.schedule(getApplication(), schedule)
                } else {
                    AlarmScheduler.cancel(getApplication(), schedule)
                }
            }
        }
    }

    fun addSchedule(label: String, startTime: String, stopTime: String, daysOfWeek: String) {
        viewModelScope.launch {
            val newSchedule = ScheduleEntity(
                id = UUID.randomUUID().toString(),
                label = label,
                startTime = startTime,
                stopTime = stopTime,
                daysOfWeek = daysOfWeek,
                isActive = true,
                version = 1,
                syncedAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateSchedule(newSchedule)
            AlarmScheduler.schedule(getApplication(), newSchedule)
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            val schedule = db.scheduleDao().getScheduleById(id)
            if (schedule != null) {
                AlarmScheduler.cancel(getApplication(), schedule)
                repository.deleteSchedule(id)
            }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            syncRepository.syncSchedules()
            syncRepository.pushDeviceHealth()
            syncRepository.uploadPendingRecordings()
        }
    }

    fun updateServerUrl(url: String) {
        RetrofitClient.setBaseUrl(url)
        triggerManualSync()
    }
}
