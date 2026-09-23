package com.cmfwatch.companion.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    summary: DashboardSummary,
    onActivityClick: () -> Unit = {},
    onHeartRateClick: () -> Unit = {},
    onSleepClick: () -> Unit = {},
    onWatchCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Greeting Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good morning,",
                        fontFamily = AppFontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Rasagna ",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "👋",
                            fontSize = 24.sp
                        )
                    }
                    Text(
                        text = "Stay consistent. You're doing great.",
                        fontFamily = AppFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Notification Bell with Red Badge Dot
                    Box {
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceWhite)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Red badge dot
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Hero CMF Watch Card
        item {
            val batteryVal = summary.deviceBatteryLevel
            val isConnected = summary.connectionState == DeviceConnectionState.CONNECTED_PAIRED ||
                    summary.connectionState == DeviceConnectionState.CONNECTED

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onWatchCardClick() },
                color = SurfaceWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CMF Watch",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) Color(0xFF10B981) else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            Icon(
                                imageVector = Icons.Default.BatteryFull,
                                contentDescription = "Battery",
                                tint = if (batteryVal != null) Color(0xFF10B981) else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = batteryVal?.let { "$it%" } ?: "--",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Device Details",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Watch Visual Representation Icon
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = "Watch Visual",
                            tint = TextPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }

        // 3. 4 Quick Action Buttons Strip (Activity, Sleep, Heart, More)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    title = "Activity",
                    icon = Icons.Default.DirectionsRun,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF10B981),
                    onClick = onActivityClick
                )

                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    title = "Sleep",
                    icon = Icons.Default.NightlightRound,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF8B5CF6),
                    onClick = onSleepClick
                )

                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    title = "Heart",
                    icon = Icons.Default.Favorite,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFEF4444),
                    onClick = onHeartRateClick
                )

                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    title = "More",
                    icon = Icons.Default.MoreHoriz,
                    iconBg = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF3B82F6),
                    onClick = { }
                )
            }
        }

        // 4. Activity Section Card (Rings + 3 Metrics)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onActivityClick() },
                color = SurfaceWhite,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Section Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Activity",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Today",
                                fontFamily = AppFontFamily,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Activity",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 3 Concentric Activity Rings (Calories Pink, Steps Green, Distance Blue)
                        val stepsVal = summary.todaySteps
                        val calVal = summary.todayCaloriesKcal
                        val distVal = summary.todayDistanceKm

                        val stepsProgress = ((stepsVal ?: 0).toFloat() / 10000f).coerceIn(0f, 1f)
                        val calProgress = ((calVal ?: 0).toFloat() / 500f).coerceIn(0f, 1f)
                        val distProgress = ((distVal ?: 0.0f) / 8.0f).coerceIn(0f, 1f)

                        Box(
                            modifier = Modifier.size(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 8.dp.toPx()
                                val center = Offset(size.width / 2, size.height / 2)

                                // Outer Ring - Calories (Pink)
                                val radius1 = (size.width / 2) - strokeWidth / 2
                                drawCircle(
                                    color = Color(0xFFFCE7F3),
                                    radius = radius1,
                                    center = center,
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = Color(0xFFEC4899),
                                    startAngle = -90f,
                                    sweepAngle = 360f * calProgress,
                                    useCenter = false,
                                    topLeft = Offset(center.x - radius1, center.y - radius1),
                                    size = Size(radius1 * 2, radius1 * 2),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                // Middle Ring - Steps (Green)
                                val radius2 = radius1 - strokeWidth - 3.dp.toPx()
                                drawCircle(
                                    color = Color(0xFFD1FAE5),
                                    radius = radius2,
                                    center = center,
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = Color(0xFF10B981),
                                    startAngle = -90f,
                                    sweepAngle = 360f * stepsProgress,
                                    useCenter = false,
                                    topLeft = Offset(center.x - radius2, center.y - radius2),
                                    size = Size(radius2 * 2, radius2 * 2),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                // Inner Ring - Distance (Blue)
                                val radius3 = radius2 - strokeWidth - 3.dp.toPx()
                                drawCircle(
                                    color = Color(0xFFDBEAFE),
                                    radius = radius3,
                                    center = center,
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = Color(0xFF3B82F6),
                                    startAngle = -90f,
                                    sweepAngle = 360f * distProgress,
                                    useCenter = false,
                                    topLeft = Offset(center.x - radius3, center.y - radius3),
                                    size = Size(radius3 * 2, radius3 * 2),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // 3 Metrics Display Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Calories Metric
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFCE7F3)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Calories",
                                        tint = Color(0xFFEC4899),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = calVal?.toString() ?: "--",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "kcal",
                                    fontFamily = AppFontFamily,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            // Steps Metric
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD1FAE5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsWalk,
                                        contentDescription = "Steps",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stepsVal?.let { String.format("%,d", it) } ?: "--",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "steps",
                                    fontFamily = AppFontFamily,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            // Distance Metric
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDBEAFE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Distance",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = distVal?.let { String.format("%.1f", it) } ?: "--",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "km",
                                    fontFamily = AppFontFamily,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. 2 Health Cards Row (Sleep & Heart Rate)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sleep Card (Left)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSleepClick() },
                    color = SurfaceWhite,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF3E8FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NightlightRound,
                                        contentDescription = "Sleep",
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(14.dp)
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
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val sleepMins = summary.lastSleepMinutes
                        val sleepText = if (sleepMins != null) {
                            val hrs = sleepMins / 60
                            val mins = sleepMins % 60
                            "${hrs} h ${mins} m"
                        } else {
                            "--"
                        }

                        Text(
                            text = sleepText,
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Last night",
                            fontFamily = AppFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sleep Epoch Bar Visualizer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            listOf(0.4f, 0.7f, 0.3f, 0.9f, 0.5f, 0.8f, 0.2f, 1.0f, 0.4f).forEach { hRatio ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(hRatio)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.3f + (hRatio * 0.7f)))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "11:12 PM",
                                fontFamily = AppFontFamily,
                                fontSize = 8.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "6:36 AM",
                                fontFamily = AppFontFamily,
                                fontSize = 8.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Heart Rate Card (Right)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onHeartRateClick() },
                    color = SurfaceWhite,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Heart Rate",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
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
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val hrBpm = summary.latestHeartRate
                        Text(
                            text = hrBpm?.let { "$it bpm" } ?: "-- bpm",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Resting",
                            fontFamily = AppFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Heart Rate Smooth Trend Curve Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height

                                val samples = summary.hrSamplesToday
                                val points = if (samples.isNotEmpty()) {
                                    val maxBpm = maxOf(120, samples.maxOf { it.bpm })
                                    val minBpm = minOf(50, samples.minOf { it.bpm })
                                    val range = maxOf(1, maxBpm - minBpm)
                                    samples.takeLast(10).mapIndexed { idx, item ->
                                        val x = (idx.toFloat() / 9f) * width
                                        val y = height - (((item.bpm - minBpm).toFloat() / range.toFloat()) * (height - 4))
                                        Offset(x, y)
                                    }
                                } else {
                                    listOf(
                                        Offset(0f, height * 0.7f),
                                        Offset(width * 0.25f, height * 0.5f),
                                        Offset(width * 0.5f, height * 0.8f),
                                        Offset(width * 0.75f, height * 0.3f),
                                        Offset(width, height * 0.6f)
                                    )
                                }

                                val path = Path().apply {
                                    moveTo(points.first().x, points.first().y)
                                    for (i in 0 until points.size - 1) {
                                        val p1 = points[i]
                                        val p2 = points[i + 1]
                                        val controlX = (p1.x + p2.x) / 2
                                        cubicTo(controlX, p1.y, controlX, p2.y, p2.x, p2.y)
                                    }
                                }

                                drawPath(
                                    path = path,
                                    color = Color(0xFFEF4444),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "12 AM",
                                fontFamily = AppFontFamily,
                                fontSize = 8.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "12 PM",
                                fontFamily = AppFontFamily,
                                fontSize = 8.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 6. Health Insights Banner Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                color = SurfaceWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = "Health Insights",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Health Insights",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your activity is up 12% this week.",
                            fontFamily = AppFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = TextPrimary
            )
        }
    }
}
