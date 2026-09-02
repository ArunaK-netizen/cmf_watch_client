package com.cmfwatch.companion.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.ui.activity.ActivityScreen
import com.cmfwatch.companion.ui.device.DeviceScreen
import com.cmfwatch.companion.ui.health.HealthScreen
import com.cmfwatch.companion.ui.history.HistoryScreen
import com.cmfwatch.companion.ui.home.HomeScreen
import com.cmfwatch.companion.ui.sleep.SleepScreen
import com.cmfwatch.companion.ui.theme.*

enum class NavDestination(val label: String) {
    HOME("HOME"),
    HEALTH("HEALTH"),
    ACTIVITY("ACTIVITY"),
    SLEEP("SLEEP"),
    DEVICE("DEVICE")
}

@Composable
fun AppNavigationShell(
    summary: DashboardSummary,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }

    Box(modifier = modifier.fillMaxSize().background(BlackBackground)) {
        // Main Screen Content
        when (currentDestination) {
            NavDestination.HOME -> HomeScreen(summary = summary)
            NavDestination.HEALTH -> HealthScreen(
                latestBpm = summary.latestHeartRate,
                restingBpm = summary.restingHeartRate,
                latestStress = summary.latestStressScore,
                latestSpO2 = 98
            )
            NavDestination.ACTIVITY -> ActivityScreen(
                steps = summary.todaySteps,
                distanceKm = summary.todayDistanceKm,
                caloriesKcal = summary.todayCaloriesKcal
            )
            NavDestination.SLEEP -> SleepScreen(session = null)
            NavDestination.DEVICE -> DeviceScreen(
                connectionState = summary.connectionState,
                batteryLevel = summary.deviceBatteryLevel
            )
        }

        // Bottom Navigation Bar
        BottomNavigationBar(
            currentDestination = currentDestination,
            onDestinationSelected = { currentDestination = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun BottomNavigationBar(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkCardSurface)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavDestination.values().forEach { destination ->
                val selected = destination == currentDestination
                val textColor = if (selected) AccentRed else TextSecondary

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onDestinationSelected(destination) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = destination.label,
                        fontFamily = NType82FontFamily,
                        fontSize = 11.sp,
                        color = textColor,
                        letterSpacing = 1.sp
                    )

                    if (selected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(AccentRed)
                        )
                    }
                }
            }
        }
    }
}
