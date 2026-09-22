package com.cmfwatch.companion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.ui.theme.*

@Composable
fun ActivityRingsRow(
    steps: Int?,
    stepsTarget: Int = 10000,
    calories: Int?,
    caloriesTarget: Int = 500,
    distanceKm: Float?,
    distanceTarget: Float = 8.0f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Steps Ring Item
            val stepsProgress = steps?.let { (it.toFloat() / stepsTarget.toFloat()).coerceIn(0f, 1f) } ?: 0f
            val stepsText = steps?.let { "%,d".format(it) } ?: "--"
            ActivityRingItem(
                progress = stepsProgress,
                ringColor = RingGreen,
                bgColor = RingGreenBg,
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                valueText = stepsText,
                unitText = "Steps",
                targetText = if (steps != null) "/ %,d".format(stepsTarget) else "No data",
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Calories Ring Item
            val kcalProgress = calories?.let { (it.toFloat() / caloriesTarget.toFloat()).coerceIn(0f, 1f) } ?: 0f
            val kcalText = calories?.let { "$it" } ?: "--"
            ActivityRingItem(
                progress = kcalProgress,
                ringColor = RingOrange,
                bgColor = RingOrangeBg,
                icon = Icons.Default.LocalFireDepartment,
                valueText = kcalText,
                unitText = "kcal",
                targetText = if (calories != null) "/ $caloriesTarget" else "No data",
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Distance Ring Item
            val distProgress = distanceKm?.let { (it / distanceTarget).coerceIn(0f, 1f) } ?: 0f
            val distText = distanceKm?.let { "%.1f".format(it) } ?: "--"
            ActivityRingItem(
                progress = distProgress,
                ringColor = RingBlue,
                bgColor = RingBlueBg,
                icon = Icons.Default.LocationOn,
                valueText = distText,
                unitText = "km",
                targetText = if (distanceKm != null) "/ %.1f".format(distanceTarget) else "No data",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ActivityRingItem(
    progress: Float,
    ringColor: Color,
    bgColor: Color,
    icon: ImageVector,
    valueText: String,
    unitText: String,
    targetText: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            // Canvas Progress Ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                // Track background
                drawArc(
                    color = ringColor.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                // Active progress arc (only if progress > 0)
                if (progress > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Center Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ringColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = valueText,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = TextPrimary
        )

        Text(
            text = unitText,
            fontFamily = AppFontFamily,
            fontSize = 12.sp,
            color = TextSecondary
        )

        Text(
            text = targetText,
            fontFamily = AppFontFamily,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}
