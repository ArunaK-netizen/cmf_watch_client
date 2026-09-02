package com.cmfwatch.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.ui.navigation.AppNavigationShell
import com.cmfwatch.companion.ui.theme.BlackBackground
import com.cmfwatch.companion.ui.theme.CmfWatchTheme
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CmfWatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BlackBackground
                ) {
                    val initialSummary = DashboardSummary(
                        latestHeartRate = 72,
                        restingHeartRate = 62,
                        todaySteps = 8421,
                        todayDistanceKm = 6.3f,
                        todayCaloriesKcal = 1842,
                        lastSleepMinutes = 462, // 7h 42m
                        latestStressScore = 24,
                        deviceBatteryLevel = 86,
                        connectionState = DeviceConnectionState.CONNECTED_PAIRED,
                        lastSyncedAt = Instant.now()
                    )

                    AppNavigationShell(summary = initialSummary)
                }
            }
        }
    }
}
