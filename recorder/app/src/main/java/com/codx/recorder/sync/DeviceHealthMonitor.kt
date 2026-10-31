package com.codx.recorder.sync

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.codx.recorder.recording.RecordingService

data class DeviceHealth(
    val batteryPct: Int,
    val isCharging: Boolean,
    val storageFreeBytes: Long,
    val storageTotalBytes: Long,
    val networkType: String,
    val currentState: String
)

object DeviceHealthMonitor {

    fun getHealth(context: Context): DeviceHealth {
        // 1. Battery & Charging
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        // 2. Storage
        val stat = StatFs(Environment.getDataDirectory().path)
        val storageFreeBytes = stat.availableBytes
        val storageTotalBytes = stat.totalBytes

        // 3. Network
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val networkType = when {
            caps == null -> "OFFLINE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }

        // 4. Current State
        val currentState = when {
            RecordingService.isServiceRunning -> "RECORDING"
            networkType == "OFFLINE" -> "OFFLINE_IDLE"
            else -> "IDLE"
        }

        return DeviceHealth(
            batteryPct = batteryPct,
            isCharging = isCharging,
            storageFreeBytes = storageFreeBytes,
            storageTotalBytes = storageTotalBytes,
            networkType = networkType,
            currentState = currentState
        )
    }
}
