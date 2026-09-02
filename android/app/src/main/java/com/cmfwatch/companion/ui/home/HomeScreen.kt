package com.cmfwatch.companion.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.ui.components.MetricCard
import com.cmfwatch.companion.ui.components.StatusPill
import com.cmfwatch.companion.ui.theme.*

@Composable
fun HomeScreen(
    summary: DashboardSummary,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BlackBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TODAY",
                        fontFamily = NType82FontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Dashboard",
                        fontFamily = NType82FontFamily,
                        fontSize = 32.sp,
                        color = TextPrimary
                    )
                }

                StatusPill(
                    state = summary.connectionState,
                    batteryLevel = summary.deviceBatteryLevel
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Heart Rate & Steps Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Heart Rate",
                    value = summary.latestHeartRate?.toString() ?: "--",
                    unit = "BPM",
                    subtitle = summary.restingHeartRate?.let { "Resting: $it BPM" } ?: "No reading",
                    accentColor = AccentHeartRate,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Steps",
                    value = String.format("%,d", summary.todaySteps),
                    unit = "steps",
                    subtitle = String.format("%.1f km", summary.todayDistanceKm),
                    accentColor = AccentActivity,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Calories & Sleep Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Active Energy",
                    value = summary.todayCaloriesKcal.toString(),
                    unit = "kcal",
                    subtitle = "Daily expenditure",
                    accentColor = AccentSleepAwake,
                    modifier = Modifier.weight(1f)
                )

                val sleepText = summary.lastSleepMinutes?.let {
                    val hrs = it / 60
                    val mins = it % 60
                    "${hrs}h ${mins}m"
                } ?: "--"

                MetricCard(
                    title = "Sleep",
                    value = sleepText,
                    unit = "",
                    subtitle = "Last session",
                    accentColor = AccentSleepREM,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Stress Card
        item {
            MetricCard(
                title = "Stress Index",
                value = summary.latestStressScore?.toString() ?: "--",
                unit = "/ 99",
                subtitle = when (summary.latestStressScore) {
                    in 1..29 -> "State: Rested / Low Stress"
                    in 30..59 -> "State: Moderate Stress"
                    in 60..100 -> "State: High Stress"
                    else -> "No data synced"
                },
                accentColor = AccentStressLow
            )
        }
    }
}
