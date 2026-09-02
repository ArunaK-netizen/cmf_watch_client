package com.cmfwatch.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.cmfwatch.companion.ui.home.HomeViewModel
import com.cmfwatch.companion.ui.navigation.AppNavigationShell
import com.cmfwatch.companion.ui.theme.BlackBackground
import com.cmfwatch.companion.ui.theme.CmfWatchTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CmfWatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BlackBackground
                ) {
                    val summaryState by homeViewModel.uiState.collectAsState()

                    AppNavigationShell(
                        summary = summaryState,
                        onStartScan = { homeViewModel.startScan() },
                        onConnectDevice = { macAddress -> homeViewModel.connectToWatch(macAddress) },
                        onDisconnectDevice = { homeViewModel.disconnect() },
                        onSyncNow = { homeViewModel.triggerSync() }
                    )
                }
            }
        }
    }
}
