package com.cmfwatch.companion.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.SleepSession
import com.cmfwatch.companion.ui.components.MetricCard
import com.cmfwatch.companion.ui.components.SleepTimelineView
import com.cmfwatch.companion.ui.theme.*

@Composable
fun SleepScreen(
    session: SleepSession?,
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
                text = "NOCTURNAL RECOVERY",
                fontFamily = NType82FontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Sleep",
                fontFamily = NType82FontFamily,
                fontSize = 32.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            val totalMins = session?.totalSleepMinutes ?: 0
            val hrs = totalMins / 60
            val mins = totalMins % 60
            val durationText = if (totalMins > 0) "${hrs}h ${mins}m" else "--"

            MetricCard(
                title = "Total Sleep Duration",
                value = durationText,
                unit = "",
                subtitle = if (session != null) "Last nocturnal session" else "No sleep records synced",
                accentColor = AccentSleepREM
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (session != null && session.epochs.isNotEmpty()) {
            item {
                SleepTimelineView(epochs = session.epochs)
            }
        }
    }
}
