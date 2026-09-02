package com.cmfwatch.companion.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.ui.components.MetricCard
import com.cmfwatch.companion.ui.theme.*

@Composable
fun HealthScreen(
    latestBpm: Int?,
    restingBpm: Int?,
    latestStress: Int?,
    latestSpO2: Int?,
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
            Text(
                text = "CARDIO & AUTONOMIC",
                fontFamily = NType82FontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Health",
                fontFamily = NType82FontFamily,
                fontSize = 32.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            MetricCard(
                title = "Heart Rate",
                value = latestBpm?.toString() ?: "--",
                unit = "BPM",
                subtitle = restingBpm?.let { "Resting HR: $it BPM" } ?: "No baseline reading",
                accentColor = AccentHeartRate
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            MetricCard(
                title = "Stress Index",
                value = latestStress?.toString() ?: "--",
                unit = "Score",
                subtitle = "Derived from 5-min HRV rMSSD",
                accentColor = AccentStressLow
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            MetricCard(
                title = "Blood Oxygen (SpO₂)",
                value = latestSpO2?.let { "$it%" } ?: "--",
                unit = "",
                subtitle = "Intermittent optical SpO₂ reading",
                accentColor = AccentSpO2
            )
        }
    }
}
