package com.cmfwatch.companion.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.ui.activity.ActivityScreen
import com.cmfwatch.companion.ui.device.DeviceScreen
import com.cmfwatch.companion.ui.health.HealthScreen
import com.cmfwatch.companion.ui.home.HomeScreen
import com.cmfwatch.companion.ui.theme.CmfBackground
import com.cmfwatch.companion.ui.theme.CmfOnSurfaceVariant
import com.cmfwatch.companion.ui.theme.CmfPrimaryContainer
import com.cmfwatch.companion.ui.theme.CmfSurfaceLowest
import com.cmfwatch.companion.ui.theme.InterFontFamily
import com.cmfwatch.companion.ui.vitals.HeartRateDetailScreen

enum class NavDestination(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.DonutLarge),
    VITALS("Vitals", Icons.Outlined.MonitorHeart),
    WORKOUTS("Workouts", Icons.Outlined.DirectionsRun),
    DEVICE("Device", Icons.Outlined.Watch)
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
    var currentDestination by remember { mutableStateOf(NavDestination.TODAY) }
    var isViewingHeartRateDetail by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CmfBackground)
    ) {
        if (isViewingHeartRateDetail) {
            HeartRateDetailScreen(
                allSamples = summary.hrSamplesToday,
                onBackClick = { isViewingHeartRateDetail = false }
            )
        } else {
            when (currentDestination) {
                NavDestination.TODAY -> HomeScreen(
                    summary = summary,
                    onActivityClick = { currentDestination = NavDestination.WORKOUTS },
                    onSleepClick = { currentDestination = NavDestination.VITALS },
                    onHeartRateClick = { isViewingHeartRateDetail = true },
                    onWatchCardClick = { currentDestination = NavDestination.DEVICE }
                )
                NavDestination.VITALS -> HealthScreen(
                    summary = summary,
                    onHeartRateClick = { isViewingHeartRateDetail = true },
                    onSleepClick = { },
                    onAvatarClick = { currentDestination = NavDestination.DEVICE }
                )
                NavDestination.WORKOUTS -> ActivityScreen(
                    summary = summary,
                    onAvatarClick = { currentDestination = NavDestination.DEVICE },
                    onStartWorkout = onSyncNow
                )
                NavDestination.DEVICE -> DeviceScreen(
                    summary = summary,
                    onStartScan = onStartScan,
                    onConnectDevice = onConnectDevice,
                    onDisconnectDevice = onDisconnectDevice,
                    onSyncNow = onSyncNow,
                    onBackClick = { currentDestination = NavDestination.TODAY }
                )
            }

            CmfBottomNav(
                currentDestination = currentDestination,
                onDestinationSelected = { currentDestination = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun CmfBottomNav(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 448.dp)
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color(0x14111315),
                    spotColor = Color(0x14111315)
                )
                .clip(RoundedCornerShape(50))
                .background(CmfSurfaceLowest.copy(alpha = 0.92f))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavDestination.entries.forEach { destination ->
                val selected = destination == currentDestination
                val tint = if (selected) CmfPrimaryContainer else CmfOnSurfaceVariant
                val interaction = remember { MutableInteractionSource() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) { onDestinationSelected(destination) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = destination.label,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.66.sp,
                        color = tint,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
