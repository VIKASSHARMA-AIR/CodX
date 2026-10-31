package com.codx.controller.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codx.controller.data.local.ControllerDatabase
import com.codx.controller.data.local.entities.CachedRecordingEntity
import com.codx.controller.data.local.entities.CachedScheduleEntity
import com.codx.controller.data.local.entities.CachedStatusEntity
import com.codx.controller.data.remote.RetrofitClient
import com.codx.controller.data.remote.models.CreateScheduleDto
import com.codx.controller.data.remote.models.UpdateScheduleDto
import com.codx.controller.data.repository.ControllerRepository
import com.codx.controller.playback.AudioPlayerManager
import com.codx.controller.playback.PlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ControllerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ControllerDatabase.getDatabase(application)
    val sessionManager = com.codx.controller.auth.UserSessionManager(application)
    val authManager = com.codx.controller.auth.FirebaseAuthManager(application, sessionManager)
    val repository = ControllerRepository(application, db)
    val audioPlayerManager = AudioPlayerManager(application)

    val userSession: StateFlow<com.codx.controller.auth.UserSession> = sessionManager.sessionState

    private val _pairedRecorderId = MutableStateFlow(repository.pairedRecorderId)
    val pairedRecorderId: StateFlow<String?> = _pairedRecorderId.asStateFlow()

    private val _pairedRecorderName = MutableStateFlow(repository.pairedRecorderName)
    val pairedRecorderName: StateFlow<String?> = _pairedRecorderName.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = audioPlayerManager.playbackState

    val statusFlow: StateFlow<CachedStatusEntity?> = _pairedRecorderId
        .flatMapLatest { id ->
            if (id != null) repository.getCachedStatus(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val schedulesFlow: StateFlow<List<CachedScheduleEntity>> = _pairedRecorderId
        .flatMapLatest { id ->
            if (id != null) repository.getCachedSchedules(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordingsFlow: StateFlow<List<CachedRecordingEntity>> = _pairedRecorderId
        .flatMapLatest { id ->
            if (id != null) repository.getCachedRecordings(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pollJob: Job? = null

    init {
        startPollingStatus()
    }

    private fun startPollingStatus() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                if (_pairedRecorderId.value != null) {
                    refreshAllData(silent = true)
                }
                delay(5000) // Poll recorder status every 5 seconds
            }
        }
    }

    fun refreshAllData(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _isRefreshing.value = true
            repository.fetchStatus()
            repository.fetchSchedules()
            repository.fetchRecordings()
            if (!silent) _isRefreshing.value = false
        }
    }

    fun pairWithRecorder(pairingCode: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.pairWithRecorder(pairingCode)
            _isRefreshing.value = false

            if (result.isSuccess) {
                val data = result.getOrThrow()
                _pairedRecorderId.value = data.recorderId
                _pairedRecorderName.value = data.recorderName
                refreshAllData()
                onComplete(true, "Successfully paired with ${data.recorderName}")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Pairing failed"
                onComplete(false, err)
            }
        }
    }

    fun createSchedule(label: String, start: String, stop: String, days: String) {
        viewModelScope.launch {
            val dto = CreateScheduleDto(
                label = label,
                startTimeStr = start,
                stopTimeStr = stop,
                daysOfWeek = days,
                isActive = true
            )
            repository.createSchedule(dto)
        }
    }

    fun toggleSchedule(scheduleId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateSchedule(scheduleId, UpdateScheduleDto(isActive = isActive))
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            repository.deleteSchedule(scheduleId)
        }
    }

    fun playRecording(recording: CachedRecordingEntity) {
        val streamUrl = "${RetrofitClient.getBaseUrl()}api/recordings/${recording.id}/stream"
        audioPlayerManager.playStream(
            url = streamUrl,
            recordingId = recording.id,
            title = recording.filename
        )
    }

    fun downloadRecording(recording: CachedRecordingEntity) {
        viewModelScope.launch {
            _statusMessage.value = "Downloading ${recording.filename}..."
            val result = repository.downloadAudioFile(recording.id, recording.filename)
            if (result.isSuccess) {
                _statusMessage.value = "Downloaded ${recording.filename} for offline playback"
            } else {
                _statusMessage.value = "Download failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateServerUrl(url: String) {
        RetrofitClient.setBaseUrl(url)
        refreshAllData()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.release()
    }
}
