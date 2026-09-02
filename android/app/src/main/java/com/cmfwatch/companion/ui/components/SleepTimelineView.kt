package com.cmfwatch.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.SleepEpoch
import com.cmfwatch.companion.domain.models.SleepStage
import com.cmfwatch.companion.ui.theme.*

@Composable
fun SleepTimelineView(
    epochs: List<SleepEpoch>,
    modifier: Modifier = Modifier
) {
    val totalMinutes = epochs.sumOf { it.durationMinutes }.coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "SLEEP STAGE TIMELINE",
            fontFamily = NType82FontFamily,
            fontSize = 12.sp,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Stage Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkCardVariant)
        ) {
            epochs.forEach { epoch ->
                val weight = (epoch.durationMinutes.toFloat() / totalMinutes).coerceAtLeast(0.01f)
                val color = when (epoch.stage) {
                    SleepStage.DEEP -> AccentSleepDeep
                    SleepStage.LIGHT -> AccentSleepLight
                    SleepStage.REM -> AccentSleepREM
                    SleepStage.AWAKE -> AccentSleepAwake
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend Row
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            LegendItem(label = "Deep", color = AccentSleepDeep)
            LegendItem(label = "Light", color = AccentSleepLight)
            LegendItem(label = "REM", color = AccentSleepREM)
            LegendItem(label = "Awake", color = AccentSleepAwake)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontFamily = NType82FontFamily,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}
