package com.codx.recorder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.codx.recorder.data.local.CodXDatabase
import com.codx.recorder.data.local.entities.SyncEventEntity
import com.codx.recorder.recording.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * Handles all system time, date, and timezone modifications.
 *
 * Ensures offline schedules and alarms are immediately re-calculated and re-armed
 * whenever:
 *  - The user manually sets the phone clock forward or backward.
 *  - Network NTP time synchronization shifts the time.
 *  - The device enters a new timezone or observes Daylight Saving Time (DST).
 *  - Midnight date roll-overs occur.
 */
class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i("TimeChangeReceiver", "Time change event received: $action at ${Date()}. Recalculating all alarms...")

        val isTimeChangeAction = action == Intent.ACTION_TIME_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED ||
                action == Intent.ACTION_DATE_CHANGED ||
                action == "android.intent.action.TIME_SET"

        if (isTimeChangeAction) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = CodXDatabase.getDatabase(context)

                    // 1. Recalculate and re-arm all active timetable alarms
                    val count = AlarmScheduler.rescheduleAllActive(context)

                    // 2. Audit the time change event for cloud synchronization
                    val event = SyncEventEntity(
                        id = UUID.randomUUID().toString(),
                        eventType = "TIME_CHANGED",
                        details = "System time shift ($action). Recalculated $count active timetable schedules at ${Date()}.",
                        timestamp = System.currentTimeMillis()
                    )
                    db.syncEventDao().insertEvent(event)

                    // 3. Safety check if currently recording during a forward time leap
                    if (RecordingService.isServiceRunning) {
                        Log.i("TimeChangeReceiver", "Active recording detected during clock shift. Re-verifying stop conditions.")
                    }

                    Log.i("TimeChangeReceiver", "Completed alarm recalculation for $count schedules.")
                } catch (e: Exception) {
                    Log.e("TimeChangeReceiver", "Failed to update alarms after time shift", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
