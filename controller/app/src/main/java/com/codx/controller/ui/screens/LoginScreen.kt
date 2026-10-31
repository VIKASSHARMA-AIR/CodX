package com.codx.controller.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codx.controller.auth.FirebaseAuthManager
import com.codx.controller.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authManager: FirebaseAuthManager,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var acceptedTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var openLegalDialog by remember { mutableStateOf<LegalDocType?>(null) }
    var serverHostInput by remember { mutableStateOf(com.codx.controller.data.remote.RetrofitClient.getBaseUrl()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ControllerBackground)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        // App Branding Logo
        Surface(
            shape = CircleShape,
            color = ControllerSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(2.dp, AccentGold),
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "CodX",
                    tint = AccentGold,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "CodX Controller",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Offline-First, Remotely Synchronized Audio System",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Feature Highlights
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ControllerSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureRow(Icons.Default.Schedule, "Remote Timetable Scheduling", "Configure daily & weekly recording timetables on Phone A")
                FeatureRow(Icons.Default.BatteryChargingFull, "Hardware Status & Telemetry", "Monitor battery level, charging, and disk storage")
                FeatureRow(Icons.Default.Headphones, "Cloud Audio Streaming", "Stream or download synchronized recordings with ExoPlayer")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Google Play Prominent Disclosure (Anti-Stalkerware Policy Requirement)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AccentCyan.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Mutual Consent Disclosure",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AccentCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CodX Controller operates solely with authorized hardware devices. Both phones must be explicitly paired using the on-screen code. Phone A displays a persistent notification during recording.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Checkbox: Terms & Conditions and Privacy Policy
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { acceptedTerms = !acceptedTerms },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = { acceptedTerms = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = AccentGold,
                    uncheckedColor = TextMuted,
                    checkmarkColor = ControllerBackground
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "I have read and agree to the",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row {
                    Text(
                        text = "Terms & Conditions",
                        fontSize = 12.sp,
                        color = AccentGold,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { openLegalDialog = LegalDocType.TERMS }
                    )
                    Text(text = " and ", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        text = "Privacy Policy",
                        fontSize = 12.sp,
                        color = AccentCyan,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { openLegalDialog = LegalDocType.PRIVACY }
                    )
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage!!,
                color = AccentRed,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary: "Sign in with Google" Button (Firebase API)
        Button(
            onClick = {
                if (!acceptedTerms) {
                    errorMessage = "Please accept the Terms & Conditions and Privacy Policy to continue"
                    return@Button
                }
                isLoading = true
                errorMessage = null
                scope.launch {
                    val result = authManager.signInWithGoogle(context)
                    isLoading = false
                    if (result.isSuccess) {
                        onLoginSuccess()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Google Sign-In failed"
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sign in with Google",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1F1F1F)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Secondary: "Play Store Reviewer / Demo Sign-In"
        OutlinedButton(
            onClick = {
                if (!acceptedTerms) {
                    errorMessage = "Please accept the Terms & Conditions and Privacy Policy to continue"
                    return@OutlinedButton
                }
                isLoading = true
                errorMessage = null
                scope.launch {
                    val result = authManager.signInWithDemoAccount()
                    isLoading = false
                    if (result.isSuccess) {
                        onLoginSuccess()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Demo login failed"
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Sign in with Demo / Review Account",
                color = AccentGold,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optional: Server URL Config Field
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ControllerSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Server IP / Endpoint:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = serverHostInput,
                    onValueChange = {
                        serverHostInput = it
                        com.codx.controller.data.remote.RetrofitClient.setBaseUrl(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = TextMuted.copy(alpha = 0.4f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Terms or Privacy Modal
    openLegalDialog?.let { docType ->
        TermsAndPrivacyDialog(
            docType = docType,
            onDismiss = { openLegalDialog = null }
        )
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ControllerSurfaceVariant,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
            Text(text = desc, fontSize = 11.sp, color = TextMuted)
        }
    }
}
