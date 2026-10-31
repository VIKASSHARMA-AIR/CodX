package com.codx.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.codx.controller.ui.theme.*

enum class LegalDocType {
    TERMS,
    PRIVACY
}

@Composable
fun TermsAndPrivacyDialog(
    docType: LegalDocType,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ControllerSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (docType == LegalDocType.TERMS) "Terms & Conditions" else "Privacy Policy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Document Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    if (docType == LegalDocType.TERMS) {
                        TermsContent()
                    } else {
                        PrivacyContent()
                    }
                }

                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I Understand & Close", color = ControllerBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TermsContent() {
    Text(
        text = "CodX Controller Terms of Service\nLast Updated: October 31, 2026\n",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = AccentGold
    )
    Text(
        text = """
1. ACCEPTANCE OF TERMS
By accessing or using CodX Controller, you agree to be bound by these Terms and Conditions.

2. STRICT PROHIBITION OF CLANDESTINE SURVEILLANCE
CodX Controller is an authorized remote companion app designed solely for hardware audio timetable management. You warrant and agree that:
- You will NEVER install or pair CodX on any device without the explicit, documented consent of the device owner.
- You will NEVER use the App to intercept private communications, record conversations without required mutual consent, or monitor any individual covertly.
- The paired CodX Recorder will ALWAYS display a persistent notification during recording sessions.

3. GOOGLE SIGN-IN & AUTHENTICATION
Users authenticate using Google Sign-In via Firebase. You are solely responsible for maintaining the confidentiality of your account credentials.

4. RECORDING LAWS COMPLIANCE
You represent that you will strictly comply with all applicable local, state, and international wiretap and recording consent statutes (including one-party and all-party consent laws).

5. ACCOUNT DELETION
You have the right to permanently delete your account, paired device records, and all cloud-stored recordings directly within the app or via https://codx-audio.com/delete-account.

6. LIMITATION OF LIABILITY
The Service is provided "AS IS". CodX Technologies is not liable for illegal or unauthorized recording conducted by users.
        """.trimIndent(),
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = TextSecondary
    )
}

@Composable
private fun PrivacyContent() {
    Text(
        text = "CodX Controller Privacy Policy\nLast Updated: October 31, 2026\n",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = AccentCyan
    )
    Text(
        text = """
1. DATA WE COLLECT
- Google Account Data: Name, Email Address, Profile Picture, and Firebase UID for authentication.
- Device Telemetry: Battery %, charging status, disk storage space, and network connectivity.
- Audio Files & Metadata: MPEG-4 AAC audio recordings, durations, timestamps, and SHA-256 integrity checksums.

2. HOW WE USE DATA
- Authenticating you and isolating your paired devices.
- Synchronizing recording timetables to Phone A.
- Facilitating audio playback and download on Phone B.
- Monitoring device battery to prevent recording interruptions.

3. ZERO THIRD-PARTY DATA SALE
We DO NOT sell, rent, or trade your personal data, audio recordings, or telemetry to any advertiser or third party.

4. DATA SECURITY
All network transmissions use TLS/HTTPS encryption. Files are verified with cryptographic SHA-256 hashes.

5. GOOGLE PLAY ACCOUNT DELETION
You may delete your account and all associated recordings at any time from within the app (User Profile -> Delete Account) or at https://codx-audio.com/delete-account.

6. ANTI-STALKERWARE COMPLIANCE
CodX Recorder displays a persistent, non-dismissible notification while recording. Covert background execution is disabled.
        """.trimIndent(),
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = TextSecondary
    )
}
