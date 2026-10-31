package com.codx.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.codx.controller.data.local.entities.CachedRecordingEntity
import com.codx.controller.ui.theme.*
import com.codx.controller.ui.viewmodel.ControllerViewModel

@Composable
fun RemoteRecordingsScreen(viewModel: ControllerViewModel) {
    val recordings by viewModel.recordingsFlow.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ControllerBackground)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Cloud Recordings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Text(
                    text = "${recordings.size} recordings available from Recorder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = { viewModel.refreshAllData() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ControllerSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = TextPrimary
                )
            }
        }

        if (statusMsg != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AccentCyan.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMsg!!,
                    color = AccentCyan,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "No recordings synced yet", color = TextSecondary, fontSize = 16.sp)
                    Text(text = "Completed recordings on Phone A will appear here", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(recordings, key = { it.id }) { rec ->
                    val isCurrent = playbackState.recordingId == rec.id
                    val isPlaying = isCurrent && playbackState.isPlaying

                    RemoteRecordingRow(
                        recording = rec,
                        isPlaying = isPlaying,
                        onPlayClick = { viewModel.playRecording(rec) },
                        onDownloadClick = { viewModel.downloadRecording(rec) }
                    )
                }
            }
        }
    }
}

@Composable
fun RemoteRecordingRow(
    recording: CachedRecordingEntity,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val sizeMb = recording.fileSizeBytes / (1024.0 * 1024.0)
    val minutes = (recording.durationMs / 1000) / 60
    val seconds = (recording.durationMs / 1000) % 60
    val durationStr = String.format("%02d:%02d", minutes, seconds)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ControllerSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) AccentGold else BorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { onPlayClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Button
            Surface(
                shape = CircleShape,
                color = if (isPlaying) AccentGold else ControllerSurfaceVariant,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPlaying) ControllerBackground else AccentGold,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.filename,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isPlaying) AccentGold else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$durationStr • ${String.format("%.1f", sizeMb)} MB",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (recording.localFilePath != null) "Downloaded for offline" else "Cloud streamable",
                    fontSize = 11.sp,
                    color = if (recording.localFilePath != null) AccentGreen else TextMuted
                )
            }

            IconButton(onClick = onDownloadClick) {
                Icon(
                    imageVector = if (recording.localFilePath != null) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = "Download",
                    tint = if (recording.localFilePath != null) AccentGreen else AccentCyan
                )
            }
        }
    }
}
