package com.codx.recorder.recording

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.codx.recorder.data.local.CodXDatabase
import com.codx.recorder.data.local.entities.RecordingEntity
import com.codx.recorder.data.local.entities.RecordingSessionEntity
import com.codx.recorder.data.local.entities.SyncQueueEntity
import com.codx.recorder.data.repository.RecorderRepository
import com.codx.recorder.sync.DeviceHealthMonitor
import com.codx.recorder.sync.UploadWorker
import kotlinx.coroutines.*
import java.util.UUID

class RecordingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var audioRecorderManager: AudioRecorderManager
    private lateinit var repository: RecorderRepository
    private var wakeLock: PowerManager.WakeLock? = null

    private var currentSessionId: String? = null
    private var currentScheduleId: String? = null

    companion object {
        const val ACTION_START_RECORDING = "com.codx.recorder.action.START"
        const val ACTION_STOP_RECORDING = "com.codx.recorder.action.STOP"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_SESSION_ID = "extra_session_id"

        const val NOTIFICATION_CHANNEL_ID = "codx_recording_channel"
        const val NOTIFICATION_ID = 1001

        var isServiceRunning = false
            private set

        fun startService(context: Context, scheduleId: String? = null, sessionId: String? = null) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(EXTRA_SESSION_ID, sessionId ?: UUID.randomUUID().toString())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = CodXDatabase.getDatabase(applicationContext)
        repository = RecorderRepository(db)
        audioRecorderManager = AudioRecorderManager(applicationContext)
        acquireWakeLock()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID)
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: UUID.randomUUID().toString()
                startForegroundRecording(sessionId, scheduleId)
            }
            ACTION_STOP_RECORDING -> {
                stopForegroundRecording("MANUAL_OR_ALARM_STOP")
            }
        }
        return START_STICKY
    }

    private fun startForegroundRecording(sessionId: String, scheduleId: String?) {
        if (isServiceRunning) return

        this.currentSessionId = sessionId
        this.currentScheduleId = scheduleId
        isServiceRunning = true

        val notification = createNotification("Recording audio in progress...")
        startForeground(NOTIFICATION_ID, notification)

        // Pre-check battery and storage
        val health = DeviceHealthMonitor.getHealth(applicationContext)
        if (health.batteryPct < 5 && !health.isCharging) {
            Log.w("RecordingService", "Battery critically low (${health.batteryPct}%). Aborting start.")
            stopForegroundRecording("LOW_BATTERY_ABORT")
            return
        }

        val startResult = audioRecorderManager.startRecording(sessionId)
        if (startResult.isSuccess) {
            serviceScope.launch {
                val session = RecordingSessionEntity(
                    id = sessionId,
                    scheduleId = scheduleId,
                    startedAt = System.currentTimeMillis(),
                    endedAt = null,
                    status = "RECORDING"
                )
                repository.createSession(session)
                repository.logSyncEvent("RECORDING_STARTED", "Session: $sessionId, Schedule: $scheduleId")
            }
        } else {
            Log.e("RecordingService", "Failed to start MediaRecorder")
            stopForegroundRecording("RECORDER_INIT_FAILED")
        }
    }

    private fun stopForegroundRecording(reason: String) {
        if (!isServiceRunning) {
            stopSelf()
            return
        }

        val stopResult = audioRecorderManager.stopRecording()
        val sessionId = currentSessionId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        serviceScope.launch {
            if (stopResult.isSuccess) {
                val res = stopResult.getOrThrow()
                val recording = RecordingEntity(
                    id = res.id,
                    sessionId = sessionId,
                    filename = res.file.name,
                    filePath = res.file.absolutePath,
                    durationMs = res.durationMs,
                    fileSizeBytes = res.fileSizeBytes,
                    checksumSha256 = res.checksumSha256,
                    createdAt = now,
                    uploadStatus = "PENDING"
                )
                repository.insertRecording(recording)

                // Enqueue upload task in SyncQueue
                val syncTask = SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "AUDIO_FILE",
                    entityId = res.id,
                    action = "UPLOAD",
                    payload = res.file.absolutePath
                )
                repository.enqueueSyncTask(syncTask)

                repository.completeSession(sessionId, now, "COMPLETED")
                repository.logSyncEvent("RECORDING_STOPPED", "File: ${res.file.name}, Duration: ${res.durationMs}ms ($reason)")

                // Trigger background upload worker immediately
                triggerUploadWorker()
            } else {
                repository.completeSession(sessionId, now, "FAILED", stopResult.exceptionOrNull()?.message)
                repository.logSyncEvent("RECORDING_FAILED", stopResult.exceptionOrNull()?.message, status = "FAILED")
            }

            releaseWakeLock()
            isServiceRunning = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun triggerUploadWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "codx_upload_work",
            ExistingWorkPolicy.REPLACE,
            uploadWorkRequest
        )
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CodX:RecordingWakeLock").apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L) // 4 hours max safeguard
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("RecordingService", "Error releasing WakeLock", e)
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "CodX Active Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing offline background audio recording"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("CodX Recorder Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop Recording", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
