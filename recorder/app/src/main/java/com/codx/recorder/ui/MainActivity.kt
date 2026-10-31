package com.codx.recorder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.codx.recorder.ui.screens.*
import com.codx.recorder.ui.theme.AccentRed
import com.codx.recorder.ui.theme.CodXRecorderTheme
import com.codx.recorder.ui.theme.DarkSurface
import com.codx.recorder.ui.theme.TextMuted
import com.codx.recorder.ui.viewmodel.RecorderViewModel

enum class RecorderTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Default.Mic),
    TIMETABLE("Timetable", Icons.Default.CalendarToday),
    HISTORY("Recordings", Icons.Default.LibraryMusic),
    SYNC("Sync", Icons.Default.CloudSync),
    PAIRING("Pairing", Icons.Default.VpnKey)
}

class MainActivity : ComponentActivity() {

    private val viewModel: RecorderViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            CodXRecorderTheme {
                var currentTab by remember { mutableStateOf(RecorderTab.DASHBOARD) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(containerColor = DarkSurface) {
                            RecorderTab.values().forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentRed,
                                        selectedTextColor = AccentRed,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = AccentRed.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            RecorderTab.DASHBOARD -> DashboardScreen(viewModel)
                            RecorderTab.TIMETABLE -> TimetableScreen(viewModel)
                            RecorderTab.HISTORY -> HistoryScreen(viewModel)
                            RecorderTab.SYNC -> SyncQueueScreen(viewModel)
                            RecorderTab.PAIRING -> PairingScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
    }
}
