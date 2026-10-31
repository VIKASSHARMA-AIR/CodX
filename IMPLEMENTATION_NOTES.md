# CodX — Implementation & Production Integration Notes

This document provides a technical breakdown of the architecture, edge cases, resilience guarantees, and production integration steps for **CodX Recorder** (Phone A), **CodX Controller** (Phone B), and the **CodX Cloud Server**.

---

## 1. Offline-First Scheduling & Recovery Mechanics

### Why AlarmManager Was Chosen Over WorkManager for Schedule Execution
Android WorkManager is designed for deferrable background jobs and batches executions to optimize battery life. Under Android Doze mode, WorkManager jobs can drift by 15–45 minutes.

For audio recording sessions that must adhere strictly to exact start times:
- `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ...)` is used for both **START** and **STOP** alarms.
- This wakes the device from low-power Doze mode at the precise minute, even if the phone has been idle on a desk for hours.
- A temporary WakeLock is held by `AlarmReceiver` while transitioning control to `RecordingService`.

### Reboot Resilience (`BootReceiver`)
Alarms registered with Android's `AlarmManager` are cleared when a phone restarts.
To fulfill the requirement that **a timetable must execute even after reboot without internet**:
1. `BootReceiver` declares intent filters for:
   - `android.intent.action.BOOT_COMPLETED`
   - `android.intent.action.MY_PACKAGE_REPLACED`
   - `android.intent.action.QUICKBOOT_POWERON` (OEM fast boot)
2. On receipt, `BootReceiver` calls `goAsync()`, opens the local Room database `CodXDatabase`, and loads all active schedules.
3. `AlarmScheduler` re-calculates the next hardware trigger timestamps and registers each schedule back into `AlarmManager`.
4. A `SyncEventEntity` is inserted into local storage recording that schedules were successfully recovered.

### Clock and Time Zone Recovery (`TimeChangeReceiver`)
If the user travels across time zones or the cellular network updates the system clock:
- `TimeChangeReceiver` intercepts `ACTION_TIME_CHANGED` and `ACTION_TIMEZONE_CHANGED`.
- It recalculates the next trigger times based on the new clock offsets and refreshes the pending intents.

---

## 2. Audio Capture & Hardware Resilience

### Partial WakeLock Management
When the screen is turned off or locks, Android transitions the CPU into a suspended state. If `MediaRecorder` is recording, audio buffers will halt or become corrupted.
- `RecordingService` acquires a `PowerManager.PARTIAL_WAKE_LOCK` with tag `CodX:RecordingWakeLock`.
- The lock is acquired when the foreground service starts and is held continuously until recording completes.
- A safety timeout of 4 hours prevents battery depletion in the event of an unexpected process lockup.

### Battery & Storage Safeguards
Before starting a session and continuously during recording:
- `DeviceHealthMonitor` checks battery percentage and charging state.
- If battery drops below 5% and the device is not charging, `RecordingService` invokes `stopForegroundRecording("LOW_BATTERY_ABORT")` to cleanly flush AAC buffers and close the `.m4a` file before the phone turns off.
- Available storage is measured; if free disk space is less than 50 MB, recording is halted gracefully.

### Audio Deduplication Guarantee
Reconnection to the cloud server must never create duplicate recordings:
1. When recording starts, `AudioRecorderManager` generates a cryptographically secure UUID (`currentRecordingId`).
2. The file is saved locally as `CODEX_<timestamp>_<uuid>.m4a`.
3. When uploaded to the server, the UUID is passed as the primary key `id`.
4. The server's upload endpoint checks:
   ```python
   existing = db.query(models.Recording).filter(models.Recording.id == id).first()
   if existing:
       return existing # Idempotent deduplication
   ```
5. If a connection drops midway through an upload and WorkManager retries, the server verifies the SHA-256 hash and prevents duplicated records.

---

## 3. Streaming & Audio Player Architecture (Controller)

### Range-Request Streaming
Long audio recordings can range from 10 MB to over 100 MB. Downloading the entire file before playback causes significant user delay.
- The FastAPI cloud server implements HTTP 206 Partial Content in `/api/recordings/{id}/stream`.
- Android Media3 ExoPlayer (`AudioPlayerManager`) leverages Range headers to begin playback in under 200ms and scrub seamlessly across the timeline.
- Users can also tap the Download button on Phone B to store an offline copy locally in `downloaded_recordings/`.

---

## 4. Production Hardening Checklist

### 1. Battery Optimization Whitelist
To guarantee Android OEMs (Samsung, Xiaomi, Huawei, Pixel) do not terminate the background recording service during long Doze cycles:
Prompt the user on Phone A to disable battery optimization:
```kotlin
val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
    data = Uri.parse("package:$packageName")
}
startActivity(intent)
```

### 2. HTTPS & Reverse Proxy Deployment
In production, place the FastAPI server behind Nginx, Caddy, or Traefik with TLS/SSL:
```nginx
server {
    server_name codx.yourdomain.com;
    client_max_body_size 500M;

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Range $http_range;
        proxy_set_header If-Range $http_if_range;
    }
}
```

### 3. ProGuard / R8 Rules
For release builds of both Android apps, add the following to `proguard-rules.pro`:
```proguard
# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retrofit & Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.codx.**.models.** { *; }

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
```
