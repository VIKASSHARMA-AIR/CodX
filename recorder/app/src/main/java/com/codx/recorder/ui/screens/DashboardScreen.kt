package com.codx.recorder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codx.recorder.recording.RecordingService
import com.codx.recorder.ui.theme.*
import com.codx.recorder.ui.viewmodel.RecorderViewModel

@Composable
fun DashboardScreen(viewModel: RecorderViewModel) {
    val device by viewModel.deviceFlow.collectAsState()
    val status by viewModel.deviceStatusFlow.collectAsState()
    val schedules by viewModel.schedulesFlow.collectAsState()
    val isRecording = RecordingService.isServiceRunning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CodX Recorder",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentCoral.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCoral.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "v1.0.1",
                            color = AccentCoral,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${device?.name ?: "Offline Device"} • Auto Time Sync",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Pairing Code Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (device?.pairedWith != null) AccentGreen else AccentOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = device?.pairingCode ?: "OFFLINE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Device Telemetry Cards (Battery, Storage, State)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Battery",
                value = "${status?.batteryPct ?: 100}%",
                subtitle = if (status?.isCharging == true) "Charging" else "Discharging",
                modifier = Modifier.weight(1f)
            )

            val freeGb = (status?.storageFreeBytes ?: 0L) / (1024 * 1024 * 1024.0)
            TelemetryCard(
                icon = Icons.Default.Storage,
                title = "Storage",
                value = "${String.format("%.1f", freeGb)} GB",
                subtitle = "Available free",
                modifier = Modifier.weight(1f)
            )

            TelemetryCard(
                icon = Icons.Default.Wifi,
                title = "Network",
                value = status?.connectivity ?: "WIFI",
                subtitle = "Sync Channel",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Live Audio Waveform Visualizer
        Text(
            text = if (isRecording) "RECORDING ACTIVE" else "STANDBY / SCHEDULED",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            color = if (isRecording) AccentRed else TextMuted
        )

        Spacer(modifier = Modifier.height(12.dp))
        WaveformVisualizer(isRecording = isRecording)

        Spacer(modifier = Modifier.height(24.dp))

        // Next Scheduled Recording Info
        val activeCount = schedules.count { it.isActive }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Offline Timetable",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "$activeCount active schedules armed via AlarmManager",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Central Record Action Button
        Button(
            onClick = {
                if (isRecording) {
                    viewModel.stopManualRecording()
                } else {
                    viewModel.startManualRecording()
                }
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) DarkSurfaceVariant else AccentRed
            ),
            modifier = Modifier.size(80.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                tint = if (isRecording) AccentRed else Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isRecording) "Tap to Stop" else "Tap to Record Manually",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TelemetryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentRedLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
        }
    }
}
