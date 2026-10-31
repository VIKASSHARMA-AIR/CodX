package com.codx.recorder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.codx.recorder.data.local.CodXDatabase
import com.codx.recorder.data.local.entities.SyncEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Ensures offline schedules are restored automatically on device reboot.
 * No active internet connection is needed.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("BootReceiver", "Device boot event received: $action. Restoring offline schedules...")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = CodXDatabase.getDatabase(context)
                    val count = AlarmScheduler.rescheduleAllActive(context)

                    // Audit reboot restoration
                    val event = SyncEventEntity(
                        id = UUID.randomUUID().toString(),
                        eventType = "REBOOT_SCHEDULES_RESTORED",
                        details = "Restored $count offline timetable schedules following system boot",
                        timestamp = System.currentTimeMillis()
                    )
                    db.syncEventDao().insertEvent(event)
                    Log.i("BootReceiver", "Restored $count offline schedules following reboot")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error restoring schedules after reboot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
