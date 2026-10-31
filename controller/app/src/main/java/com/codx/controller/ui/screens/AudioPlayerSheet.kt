package com.codx.controller.ui.screens

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
import com.codx.controller.playback.AudioPlayerManager
import com.codx.controller.ui.theme.*

@Composable
fun AudioPlayerSheet(
    playerManager: AudioPlayerManager,
    modifier: Modifier = Modifier
) {
    val state by playerManager.playbackState.collectAsState()
    if (state.recordingId == null) return

    val currentSec = state.currentPositionMs / 1000
    val totalSec = state.durationMs / 1000

    val currentStr = String.format("%02d:%02d", currentSec / 60, currentSec % 60)
    val totalStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60)

    val sliderProgress = if (state.durationMs > 0) {
        (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = ControllerSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Track Info & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.currentTitle.ifEmpty { "Audio Recording" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = if (state.isBuffering) "Buffering stream..." else "Streaming from cloud",
                        fontSize = 12.sp,
                        color = if (state.isBuffering) AccentGold else TextSecondary
                    )
                }

                IconButton(onClick = { playerManager.stop() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Seek Slider
            Slider(
                value = sliderProgress,
                onValueChange = { newPct ->
                    val targetMs = (newPct * state.durationMs).toLong()
                    playerManager.seekTo(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = AccentGold,
                    activeTrackColor = AccentGold,
                    inactiveTrackColor = ControllerSurface
                )
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = currentStr, fontSize = 11.sp, color = TextMuted)
                Text(text = totalStr, fontSize = 11.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback Speed Button
                val nextSpeed = when (state.playbackSpeed) {
                    1.0f -> 1.25f
                    1.25f -> 1.5f
                    1.5f -> 2.0f
                    else -> 1.0f
                }
                TextButton(
                    onClick = { playerManager.setPlaybackSpeed(nextSpeed) },
                    colors = ButtonDefaults.textButtonColors(contentColor = AccentGold)
                ) {
                    Text("${state.playbackSpeed}x", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Central Play/Pause Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val back10 = (state.currentPositionMs - 10000L).coerceAtLeast(0L)
                            playerManager.seekTo(back10)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Replay10, contentDescription = "Back 10s", tint = TextPrimary)
                    }

                    Button(
                        onClick = { playerManager.togglePlayPause() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        modifier = Modifier.size(52.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = ControllerBackground,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val fwd10 = (state.currentPositionMs + 10000L).coerceAtMost(state.durationMs)
                            playerManager.seekTo(fwd10)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Forward10, contentDescription = "Forward 10s", tint = TextPrimary)
                    }
                }

                // Stop button
                IconButton(onClick = { playerManager.stop() }) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = TextSecondary)
                }
            }
        }
    }
}
