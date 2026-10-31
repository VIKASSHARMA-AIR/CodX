package com.codx.recorder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codx.recorder.data.local.entities.SyncEventEntity
import com.codx.recorder.ui.theme.*
import com.codx.recorder.ui.viewmodel.RecorderViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyncQueueScreen(viewModel: RecorderViewModel) {
    val syncQueue by viewModel.syncQueueFlow.collectAsState()
    val syncEvents by viewModel.syncEventsFlow.collectAsState()
    val status by viewModel.deviceStatusFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sync & Storage",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Text(
                    text = "${syncQueue.size} pending upload items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Button(
                onClick = { viewModel.triggerManualSync() },
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Process Queue", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Storage Bar Card
        val freeBytes = status?.storageFreeBytes ?: 0L
        val totalBytes = (status?.storageTotalBytes ?: 1L).coerceAtLeast(1L)
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        val usedPct = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Disk Storage", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        text = "${String.format("%.1f", freeBytes / (1024.0*1024*1024))} GB Free / ${String.format("%.1f", totalBytes / (1024.0*1024*1024))} GB",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { usedPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (usedPct > 0.85f) AccentRed else AccentBlue,
                    trackColor = DarkSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Synchronization Event Log",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = TextPrimary
        )
        Text(
            text = "Audit history of background jobs, recovery & uploads",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (syncEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "No sync events recorded yet", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(syncEvents, key = { it.id }) { event ->
                    SyncEventRow(event)
                }
            }
        }
    }
}

@Composable
fun SyncEventRow(event: SyncEventEntity) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeStr = dateFormat.format(Date(event.timestamp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = if (event.status == "SUCCESS") AccentGreen else AccentRed
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, shape = RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.eventType,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                if (!event.details.isNullOrEmpty()) {
                    Text(
                        text = event.details,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            Text(
                text = timeStr,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}
