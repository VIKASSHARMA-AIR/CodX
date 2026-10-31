package com.codx.recorder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codx.recorder.data.remote.RetrofitClient
import com.codx.recorder.ui.theme.*
import com.codx.recorder.ui.viewmodel.RecorderViewModel

@Composable
fun PairingScreen(viewModel: RecorderViewModel) {
    val device by viewModel.deviceFlow.collectAsState()
    var serverUrl by remember { mutableStateOf(RetrofitClient.getBaseUrl()) }
    var saveSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Device Pairing",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text = "Enter this pairing code on CodX Controller Phone B",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Large 6-digit Pairing Code Display
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(2.dp, AccentRed.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "PAIRING CODE",
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = device?.pairingCode ?: "CONNECTING...",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                val isPaired = device?.pairedWith != null
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPaired) AccentGreen.copy(alpha = 0.15f) else AccentOrange.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isPaired) "PAIRED WITH CONTROLLER" else "WAITING FOR CONTROLLER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaired) AccentGreen else AccentOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Server URL Configuration Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CodX Cloud Server URL",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        saveSuccess = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Server Base URL") },
                    placeholder = { Text("http://192.168.1.50:8000/") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentRed,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.updateServerUrl(serverUrl)
                        saveSuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (saveSuccess) "Saved & Reconnected!" else "Save & Reconnect", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device UUID
        Text(
            text = "Device ID: ${device?.id ?: "Unknown"}",
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}
