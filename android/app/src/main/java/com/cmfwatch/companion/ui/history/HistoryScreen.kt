package com.cmfwatch.companion.ui.history

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
fun HistoryScreen(
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
                text = "CALENDAR & ARCHIVE",
                fontFamily = NType82FontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "History",
                fontFamily = NType82FontFamily,
                fontSize = 32.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            MetricCard(
                title = "Historical Archives",
                value = "Synced",
                unit = "",
                subtitle = "Select date from calendar to inspect historical telemetry summaries",
                accentColor = AccentSpO2
            )
        }
    }
}
