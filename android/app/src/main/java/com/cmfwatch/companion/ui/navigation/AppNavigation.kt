package com.cmfwatch.companion.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.ui.activity.ActivityScreen
import com.cmfwatch.companion.ui.device.DeviceScreen
import com.cmfwatch.companion.ui.home.HomeScreen
import com.cmfwatch.companion.ui.sleep.SleepScreen
import com.cmfwatch.companion.ui.vitals.HeartRateDetailScreen
import com.cmfwatch.companion.ui.theme.*

enum class NavDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    ACTIVITY("Activity", Icons.Default.BarChart),
    DEVICES("Devices", Icons.Default.Watch),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun AppNavigationShell(
    summary: DashboardSummary,
    onStartScan: () -> Unit = {},
    onConnectDevice: (String) -> Unit = {},
    onDisconnectDevice: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    var isViewingHeartRateDetail by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(LightBackground)) {
        if (isViewingHeartRateDetail) {
            HeartRateDetailScreen(
                allSamples = summary.hrSamplesToday,
                onBackClick = { isViewingHeartRateDetail = false }
            )
        } else {
            // Main Screen Content
            when (currentDestination) {
                NavDestination.HOME -> HomeScreen(
                    summary = summary,
                    onActivityClick = { currentDestination = NavDestination.ACTIVITY },
                    onSleepClick = { currentDestination = NavDestination.PROFILE },
                    onHeartRateClick = { isViewingHeartRateDetail = true },
                    onWatchCardClick = { currentDestination = NavDestination.DEVICES }
                )
                NavDestination.ACTIVITY -> ActivityScreen(
                    summary = summary
                )
                NavDestination.DEVICES -> DeviceScreen(
                    summary = summary,
                    onStartScan = onStartScan,
                    onConnectDevice = onConnectDevice,
                    onDisconnectDevice = onDisconnectDevice,
                    onSyncNow = onSyncNow,
                    onBackClick = { currentDestination = NavDestination.HOME }
                )
                NavDestination.PROFILE -> SleepScreen(session = null)
            }

            // Integrated Bottom Navigation Bar (White surface, zero gray rectangle ripple)
            BottomNavigationBar(
                currentDestination = currentDestination,
                onDestinationSelected = { currentDestination = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
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
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(SurfaceWhite)
            .padding(vertical = 10.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavDestination.values().forEach { destination ->
                val selected = destination == currentDestination
                val iconTint = if (selected) TextPrimary else TextSecondary
                val textColor = if (selected) TextPrimary else TextSecondary
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onDestinationSelected(destination) }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = destination.label,
                        fontFamily = AppFontFamily,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}
