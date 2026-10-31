package com.codx.recorder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.codx.recorder.data.local.CodXDatabase
import com.codx.recorder.recording.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_ID) ?: return
        val isStartAlarm = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_START_ALARM, true)

        Log.i("AlarmReceiver", "Alarm fired! scheduleId=$scheduleId, isStart=$isStartAlarm")

        if (isStartAlarm) {
            val sessionId = UUID.randomUUID().toString()
            RecordingService.startService(context, scheduleId = scheduleId, sessionId = sessionId)

            // Reschedule next recurrence in the background
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = CodXDatabase.getDatabase(context)
                    val schedule = db.scheduleDao().getScheduleById(scheduleId)
                    if (schedule != null && schedule.isActive) {
                        AlarmScheduler.schedule(context, schedule)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            RecordingService.stopService(context)
        }
    }
}
