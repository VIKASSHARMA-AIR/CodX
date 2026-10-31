package com.codx.controller.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codx.controller.ui.screens.*
import com.codx.controller.ui.theme.AccentGold
import com.codx.controller.ui.theme.CodXControllerTheme
import com.codx.controller.ui.theme.ControllerSurface
import com.codx.controller.ui.theme.ControllerSurfaceVariant
import com.codx.controller.ui.theme.TextMuted
import com.codx.controller.ui.viewmodel.ControllerViewModel

enum class ControllerTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Status", Icons.Default.Dashboard),
    SCHEDULES("Schedules", Icons.Default.CalendarToday),
    RECORDINGS("Recordings", Icons.Default.Headphones),
    PAIR("Pair", Icons.Default.VpnKey)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ControllerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CodXControllerTheme {
                val userSession by viewModel.userSession.collectAsState()
                var showProfileSheet by remember { mutableStateOf(false) }
                var showLegalDialog by remember { mutableStateOf<LegalDocType?>(null) }

                if (!userSession.isLoggedIn) {
                    LoginScreen(
                        authManager = viewModel.authManager,
                        onLoginSuccess = {
                            viewModel.refreshAllData()
                        }
                    )
                } else {
                    var currentTab by remember { mutableStateOf(ControllerTab.DASHBOARD) }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ControllerSurface)
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CodX",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // User Profile Avatar Button (Opens Profile Sheet & Account Deletion)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ControllerSurfaceVariant)
                                        .border(1.5.dp, AccentGold, CircleShape)
                                        .clickable { showProfileSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!userSession.photoUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = userSession.photoUrl,
                                            contentDescription = "Profile",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile",
                                            tint = AccentGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            NavigationBar(containerColor = ControllerSurface) {
                                ControllerTab.values().forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                                        label = { Text(tab.title) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = AccentGold,
                                            selectedTextColor = AccentGold,
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted,
                                            indicatorColor = AccentGold.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                ControllerTab.DASHBOARD -> RemoteDashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToSchedules = { currentTab = ControllerTab.SCHEDULES },
                                    onNavigateToRecordings = { currentTab = ControllerTab.RECORDINGS },
                                    onNavigateToPair = { currentTab = ControllerTab.PAIR }
                                )
                                ControllerTab.SCHEDULES -> ScheduleEditorScreen(viewModel)
                                ControllerTab.RECORDINGS -> RemoteRecordingsScreen(viewModel)
                                ControllerTab.PAIR -> PairRecorderScreen(viewModel)
                            }

                            // Floating Persistent Audio Player Sheet
                            AudioPlayerSheet(
                                playerManager = viewModel.audioPlayerManager,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }

                    // Profile & Account Management Sheet
                    if (showProfileSheet) {
                        UserProfileSheet(
                            authManager = viewModel.authManager,
                            onDismiss = { showProfileSheet = false },
                            onOpenTerms = {
                                showProfileSheet = false
                                showLegalDialog = LegalDocType.TERMS
                            },
                            onOpenPrivacy = {
                                showProfileSheet = false
                                showLegalDialog = LegalDocType.PRIVACY
                            },
                            onLoggedOut = {
                                showProfileSheet = false
                            }
                        )
                    }

                    // In-App Terms or Privacy Dialog
                    showLegalDialog?.let { docType ->
                        TermsAndPrivacyDialog(
                            docType = docType,
                            onDismiss = { showLegalDialog = null }
                        )
                    }
                }
            }
        }
    }
}
