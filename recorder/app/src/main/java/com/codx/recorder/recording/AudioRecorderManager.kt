package com.codx.recorder.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.UUID

class AudioRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    var isRecording: Boolean = false
        private set

    var currentRecordingId: String? = null
        private set
    var currentOutputFile: File? = null
        private set
    private var startTimeMs: Long = 0
    private var startElapsedRealtimeMs: Long = 0

    fun startRecording(sessionId: String): Result<File> {
        if (isRecording) {
            return Result.failure(IllegalStateException("Recorder is already running"))
        }

        val recordingId = UUID.randomUUID().toString()
        val audioDir = File(context.filesDir, "codx_recordings")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        val filename = "CODEX_${System.currentTimeMillis()}_$recordingId.m4a"
        val outputFile = File(audioDir, filename)

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            this.recorder = mediaRecorder
            this.isRecording = true
            this.currentRecordingId = recordingId
            this.currentOutputFile = outputFile
            this.startTimeMs = System.currentTimeMillis()
            this.startElapsedRealtimeMs = SystemClock.elapsedRealtime()

            Log.i("AudioRecorderManager", "Recording started: ${outputFile.absolutePath}")
            return Result.success(outputFile)
        } catch (e: IOException) {
            Log.e("AudioRecorderManager", "Failed to start recording", e)
            mediaRecorder.release()
            return Result.failure(e)
        }
    }

    fun stopRecording(): Result<RecordingResult> {
        if (!isRecording || recorder == null) {
            return Result.failure(IllegalStateException("No active recording session"))
        }

        return try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
            recorder = null
            isRecording = false

            val file = currentOutputFile
            val id = currentRecordingId
            if (file == null || !file.exists() || id == null) {
                return Result.failure(IllegalStateException("Recording file missing"))
            }

            // Monotonic clock protects duration calculation from device time changes/shifts
            val durationMs = if (startElapsedRealtimeMs > 0) {
                (SystemClock.elapsedRealtime() - startElapsedRealtimeMs).coerceAtLeast(0L)
            } else {
                (System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L)
            }
            val checksum = AudioFileValidator.computeSha256(file)

            val result = RecordingResult(
                id = id,
                file = file,
                durationMs = durationMs,
                fileSizeBytes = file.length(),
                checksumSha256 = checksum
            )
            Log.i("AudioRecorderManager", "Recording stopped: $id, size=${file.length()} bytes, duration=${durationMs}ms")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error stopping recorder", e)
            recorder?.release()
            recorder = null
            isRecording = false
            Result.failure(e)
        }
    }

    /**
     * Returns the maximum amplitude recorded since the last call.
     * Normalized between 0.0f and 1.0f for smooth waveform visualizer.
     */
    fun getMaxAmplitude(): Float {
        if (!isRecording || recorder == null) return 0f
        return try {
            val amp = recorder?.maxAmplitude ?: 0
            (amp / 32767f).coerceIn(0f, 1f)
        } catch (e: Exception) {
            0f
        }
    }
}

data class RecordingResult(
    val id: String,
    val file: File,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val checksumSha256: String
)
