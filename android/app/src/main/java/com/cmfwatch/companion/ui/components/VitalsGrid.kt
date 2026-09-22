package com.cmfwatch.companion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import com.cmfwatch.companion.domain.models.HeartRateSample
import com.cmfwatch.companion.ui.theme.*

@Composable
fun VitalsGrid(
    heartRateBpm: Int?,
    hrSamples: List<HeartRateSample>,
    sleepMinutes: Int?,
    onHeartRateClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Card: Heart Rate
        HeartRateCard(
            bpm = heartRateBpm,
            samples = hrSamples,
            onClick = onHeartRateClick,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Right Card: Sleep
        SleepCard(
            totalMinutes = sleepMinutes,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun HeartRateCard(
    bpm: Int?,
    samples: List<HeartRateSample>,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(16.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HeartRateBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = HeartRateRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Heart Rate",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Value Text
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = bpm?.let { "$it" } ?: "--",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "bpm",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }
            Text(
                text = if (bpm != null) "Resting" else "Unavailable",
                fontFamily = AppFontFamily,
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real Dynamic Heart Rate Bezier Sparkline Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (samples.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val bpms = samples.map { it.bpm.toFloat() }
                        val minBpm = (bpms.minOrNull() ?: 50f) - 5f
                        val maxBpm = (bpms.maxOrNull() ?: 120f) + 5f
                        val range = if (maxBpm > minBpm) maxBpm - minBpm else 1f

                        val zoneId = java.time.ZoneId.systemDefault()
                        val points = samples.map { sample ->
                            val time = sample.timestamp.atZone(zoneId).toLocalTime()
                            val minuteOfDay = time.hour * 60 + time.minute
                            val x = (minuteOfDay / 1440f) * width
                            val y = height * (1f - ((sample.bpm - minBpm) / range).coerceIn(0f, 1f))
                            Offset(x, y)
                        }

                        val path = Path()
                        if (points.size == 1) {
                            path.moveTo(0f, points[0].y)
                            path.lineTo(width, points[0].y)
                        } else if (points.isNotEmpty()) {
                            path.moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val current = points[i]
                                val prev = points[i - 1]
                                val controlX = (prev.x + current.x) / 2f
                                path.cubicTo(controlX, prev.y, controlX, current.y, current.x, current.y)
                            }
                        }

                        // Draw Stroke Path
                        drawPath(
                            path = path,
                            color = HeartRateRed,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Draw Gradient Area Fill
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(HeartRateRed.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                    }
                } else {
                    Text(
                        text = "No HR recorded today",
                        fontFamily = AppFontFamily,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Timeline Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("12 AM", fontSize = 9.sp, color = TextMuted)
                Text("6 AM", fontSize = 9.sp, color = TextMuted)
                Text("12 PM", fontSize = 9.sp, color = TextMuted)
                Text("6 PM", fontSize = 9.sp, color = TextMuted)
                Text("12 AM", fontSize = 9.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun SleepCard(
    totalMinutes: Int?,
    modifier: Modifier = Modifier
) {
    val hours = totalMinutes?.let { it / 60 }
    val mins = totalMinutes?.let { it % 60 }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .padding(16.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SleepPurpleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = SleepPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sleep",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Value Text
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (hours != null && mins != null) "${hours}h ${mins}m" else "--",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
            }
            Text(
                text = if (totalMinutes != null) "Last night" else "No sleep record",
                fontFamily = AppFontFamily,
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Vertical Stage Bar Visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (totalMinutes != null) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.4f, 0.8f, 0.5f, 0.9f, 0.6f, 0.7f, 0.4f)
                        val colors = listOf(SleepDeep, SleepCore, SleepREM, SleepDeep, SleepCore, SleepREM, SleepDeep, SleepCore, SleepREM, SleepCore, SleepREM, SleepDeep)

                        heights.forEachIndexed { index, fraction ->
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors[index % colors.size])
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No sleep stages",
                        fontFamily = AppFontFamily,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VitalsLegendItem(color = SleepDeep, label = if (totalMinutes != null) "Deep 1h 32m" else "Deep --")
                VitalsLegendItem(color = SleepCore, label = if (totalMinutes != null) "Core 4h 28m" else "Core --")
                VitalsLegendItem(color = SleepREM, label = if (totalMinutes != null) "REM 1h 24m" else "REM --")
            }
        }
    }
}

@Composable
fun VitalsLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = TextSecondary
        )
    }
}
