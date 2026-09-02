package com.cmfwatch.companion.ui.activity

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
fun ActivityScreen(
    steps: Int?,
    distanceKm: Float?,
    caloriesKcal: Int?,
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
                text = "EXERCISE & WORKOUTS",
                fontFamily = NType82FontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Activity",
                fontFamily = NType82FontFamily,
                fontSize = 32.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            val stepsText = steps?.let { String.format("%,d", it) } ?: "--"
            MetricCard(
                title = "Total Steps",
                value = stepsText,
                unit = "steps",
                subtitle = "Goal: 10,000 steps",
                accentColor = AccentActivity
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            val distText = distanceKm?.let { String.format("%.2f", it) } ?: "--"
            MetricCard(
                title = "Distance",
                value = distText,
                unit = "km",
                subtitle = "Calculated walking/running distance",
                accentColor = AccentSpO2
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            val caloriesText = caloriesKcal?.toString() ?: "--"
            MetricCard(
                title = "Active Energy",
                value = caloriesText,
                unit = "kcal",
                subtitle = "Movement expenditure",
                accentColor = AccentSleepAwake
            )
        }
    }
}
