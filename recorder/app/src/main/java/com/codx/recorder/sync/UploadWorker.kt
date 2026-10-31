package com.codx.recorder.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codx.recorder.data.local.CodXDatabase
import com.codx.recorder.data.repository.RecorderRepository
import com.codx.recorder.data.repository.SyncRepository
import com.codx.recorder.scheduler.AlarmScheduler

class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i("UploadWorker", "Starting background synchronization job...")

        val db = CodXDatabase.getDatabase(applicationContext)
        val repository = RecorderRepository(db)
        val syncRepository = SyncRepository(applicationContext, db, repository)

        return try {
            // 1. Ensure device is registered
            syncRepository.registerOrRefreshDevice()

            // 2. Push device health
            syncRepository.pushDeviceHealth()

            // 3. Sync schedules from server & update local alarms
            val schedulesUpdated = syncRepository.syncSchedules()
            if (schedulesUpdated) {
                val activeSchedules = repository.getActiveSchedules()
                for (schedule in activeSchedules) {
                    AlarmScheduler.schedule(applicationContext, schedule)
                }
            }

            // 4. Upload pending audio recordings
            val uploadedCount = syncRepository.uploadPendingRecordings()
            Log.i("UploadWorker", "Sync complete. Uploaded $uploadedCount recording(s)")

            Result.success()
        } catch (e: Exception) {
            Log.e("UploadWorker", "Sync worker encountered an error, scheduling retry", e)
            Result.retry()
        }
    }
}
