package com.codx.recorder.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.codx.recorder.data.local.entities.ScheduleEntity
import java.util.Calendar

object AlarmScheduler {

    const val EXTRA_SCHEDULE_ID = "alarm_schedule_id"
    const val EXTRA_IS_START_ALARM = "alarm_is_start"

    fun schedule(context: Context, schedule: ScheduleEntity) {
        if (!schedule.isActive) {
            cancel(context, schedule)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Schedule START alarm
        val startMillis = calculateNextTrigger(schedule.startTime, schedule.daysOfWeek)
        val startIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_IS_START_ALARM, true)
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.hashCode(),
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(alarmManager, startMillis, startPendingIntent)
        Log.i("AlarmScheduler", "Scheduled START alarm for '${schedule.label}' at $startMillis")

        // 2. Schedule STOP alarm
        val stopMillis = calculateNextTrigger(schedule.stopTime, schedule.daysOfWeek, isStop = true, refStart = startMillis)
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_IS_START_ALARM, false)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.hashCode() + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(alarmManager, stopMillis, stopPendingIntent)
        Log.i("AlarmScheduler", "Scheduled STOP alarm for '${schedule.label}' at $stopMillis")
    }

    fun cancel(context: Context, schedule: ScheduleEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startIntent = Intent(context, AlarmReceiver::class.java)
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.hashCode(),
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPendingIntent != null) {
            alarmManager.cancel(startPendingIntent)
        }

        val stopIntent = Intent(context, AlarmReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.hashCode() + 1,
            stopIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (stopPendingIntent != null) {
            alarmManager.cancel(stopPendingIntent)
        }

        Log.i("AlarmScheduler", "Cancelled alarms for '${schedule.label}'")
    }

    /**
     * Reschedules all active timetable schedules across the device.
     * Invoked automatically whenever system time, date, or timezone changes,
     * after device reboot, or when schedules are updated.
     */
    suspend fun rescheduleAllActive(context: Context): Int {
        val db = com.codx.recorder.data.local.CodXDatabase.getDatabase(context)
        val activeSchedules = db.scheduleDao().getActiveSchedules()
        var count = 0
        for (schedule in activeSchedules) {
            schedule(context, schedule)
            count++
        }
        Log.i("AlarmScheduler", "Successfully recalculated and refreshed $count active timetable alarms")
        return count
    }

    private fun scheduleExact(alarmManager: AlarmManager, triggerMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun calculateNextTrigger(
        timeStr: String,
        daysOfWeek: String,
        isStop: Boolean = false,
        refStart: Long = 0L
    ): Long {
        val parts = timeStr.split(":")
        val hour = parts[0].trim().toIntOrNull() ?: 0
        val minute = parts[1].trim().toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If time already passed today, advance by at least 1 day
        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // If this is a stop alarm, ensure it is after refStart
        if (isStop && refStart > 0 && calendar.timeInMillis <= refStart) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Handle weekly recurrence (e.g. "MON,WED,FRI")
        if (daysOfWeek != "DAILY" && !daysOfWeek.startsWith("ONCE")) {
            val allowedDays = parseDaysOfWeek(daysOfWeek)
            if (allowedDays.isNotEmpty()) {
                var attempts = 0
                while (!allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK)) && attempts < 8) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    attempts++
                }
            }
        }

        return calendar.timeInMillis
    }

    private fun parseDaysOfWeek(daysStr: String): Set<Int> {
        val set = mutableSetOf<Int>()
        val tokens = daysStr.uppercase().split(",", " ")
        for (token in tokens) {
            when (token.trim()) {
                "SUN" -> set.add(Calendar.SUNDAY)
                "MON" -> set.add(Calendar.MONDAY)
                "TUE" -> set.add(Calendar.TUESDAY)
                "WED" -> set.add(Calendar.WEDNESDAY)
                "THU" -> set.add(Calendar.THURSDAY)
                "FRI" -> set.add(Calendar.FRIDAY)
                "SAT" -> set.add(Calendar.SATURDAY)
            }
        }
        return set
    }
}
