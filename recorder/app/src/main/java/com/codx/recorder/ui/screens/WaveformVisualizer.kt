package com.codx.recorder.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.codx.recorder.ui.theme.AccentRed
import com.codx.recorder.ui.theme.DarkSurfaceVariant
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 28
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    // Animated phase offset
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val baseHeight = if (isRecording) {
                val sinVal = kotlin.math.sin(phase + (i * 0.4f)).toFloat()
                val heightPct = (kotlin.math.abs(sinVal) * 0.7f + 0.15f).coerceIn(0.1f, 1.0f)
                (heightPct * 65).dp
            } else {
                8.dp
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(baseHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isRecording) AccentRed else AccentRed.copy(alpha = 0.25f))
            )
        }
    }
}
